package com.blue.open.Code.Service;

import com.blue.open.Code.Auth.PermissionEnum;
import com.blue.open.Code.Auth.RoleEnum;
import com.blue.open.Code.Entity.DTO.MemberDTO;
import com.blue.open.Code.Entity.DTO.MemberDescuentoCommand;
import com.blue.open.Code.Entity.DTO.MemberTitularAltaCommand;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.Descuento;
import com.blue.open.Code.Entity.Db.Member;
import com.blue.open.Code.Entity.Db.MemberDescuento;
import com.blue.open.Code.Entity.Db.User;
import com.blue.open.Code.Entity.Enums.MemberEstadoEnum;
import com.blue.open.Code.Entity.Enums.MemberRegistroTipoEnum;
import com.blue.open.Code.Mapper.MemberMapper;
import com.blue.open.Code.Repository.DescuentoRepository;
import com.blue.open.Code.Repository.MemberDescuentoRepository;
import com.blue.open.Code.Repository.MemberRepository;
import com.blue.open.Code.Service.CuotaSocialRango.CuotaSocialRangoResolver;
import com.blue.open.Code.Service.Tesoreria.CuotaSocialGeneracionService;
import com.blue.open.Core.Exceptions.CustomException;
import com.blue.open.Core.Service.Auth.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class MemberTitularService {

    private static final int STATUS_ACTIVE = MemberStatusConstants.STATUS_ACTIVE;

    private enum ProvisioningMode {
        BACKOFFICE,
        PUBLIC,
        MIGRATION
    }

    @Autowired
    private AuthService authService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private MemberUserProvisioningService userProvisioningService;

    @Autowired
    private MemberNumeroSocioService numeroSocioService;

    @Autowired
    private MemberMapper memberMapper;

    @Autowired
    private MemberRegistroNotificacionService registroNotificacionService;

    @Autowired
    private CuotaSocialGeneracionService cuotaSocialGeneracionService;

    @Autowired
    private CuotaSocialRangoResolver cuotaSocialRangoResolver;

    @Autowired
    private DescuentoRepository descuentoRepository;

    @Autowired
    private MemberDescuentoRepository memberDescuentoRepository;

    @Autowired
    private MemberCupoService memberCupoService;

    @Transactional
    public MemberDTO altaTitular(MemberTitularAltaCommand command) {
        authService.valid(List.of(PermissionEnum.MEMBER_CREATE));
        Company company = authService.getCurrentCompanyOrThrow();
        memberCupoService.validarCupoDisponible(company, 1);
        return registrarTitularEnClub(company, command, ProvisioningMode.BACKOFFICE);
    }

    @Transactional
    public MemberDTO registrarTitularPublicoEnClub(Company company, MemberTitularAltaCommand command) {
        return registrarTitularEnClub(company, command, ProvisioningMode.PUBLIC);
    }

    @Transactional
    public MemberDTO registrarTitularMigracionEnClub(Company company, MemberTitularAltaCommand command) {
        return registrarTitularEnClub(company, command, ProvisioningMode.MIGRATION);
    }

    private MemberDTO registrarTitularEnClub(Company company, MemberTitularAltaCommand command,
            ProvisioningMode mode) {
        if (command == null) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Datos requeridos",
                    "MemberTitularService | command null");
        }

        User user = resolveUser(command, company, mode);

        if (memberRepository.existsByCompany_IdAndUser_IdAndStatusIn(company.getId(), user.getId(),
                java.util.List.of(STATUS_ACTIVE, MemberStatusConstants.STATUS_PENDING_APPROVAL))) {
            HttpStatus status = mode == ProvisioningMode.PUBLIC ? HttpStatus.CONFLICT : HttpStatus.BAD_REQUEST;
            throw new CustomException(status, "Usuario ya registrado como socio",
                    "MemberTitularService | userId: " + user.getId());
        }

        LocalDate fechaAlta = command.getFechaAlta() != null ? command.getFechaAlta() : LocalDate.now();

        Member member = new Member();
        member.setCompany(company);
        member.setUser(user);
        member.setTitular(null);
        member.setParentesco(null);
        member.setCategoria(null);
        member.setEstado(MemberEstadoEnum.ACTIVO);
        member.setFechaAlta(fechaAlta);
        member.setProximaGeneracionCuota(fechaAlta);
        member.setNumeroSocio(switch (mode) {
            case PUBLIC -> null;
            case BACKOFFICE, MIGRATION -> numeroSocioService.resolver(company.getId(), command.getNumeroSocio(), null);
            default -> numeroSocioService.generateNext(company.getId());
        });
        member.setCuotaSocialRango(cuotaSocialRangoResolver.resolver(company, user.getFechaNacimiento(), fechaAlta));
        if (mode == ProvisioningMode.PUBLIC) {
            member.setStatus(MemberStatusConstants.STATUS_PENDING_APPROVAL);
        }

        member = numeroSocioService.guardar(member, company.getId());

        List<MemberDescuentoCommand> descuentoCommands = command.getDescuentos();
        if (descuentoCommands != null) {
            List<MemberDescuento> asignaciones = new ArrayList<>();
            for (MemberDescuentoCommand descuentoCommand : descuentoCommands) {
                Descuento descuento = descuentoRepository
                        .findByUuidAndCompanyIdAndStatusActive(descuentoCommand.getDescuentoUuid(), company.getId())
                        .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Descuento no encontrado",
                                "MemberTitularService | descuentoUuid: " + descuentoCommand.getDescuentoUuid()));

                asignaciones.add(MemberDescuento.asignar(
                        member, descuento, descuentoCommand.getFechaDesde(), descuentoCommand.getFechaHasta()));
            }
            memberDescuentoRepository.saveAll(asignaciones);
        }

        if (mode != ProvisioningMode.PUBLIC) {
            cuotaSocialGeneracionService.generarPorAlta(member.getId(), fechaAlta);
        } else {
            registroNotificacionService.notificarSolicitudPendiente(company, member,
                    MemberRegistroTipoEnum.SOCIO_TITULAR);
        }

        return memberMapper.modelToDto(
                memberRepository.findByIdWithTitular(member.getId()).orElse(member));
    }

    private User resolveUser(MemberTitularAltaCommand command, Company company, ProvisioningMode mode) {
        if (mode == ProvisioningMode.PUBLIC) {
            return userProvisioningService.resolveOrCreatePortalUser(command, company, RoleEnum.SOCIO_TITULAR);
        }
        if (mode == ProvisioningMode.MIGRATION) {
            return userProvisioningService.resolveOrCreateMigrationUser(command, company, RoleEnum.SOCIO_TITULAR);
        }
        return userProvisioningService.resolveOrCreateMigrationUser(command, company, RoleEnum.SOCIO_TITULAR);
    }

}
