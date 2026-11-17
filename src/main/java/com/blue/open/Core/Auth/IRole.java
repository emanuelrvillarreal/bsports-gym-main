package com.blue.open.Core.Auth;

import java.util.Set;

public interface IRole {
    String name();
    Set<? extends IPermission> getPermissions();
}
