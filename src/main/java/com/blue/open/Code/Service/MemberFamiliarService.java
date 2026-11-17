package com.blue.open.Code.Service;

import com.blue.open.Code.Auth.PermissionEnum;
import com.blue.open.Code.Auth.RoleEnum;
import com.blue.open.Code.Entity.DTO.MemberAltaUserFields;
import com.blue.open.Code.Entity.DTO.MemberDTO;
import com.blue.open.Code.Entity.DTO.MemberDescuentoCommand;
import com.blue.open.Code.Entity.DTO.MemberFamiliarAltaCommand;
import com.blue.open.Code.Entity.DTO.MemberPortalFamiliarAltaCommand;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.Descuento;
import com.blue.open.Code.Entity.Db.Member;
import com.blue.open.Code.Entity.Db.MemberDescuento;
import com.blue.open.Code.Entity.Db.User;
import com.blue.open.Code.Entity.Enums.MemberEstadoEnum;
import com.blue.open.Code.Entity.Enums.MemberRegistroTipoEnum;
import com.blue.open.Code.Entity.Enums.ParentescoEnum;
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
public class MemberFamiliarService {

    private static final int STATUS_ACTIVE = MemberStatusConstants.STATUS_ACTIVE;

    private enum ProvisioningMode {
        BACKOFFICE,
        PUBLIC,
        MIGRATION
    }

    @Autowired
    private AuthService authService;

    @Autowired
    private MemberAuthService memberAuthService;

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
    public MemberDTO altaFamiliar(MemberFamiliarAltaCommand command) {
        authService.valid(List.of(PermissionEnum.FAMILY_CREATE));
        Company company = authService.getCurrentCompanyOrThrow();
        memberCupoService.validarCupoDisponible(company, 1);

        if (command == null) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Datos requeridos",
                    "MemberFamiliarService | command null");
        }
        if (command.getTitularMemberUuid() == null || command.getTitularMemberUuid().isBlank()) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Socio titular requerido",
                    "MemberFamiliarService | titularMemberUuid vacío");
        }
        if (command.getParentesco() == null) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Parentesco requerido",
                    "MemberFamiliarService | parentesco null");
        }

        Member titular = memberAuthService.resolveTitularByUuid(command.getTitularMemberUuid(), company.getId());
        return registrarFamiliarEnClub(company, titular, command, command.getParentesco(),
                command.getFechaAlta(), ProvisioningMode.BACKOFFICE);
    }

    @Transactional
    public MemberDTO registrarFamiliarMigracionEnClub(Company company, Member titular,
            MemberFamiliarAltaCommand command) {
        if (command == null) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Datos requeridos",
                    "MemberFamiliarService | command null");
        }
        if (command.getParentesco() == null) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Parentesco requerido",
                    "MemberFamiliarService | parentesco null");
        }
        return registrarFamiliarEnClub(company, titular, command, command.getParentesco(),
                command.getFechaAlta(), ProvisioningMode.MIGRATION);
    }

    @Transactional
    public MemberDTO registrarFamiliarPortalEnClub(Company company, Member titular,
            MemberPortalFamiliarAltaCommand command) {
        if (command == null) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Datos requeridos",
                    "MemberFamiliarService | command null");
        }
        if (command.getParentesco() == null) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Parentesco requerido",
                    "MemberFamiliarService | parentesco null");
        }
        MemberFamiliarAltaCommand internal = toInternalCommand(command);
        return registrarFamiliarEnClub(company, titular, internal, command.getParentesco(),
                LocalDate.now(), ProvisioningMode.PUBLIC);
    }

    private MemberDTO registrarFamiliarEnClub(Company company, Member titular, MemberFamiliarAltaCommand command,
            ParentescoEnum parentesco, LocalDate fechaAltaInput, ProvisioningMode mode) {
        assertTitularPermiteAltaFamiliar(titular);

        User user = resolveUser(command, company, mode);

        if (memberRepository.existsByCompany_IdAndUser_IdAndStatusIn(company.getId(), user.getId(),
                java.util.List.of(STATUS_ACTIVE, MemberStatusConstants.STATUS_PENDING_APPROVAL))) {
            HttpStatus status = mode == ProvisioningMode.PUBLIC ? HttpStatus.CONFLICT : HttpStatus.BAD_REQUEST;
            throw new CustomException(status, "Usuario ya registrado como socio",
                    "MemberFamiliarService | userId: " + user.getId());
        }

        LocalDate fechaAlta = fechaAltaInput != null ? fechaAltaInput : LocalDate.now();

        Member member = new Member();
        member.setCompany(company);
        member.setUser(user);
        member.setTitular(titular);
        member.setParentesco(parentesco);
        member.setNumeroSocio(switch (mode) {
            case PUBLIC -> null;
            // En la importación se respeta el número de la planilla (si está libre).
            case MIGRATION -> numeroSocioService.resolver(company.getId(), command.getNumeroSocio(), null);
            default -> numeroSocioService.generateNext(company.getId());
        });
        member.setCategoria(null);
        member.setEstado(MemberEstadoEnum.ACTIVO);
        member.setFechaAlta(fechaAlta);
        member.setProximaGeneracionCuota(fechaAlta);
        member.setCuotaSocialRango(cuotaSocialRangoResolver.resolver(company, user.getFechaNacimiento(), fechaAlta));
        if (mode == ProvisioningMode.PUBLIC) {
            member.setStatus(MemberStatusConstants.STATUS_PENDING_APPROVAL);
        }

        member = memberRepository.save(member);

        List<MemberDescuentoCommand> descuentoCommands = command.getDescuentos();
        if (descuentoCommands != null) {
            List<MemberDescuento> asignaciones = new ArrayList<>();
            for (MemberDescuentoCommand descuentoCommand : descuentoCommands) {
                Descuento descuento = descuentoRepository
                        .findByUuidAndCompanyIdAndStatusActive(descuentoCommand.getDescuentoUuid(), company.getId())
                        .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Descuento no encontrado",
                                "MemberFamiliarService | descuentoUuid: " + descuentoCommand.getDescuentoUuid()));

                asignaciones.add(MemberDescuento.asignar(
                        member, descuento, descuentoCommand.getFechaDesde(), descuentoCommand.getFechaHasta()));
            }
            memberDescuentoRepository.saveAll(asignaciones);
        }

        if (mode != ProvisioningMode.PUBLIC) {
            cuotaSocialGeneracionService.generarPorAlta(member.getId(), fechaAlta);
        } else {
            registroNotificacionService.notificarSolicitudPendiente(company, member,
                    MemberRegistroTipoEnum.FAMILIAR);
        }

        return memberMapper.modelToDto(
                memberRepository.findByIdWithTitular(member.getId()).orElse(member));
    }

    private User resolveUser(MemberAltaUserFields fields, Company company, ProvisioningMode mode) {
        if (mode == ProvisioningMode.PUBLIC) {
            return userProvisioningService.resolveOrCreatePortalUser(fields, company, RoleEnum.FAMILIAR);
        }
        if (mode == ProvisioningMode.MIGRATION) {
            return userProvisioningService.resolveOrCreateMigrationUser(fields, company, RoleEnum.FAMILIAR);
        }
        return userProvisioningService.createPortalUser(fields, company, RoleEnum.FAMILIAR);
    }

    private MemberFamiliarAltaCommand toInternalCommand(MemberPortalFamiliarAltaCommand command) {
        MemberFamiliarAltaCommand internal = new MemberFamiliarAltaCommand();
        internal.setNombre(command.getNombre());
        internal.setApellido(command.getApellido());
        internal.setDni(command.getDni());
        internal.setFechaNacimiento(command.getFechaNacimiento());
        internal.setSexo(command.getSexo());
        internal.setTelefono(command.getTelefono());
        internal.setEmail(command.getEmail());
        internal.setPassword(command.getPassword());
        internal.setParentesco(command.getParentesco());
        return internal;
    }

    private void assertTitularPermiteAltaFamiliar(Member titular) {
        MemberEstadoEnum estado = titular.getEstado();
        if (estado == MemberEstadoEnum.INACTIVO || estado == MemberEstadoEnum.INHABILITADO) {
            throw new CustomException(HttpStatus.BAD_REQUEST,
                    "No se puede dar de alta un familiar para este titular",
                    "MemberFamiliarService | titular estado: " + estado);
        }
    }

}
