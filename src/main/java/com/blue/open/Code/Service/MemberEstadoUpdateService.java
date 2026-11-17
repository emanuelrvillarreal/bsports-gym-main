package com.blue.open.Code.Service;

import com.blue.open.Code.Auth.PermissionEnum;
import com.blue.open.Code.Entity.DTO.MemberDTO;
import com.blue.open.Code.Entity.DTO.MemberEstadoUpdateCommand;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.Member;
import com.blue.open.Code.Mapper.MemberMapper;
import com.blue.open.Code.Repository.MemberRepository;
import com.blue.open.Core.Service.Auth.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MemberEstadoUpdateService {

    @Autowired
    private AuthService authService;

    @Autowired
    private MemberAuthService memberAuthService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private MemberMapper memberMapper;

    @Transactional
    public MemberDTO actualizarEstado(String memberUuid, MemberEstadoUpdateCommand command) {
        authService.valid(List.of(PermissionEnum.MEMBER_CHANGE_STATUS));

        Company company = authService.getCurrentCompanyOrThrow();
        Member member = memberAuthService.resolveMemberByUuid(memberUuid, company.getId());

        member.setEstado(command.getEstado());
        member = memberRepository.save(member);

        return memberMapper.modelToDto(
                memberRepository.findByIdWithTitular(member.getId()).orElse(member));
    }
}
