package com.blue.open.Code.Service;

import com.blue.open.Code.Auth.PermissionEnum;
import com.blue.open.Code.Entity.DTO.MemberDTO;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.Member;
import com.blue.open.Code.Entity.Enums.MemberEstadoEnum;
import com.blue.open.Code.Mapper.MemberMapper;
import com.blue.open.Code.Repository.MemberRepository;
import com.blue.open.Code.Service.Actividades.InscripcionActividadBajaService;
import com.blue.open.Core.Service.Auth.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MemberBajaService {

    @Autowired
    private AuthService authService;

    @Autowired
    private MemberAuthService memberAuthService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private MemberMapper memberMapper;

    @Autowired
    private InscripcionActividadBajaService inscripcionActividadBajaService;

    @Transactional
    public MemberDTO darBajaLogica(String memberUuid) {
        Company company = authService.getCurrentCompanyOrThrow();
        Member member = memberAuthService.resolveMemberByUuid(memberUuid, company.getId());

        if (member.isTitular()) {
            authService.valid(List.of(PermissionEnum.MEMBER_EDIT));
        } else {
            authService.valid(List.of(PermissionEnum.FAMILY_EDIT));
        }

        if (member.getEstado() == MemberEstadoEnum.INACTIVO) {
            return memberMapper.modelToDto(member);
        }

        member.setEstado(MemberEstadoEnum.INACTIVO);
        inscripcionActividadBajaService.bajaTodasActivasPorMember(member.getId(), company.getId());
        member = memberRepository.save(member);

        return memberMapper.modelToDto(
                memberRepository.findByIdWithTitular(member.getId()).orElse(member));
    }
}
