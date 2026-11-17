package com.blue.open.Code.Service;

import com.blue.open.Code.Auth.PermissionEnum;
import com.blue.open.Code.Entity.DTO.MemberAprobarRegistroCommand;
import com.blue.open.Code.Entity.DTO.MemberDTO;
import com.blue.open.Code.Entity.DTO.MemberDescuentoCommand;
import com.blue.open.Code.Entity.DTO.MemberRegistroRechazoCommand;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.Descuento;
import com.blue.open.Code.Entity.Db.Member;
import com.blue.open.Code.Entity.Db.MemberDescuento;
import com.blue.open.Code.Entity.Db.User;
import com.blue.open.Code.Mapper.MemberMapper;
import com.blue.open.Code.Repository.DescuentoRepository;
import com.blue.open.Code.Repository.MemberDescuentoRepository;
import com.blue.open.Code.Repository.MemberRepository;
import com.blue.open.Code.Service.Tesoreria.CuotaSocialGeneracionService;
import com.blue.open.Core.Exceptions.CustomException;
import com.blue.open.Core.Repository.UserRepository;
import com.blue.open.Core.Service.Auth.AuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class MemberRegistroAprobacionService {

    @Autowired
    private AuthService authService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MemberMapper memberMapper;

    @Autowired
    private CuotaSocialGeneracionService cuotaSocialGeneracionService;

    @Autowired
    private DescuentoRepository descuentoRepository;

    @Autowired
    private MemberDescuentoRepository memberDescuentoRepository;

    @Autowired
    private MemberCupoService memberCupoService;

    @Autowired
    private MemberNumeroSocioService numeroSocioService;

    @Transactional
    public MemberDTO aprobarRegistro(String memberUuid, MemberAprobarRegistroCommand command) {
        authService.valid(List.of(PermissionEnum.MEMBER_CREATE));
        Company company = authService.getCurrentCompanyOrThrow();
        memberCupoService.validarCupoDisponible(company, 1);
        Member member = resolvePendingMemberOrThrow(memberUuid, company.getId());

        member.setNumeroSocio(numeroSocioService.generateNext(company.getId()));
        member.setStatus(MemberStatusConstants.STATUS_ACTIVE);
        member = memberRepository.save(member);

        User user = member.getUser();
        if (user != null && user.getStatus() != null
                && user.getStatus() == MemberStatusConstants.STATUS_PENDING_APPROVAL) {
            user.setStatus(MemberStatusConstants.STATUS_ACTIVE);
            userRepository.save(user);
        }

        List<MemberDescuentoCommand> descuentoCommands = command != null ? command.getDescuentos() : null;
        if (descuentoCommands != null) {
            List<MemberDescuento> asignaciones = new ArrayList<>();
            for (MemberDescuentoCommand descuentoCommand : descuentoCommands) {
                Descuento descuento = descuentoRepository
                        .findByUuidAndCompanyIdAndStatusActive(descuentoCommand.getDescuentoUuid(), company.getId())
                        .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Descuento no encontrado",
                                "MemberRegistroAprobacionService | descuentoUuid: " + descuentoCommand.getDescuentoUuid()));

                asignaciones.add(MemberDescuento.asignar(
                        member, descuento, descuentoCommand.getFechaDesde(), descuentoCommand.getFechaHasta()));
            }
            memberDescuentoRepository.saveAll(asignaciones);
        }

        cuotaSocialGeneracionService.generarPorAlta(member.getId(), member.getFechaAlta());

        return memberMapper.modelToDto(
                memberRepository.findByIdWithTitular(member.getId()).orElse(member));
    }

    @Transactional
    public MemberDTO rechazarRegistro(String memberUuid, MemberRegistroRechazoCommand command) {
        authService.valid(List.of(PermissionEnum.MEMBER_CREATE));
        Company company = authService.getCurrentCompanyOrThrow();
        Member member = resolvePendingMemberOrThrow(memberUuid, company.getId());

        if (command != null && command.getMotivo() != null && !command.getMotivo().isBlank()) {
            log.info("MemberRegistroAprobacionService | rechazo memberUuid={} motivo={}",
                    memberUuid, command.getMotivo().trim());
        }

        member.setStatus(MemberStatusConstants.STATUS_DELETED);
        member = memberRepository.save(member);

        User user = member.getUser();
        if (user != null && user.getStatus() != null
                && user.getStatus() == MemberStatusConstants.STATUS_PENDING_APPROVAL) {
            long otrosActivos = memberRepository.countByUser_IdAndStatus(
                    user.getId(), MemberStatusConstants.STATUS_ACTIVE);
            if (otrosActivos == 0) {
                user.setStatus(MemberStatusConstants.STATUS_DELETED);
                anonymizeDeletedUser(user);
                userRepository.save(user);
            }
        }

        return memberMapper.modelToDto(member);
    }

    private void anonymizeDeletedUser(User user) {
        String prefix = "deleted-" + user.getId() + "-";
        user.setEmail(prefix + user.getEmail());
        user.setUsername(prefix + user.getUsername());
        if (user.getDni() != null && !user.getDni().isBlank()) {
            user.setDni(prefix + user.getDni());
        }
        user.setNombre("Usuario");
        user.setApellido("Eliminado");
    }

    private Member resolvePendingMemberOrThrow(String memberUuid, Long companyId) {
        if (memberUuid == null || memberUuid.isBlank()) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Member requerido",
                    "MemberRegistroAprobacionService | memberUuid vacío");
        }
        Member member = memberRepository.findByUuidAndCompanyIdAndStatus(
                        memberUuid, companyId, MemberStatusConstants.STATUS_PENDING_APPROVAL)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND,
                        "Solicitud de registro no encontrada",
                        "MemberRegistroAprobacionService | uuid: " + memberUuid));
        return member;
    }
}
