package com.blue.open.Code.Service;

import com.blue.open.Code.Auth.PermissionEnum;
import com.blue.open.Code.Entity.DTO.MemberDTO;
import com.blue.open.Code.Entity.DTO.MemberPortalProfileCommand;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.Member;
import com.blue.open.Code.Mapper.MemberMapper;
import com.blue.open.Code.Repository.MemberRepository;
import com.blue.open.Code.Entity.Db.User;
import com.blue.open.Core.Repository.UserRepository;
import com.blue.open.Core.Service.Auth.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MemberPortalProfileService {

    @Autowired
    private AuthService authService;

    @Autowired
    private MemberAuthService memberAuthService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MemberMapper memberMapper;

    @Autowired
    private CertificadoMedicoQueryService certificadoMedicoQueryService;

    @Transactional(readOnly = true)
    public MemberDTO obtenerPerfil() {
        authService.valid(List.of(PermissionEnum.PORTAL_CARD_VIEW));
        Company company = authService.getCurrentCompanyOrThrow();
        Member member = memberAuthService.requirePortalEligible(
                memberAuthService.getCurrentMemberOrThrow(company.getId()));
        MemberDTO dto = memberMapper.modelToDto(member);
        dto.setCertificados(certificadoMedicoQueryService.listarPorMember(member));
        return dto;
    }

    @Transactional
    public MemberDTO actualizarPerfil(MemberPortalProfileCommand command, String memberUuid) {
        authService.valid(List.of(PermissionEnum.PORTAL_PROFILE_EDIT));
        Company company = authService.getCurrentCompanyOrThrow();
        Member member = memberAuthService.requirePortalEligible(
                memberAuthService.getCurrentMemberOrThrow(company.getId()));

        if (memberUuid != null && !memberUuid.isBlank()) {
            memberAuthService.requireTitular(member);
            member = memberAuthService.resolveMemberOfFamilyOrThrow(memberUuid, company.getId());
        }

        if (command != null) {
            memberMapper.applyPortalProfileUpdate(command, member);
            User user = member.getUser();
            userRepository.save(user);
            member = memberRepository.save(member);
        }

        return memberMapper.modelToDto(
                memberRepository.findByIdWithTitular(member.getId()).orElse(member));
    }
}
