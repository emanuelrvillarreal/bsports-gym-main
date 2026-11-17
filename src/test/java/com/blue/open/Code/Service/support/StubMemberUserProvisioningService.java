package com.blue.open.Code.Service.support;

import com.blue.open.Code.Auth.RoleEnum;
import com.blue.open.Code.Entity.DTO.MemberAltaUserFields;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.User;
import com.blue.open.Code.Service.MemberUserProvisioningService;

public class StubMemberUserProvisioningService extends MemberUserProvisioningService {

    private User userToReturn;
    private RoleEnum lastRoleEnum;
    private int createPortalUserCalls;

    public void setUserToReturn(User userToReturn) {
        this.userToReturn = userToReturn;
    }

    public RoleEnum getLastRoleEnum() {
        return lastRoleEnum;
    }

    public int getCreatePortalUserCalls() {
        return createPortalUserCalls;
    }

    @Override
    public User createPortalUser(MemberAltaUserFields fields, Company company, RoleEnum roleEnum) {
        createPortalUserCalls++;
        lastRoleEnum = roleEnum;
        return userToReturn;
    }

    @Override
    public User resolveOrCreatePortalUser(MemberAltaUserFields fields, Company company, RoleEnum roleEnum) {
        lastRoleEnum = roleEnum;
        return userToReturn;
    }

    @Override
    public User resolveOrCreateMigrationUser(MemberAltaUserFields fields, Company company, RoleEnum roleEnum) {
        lastRoleEnum = roleEnum;
        return userToReturn;
    }
}
