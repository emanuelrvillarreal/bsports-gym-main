package com.blue.open.Code.Service;

import com.blue.open.Code.Entity.Db.Member;
import com.blue.open.Code.Entity.Db.User;
import com.blue.open.Code.Entity.Enums.MemberEstadoEnum;
import com.blue.open.Code.Repository.MemberRepository;
import com.blue.open.Core.Exceptions.CustomException;
import com.blue.open.Core.Service.Auth.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class MemberAuthService {

    @Autowired
    private AuthService authService;

    @Autowired
    private MemberRepository memberRepository;

    public Member getCurrentMemberOrThrow(Long companyId) {
        User user = authService.getActualOrThrow();
        return memberRepository.findByUserIdAndCompanyIdWithDetails(user.getId(), companyId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Socio no encontrado",
                        "MemberAuthService | userId: " + user.getId() + " companyId: " + companyId));
    }

    public Member requireTitular(Member member) {
        if (member == null || !member.isTitular()) {
            throw new CustomException(HttpStatus.FORBIDDEN,
                    "Solo el socio titular puede acceder a esta información",
                    "MemberAuthService | memberId: " + (member != null ? member.getId() : null));
        }
        return member;
    }

    public Member resolveMemberByUuid(String memberUuid, Long companyId) {
        if (memberUuid == null || memberUuid.isBlank()) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Member requerido",
                    "MemberAuthService | memberUuid vacío");
        }
        return memberRepository.findByUuidAndCompanyIdWithDetails(memberUuid, companyId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Socio no encontrado",
                        "MemberAuthService | uuid: " + memberUuid));
    }

    public Member resolveTitularByUuid(String titularMemberUuid, Long companyId) {
        Member member = resolveMemberByUuid(titularMemberUuid, companyId);
        if (!member.isTitular()) {
            throw new CustomException(HttpStatus.BAD_REQUEST,
                    "El identificador no corresponde a un socio titular",
                    "MemberAuthService | uuid: " + titularMemberUuid);
        }
        return member;
    }

    public Member resolveMemberOfFamilyOrThrow(String memberUuid, Long companyId) {
        Member current = getCurrentMemberOrThrow(companyId);
        Member target = resolveMemberByUuid(memberUuid, companyId);
        if (!current.resolveTitularMemberId().equals(target.resolveTitularMemberId())) {
            throw new CustomException(HttpStatus.FORBIDDEN,
                    "No tenés acceso a este socio",
                    "MemberAuthService | currentMemberId: " + current.getId() + " targetUuid: " + memberUuid);
        }
        return target;
    }

    public Member requirePortalEligible(Member member) {
        if (member == null) {
            throw new CustomException(HttpStatus.FORBIDDEN, "Acceso al portal no disponible",
                    "MemberAuthService | member null");
        }
        MemberEstadoEnum estado = member.getEstado();
        if (estado == MemberEstadoEnum.INHABILITADO
                || estado == MemberEstadoEnum.SUSPENDIDO
                || estado == MemberEstadoEnum.INACTIVO) {
            throw new CustomException(HttpStatus.FORBIDDEN, "Acceso al portal no disponible",
                    "MemberAuthService | estado: " + estado + " memberId: " + member.getId());
        }
        return member;
    }
}
