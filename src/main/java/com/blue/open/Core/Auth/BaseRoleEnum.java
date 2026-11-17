package com.blue.open.Core.Auth;

import lombok.AllArgsConstructor;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@AllArgsConstructor
public enum BaseRoleEnum implements IRole {

    SYSADMIN(all()),
    ADMIN(exclude(all(),
            BasePermissionEnum.COMPANY_NOTNEED,
            BasePermissionEnum.COMPANY_CREATE,
            BasePermissionEnum.COMPANY_DELETE,
            BasePermissionEnum.COMPANY_EDIT,
            BasePermissionEnum.PERMISSION_CREATE,
            BasePermissionEnum.PERMISSION_EDIT,
            BasePermissionEnum.PERMISSION_DELETE));

    private final Set<IPermission> permissions;

    @Override
    public Set<IPermission> getPermissions() {
        Set<IPermission> allPermissions = new HashSet<>(permissions);
        if (this == SYSADMIN || this == ADMIN) {
            allPermissions.addAll(PermissionRegistry.getAdditionalPermissions());
        }
        return allPermissions;
    }

    private static Set<IPermission> all() {
        return new HashSet<>(Arrays.asList(BasePermissionEnum.values()));
    }

    private static Set<IPermission> exclude(Set<IPermission> base, IPermission... exclusions) {
        Set<IPermission> temp = new HashSet<>(base);
        for (IPermission exclusion : exclusions) {
            temp.remove(exclusion);
        }
        return temp;
    }
}
