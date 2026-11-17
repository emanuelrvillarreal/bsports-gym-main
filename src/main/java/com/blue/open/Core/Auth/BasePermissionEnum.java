package com.blue.open.Core.Auth;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum BasePermissionEnum implements IPermission {
    PERMISSION_VIEW("permission.view"),
    PERMISSION_CREATE("permission.create"),
    PERMISSION_EDIT("permission.edit"),
    PERMISSION_DELETE("permission.delete"),
    COMPANY_NOTNEED("company.notneed"),
    COMPANY_VIEW("company.view"),
    COMPANY_CREATE("company.create"),
    COMPANY_EDIT("company.edit"),
    COMPANY_DELETE("company.delete"),
    USER_VIEW("user.view"),
    USER_CREATE("user.create"),
    USER_EDIT("user.edit"),
    USER_DELETE("user.delete"),
    ROLE_VIEW("role.view"),
    ROLE_CREATE("role.create"),
    ROLE_EDIT("role.edit"),
    ROLE_DELETE("role.delete");

    private final String name;
}
