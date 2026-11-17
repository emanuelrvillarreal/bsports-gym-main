package com.blue.open.Code.Service;

import com.blue.open.Code.Auth.PermissionEnum;
import com.blue.open.Code.Entity.DTO.MemberDTO;
import com.blue.open.Code.Entity.DTO.PortalMemberCredencialDTO;
import com.blue.open.Code.Entity.DTO.PortalMemberGrupoDTO;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.Member;
import com.blue.open.Code.Entity.Db.User;
import com.blue.open.Code.Entity.Enums.MemberEstadoEnum;
import com.blue.open.Code.Mapper.MemberMapper;
import com.blue.open.Code.Repository.MemberRepository;
import com.blue.open.Core.Service.Auth.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
public class MemberPortalCredencialService {

    @Autowired
    private AuthService authService;

    @Autowired
    private MemberAuthService memberAuthService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private MemberMapper memberMapper;

    @Autowired
    private QrCodeService qrCodeService;

    @Autowired
    private CertificadoMedicoQueryService certificadoMedicoQueryService;

    @Transactional(readOnly = true)
    public PortalMemberCredencialDTO obtenerCredencial() {
        authService.valid(List.of(PermissionEnum.PORTAL_CARD_VIEW));
        Company company = authService.getCurrentCompanyOrThrow();
        Member member = memberAuthService.requirePortalEligible(
                memberAuthService.getCurrentMemberOrThrow(company.getId()));
        return toCredencial(member, false);
    }

    @Transactional(readOnly = true)
    public PortalMemberGrupoDTO obtenerGrupoFamiliar() {
        authService.valid(List.of(PermissionEnum.PORTAL_FAMILY_VIEW));
        Company company = authService.getCurrentCompanyOrThrow();
        Member titular = memberAuthService.requireTitular(
                memberAuthService.requirePortalEligible(
                        memberAuthService.getCurrentMemberOrThrow(company.getId())));

        List<Member> grupo = memberRepository.findGrupoConPendientesByTitularId(company.getId(), titular.getId());
        List<PortalMemberCredencialDTO> integrantes = grupo.stream()
                .filter(m -> m.getEstado() != MemberEstadoEnum.INACTIVO)
                .sorted(Comparator
                        .comparing((Member m) -> !m.isTitular())
                        .thenComparing(this::apellidoOrEmpty, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(this::nombreOrEmpty, String.CASE_INSENSITIVE_ORDER))
                .map(m -> toCredencial(m, true))
                .toList();

        PortalMemberGrupoDTO dto = new PortalMemberGrupoDTO();
        dto.setTitularMemberUuid(titular.getUuid());
        dto.setIntegrantes(integrantes);
        return dto;
    }

    private PortalMemberCredencialDTO toCredencial(Member member, boolean incluirCertificados) {
        User user = member.getUser();
        PortalMemberCredencialDTO dto = new PortalMemberCredencialDTO();
        MemberDTO memberDto = memberMapper.modelToDto(member);
        if (incluirCertificados) {
            memberDto.setCertificados(certificadoMedicoQueryService.listarPorMember(member));
        }
        dto.setMember(memberDto);
        if (user != null) {
            dto.setUserUuid(user.getUuid());
            dto.setQr(qrCodeService.generateBase64Png(user.getUuid()));
        }
        return dto;
    }

    private String apellidoOrEmpty(Member member) {
        if (member.getUser() == null || member.getUser().getApellido() == null) {
            return "";
        }
        return member.getUser().getApellido();
    }

    private String nombreOrEmpty(Member member) {
        if (member.getUser() == null || member.getUser().getNombre() == null) {
            return "";
        }
        return member.getUser().getNombre();
    }
}
