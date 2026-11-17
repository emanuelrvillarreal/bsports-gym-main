package com.blue.open.Core.Auth;

import java.util.HashSet;
import java.util.Set;
import java.util.Arrays;
import java.util.Collections;

public class PermissionRegistry {
    private static final Set<IPermission> additionalPermissions = Collections.synchronizedSet(new HashSet<>());

    public static void register(IPermission[] permissions) {
        if (permissions != null) {
            additionalPermissions.addAll(Arrays.asList(permissions));
        }
    }

    public static Set<IPermission> getAdditionalPermissions() {
        return new HashSet<>(additionalPermissions);
    }
}
