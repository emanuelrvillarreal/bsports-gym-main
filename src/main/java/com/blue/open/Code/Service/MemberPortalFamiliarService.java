package com.blue.open.Code.Service;

import com.blue.open.Code.Auth.PermissionEnum;
import com.blue.open.Code.Entity.DTO.MemberDTO;
import com.blue.open.Code.Entity.DTO.MemberPortalFamiliarAltaCommand;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.Member;
import com.blue.open.Core.Service.Auth.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MemberPortalFamiliarService {

    @Autowired
    private AuthService authService;

    @Autowired
    private MemberAuthService memberAuthService;

    @Autowired
    private MemberFamiliarService memberFamiliarService;

    @Transactional
    public MemberDTO altaFamiliar(MemberPortalFamiliarAltaCommand command) {
        authService.valid(List.of(PermissionEnum.PORTAL_FAMILY_CREATE));
        Company company = authService.getCurrentCompanyOrThrow();
        Member titular = memberAuthService.requireTitular(
                memberAuthService.requirePortalEligible(
                        memberAuthService.getCurrentMemberOrThrow(company.getId())));
        return memberFamiliarService.registrarFamiliarPortalEnClub(company, titular, command);
    }
}
