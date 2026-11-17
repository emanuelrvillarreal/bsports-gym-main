package com.blue.open.Code.Service;

import com.blue.open.Code.Auth.PermissionEnum;
import com.blue.open.Code.Entity.DTO.MemberBackofficeUpdateCommand;
import com.blue.open.Code.Entity.DTO.MemberDTO;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.Member;
import com.blue.open.Code.Mapper.MemberMapper;
import com.blue.open.Code.Repository.MemberRepository;
import com.blue.open.Code.Entity.Db.User;
import com.blue.open.Core.Exceptions.CustomException;
import com.blue.open.Core.Repository.UserRepository;
import com.blue.open.Core.Service.Auth.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MemberBackofficeUpdateService {

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
    private MemberUserValidationHelper validationHelper;

    @Autowired
    private MemberNumeroSocioService numeroSocioService;

    @Transactional
    public MemberDTO actualizar(String memberUuid, MemberBackofficeUpdateCommand command) {
        Company company = authService.getCurrentCompanyOrThrow();
        Member member = memberAuthService.resolveMemberByUuid(memberUuid, company.getId());

        if (member.isTitular()) {
            authService.valid(List.of(PermissionEnum.MEMBER_EDIT));
        } else {
            authService.valid(List.of(PermissionEnum.FAMILY_EDIT));
        }

        if (command == null) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Datos requeridos",
                    "MemberBackofficeUpdateService | command null");
        }

        User user = member.getUser();
        validationHelper.validateBackofficeUpdateEmail(command, user);
        memberMapper.applyBackofficeUpdate(command, member);
        actualizarNumeroSocio(command.getNumeroSocio(), member, company);

        userRepository.save(user);
        member = numeroSocioService.guardar(member, company.getId());

        return memberMapper.modelToDto(
                memberRepository.findByIdWithTitular(member.getId()).orElse(member));
    }

    private void actualizarNumeroSocio(Integer numeroSocio, Member member, Company company) {
        if (numeroSocio == null) {
            return;
        }
        // Valida contra todos los socios del club (también los dados de baja), igual que la clave única.
        member.setNumeroSocio(numeroSocioService.resolver(company.getId(), numeroSocio, member.getId()));
    }
}
