package com.blue.open.Code.Auth;

import com.blue.open.Core.Auth.IPermission;
import com.blue.open.Core.Auth.IRole;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Getter
@AllArgsConstructor
public enum RoleEnum implements IRole {

    ADM_RECEPCION(new HashSet<>(Arrays.asList(
            PermissionEnum.DASHBOARD_VIEW,
            PermissionEnum.MEMBER_VIEW,
            PermissionEnum.MEMBER_CREATE,
            PermissionEnum.MEMBER_EDIT,
            PermissionEnum.MEMBER_CHANGE_STATUS,
            PermissionEnum.FAMILY_CREATE,
            PermissionEnum.FAMILY_EDIT,
            PermissionEnum.ACTIVITY_VIEW,
            PermissionEnum.ACTIVITY_CREATE,
            PermissionEnum.ACTIVITY_EDIT,
            PermissionEnum.ACTIVITY_DELETE,
            PermissionEnum.ACTIVITY_ENROLL,
            PermissionEnum.ACTIVITY_UNENROLL,
            PermissionEnum.AMENITY_VIEW,
            PermissionEnum.AMENITY_CREATE,
            PermissionEnum.AMENITY_EDIT,
            PermissionEnum.AMENITY_DELETE,
            PermissionEnum.AMENITY_RESERVATION_CREATE,
            PermissionEnum.AMENITY_RESERVATION_COLLECT,
            PermissionEnum.AMENITY_RESERVATION_CANCEL,
            PermissionEnum.TREASURY_VIEW,
            PermissionEnum.TREASURY_COLLECT,
            PermissionEnum.TREASURY_EXPENSE,
            PermissionEnum.TREASURY_DEBT_CONSOLIDATED,
            PermissionEnum.TREASURY_NOTIFY_DELINQUENT,
            PermissionEnum.TREASURY_VOID,
            PermissionEnum.ACTIVITY_ATTENDANCE_VIEW,
            PermissionEnum.CATEGORIA_VIEW,
            PermissionEnum.CATEGORIA_CREATE,
            PermissionEnum.CATEGORIA_EDIT,
            PermissionEnum.CATEGORIA_DELETE,
            PermissionEnum.CUOTA_SOCIAL_RANGO_VIEW,
            PermissionEnum.CUOTA_SOCIAL_RANGO_CREATE,
            PermissionEnum.CUOTA_SOCIAL_RANGO_EDIT,
            PermissionEnum.CUOTA_SOCIAL_RANGO_DELETE,
            PermissionEnum.DESCUENTO_VIEW,
            PermissionEnum.DESCUENTO_CREATE,
            PermissionEnum.DESCUENTO_EDIT,
            PermissionEnum.DESCUENTO_DELETE,
            PermissionEnum.CLUB_ACCESS_REGISTER,
            PermissionEnum.MEMBERSHIPS_MANAGE,
            PermissionEnum.CLUB_ACCESS_VIEW,
            PermissionEnum.MEMBER_CERTIFICADO_UPLOAD,
            PermissionEnum.MEMBER_CERTIFICADO_CHANGE_STATUS))),

    PROFESOR(new HashSet<>(Arrays.asList(
            PermissionEnum.PORTAL_CARD_VIEW,
            PermissionEnum.ACTIVITY_VIEW,
            PermissionEnum.ACTIVITY_VIEW_ENROLLED,
            PermissionEnum.ACTIVITY_CREATE,
            PermissionEnum.ACTIVITY_EDIT,
            PermissionEnum.ACTIVITY_DELETE,
            PermissionEnum.ACTIVITY_ATTENDANCE_MANAGE,
            PermissionEnum.TRAINING_MANAGE,
            PermissionEnum.PHYSICAL_EVALUATION_MANAGE,
            PermissionEnum.TEAMS_VIEW_ASSIGNED,
            PermissionEnum.CATEGORIA_VIEW,
            PermissionEnum.CATEGORIA_CREATE,
            PermissionEnum.CATEGORIA_EDIT,
            PermissionEnum.CATEGORIA_DELETE))),

    SOCIO_TITULAR(new HashSet<>(Arrays.asList(
            PermissionEnum.PORTAL_CARD_VIEW,
            PermissionEnum.PORTAL_FEE_VIEW,
            PermissionEnum.PORTAL_MEMBERSHIPS_VIEW,
            PermissionEnum.PORTAL_MEMBERSHIPS_BUY,
            PermissionEnum.PORTAL_FEE_PAY_MERCADO_PAGO,
            PermissionEnum.PORTAL_ACTIVITIES_VIEW,
            PermissionEnum.ACTIVITY_VIEW,
            PermissionEnum.PORTAL_ACTIVITIES_ENROLL,
            PermissionEnum.PORTAL_ACTIVITIES_UNENROLL,
            PermissionEnum.PORTAL_AMENITIES_VIEW,
            PermissionEnum.PORTAL_AMENITIES_RESERVE,
            PermissionEnum.PORTAL_FAMILY_VIEW,
            PermissionEnum.PORTAL_FAMILY_CREATE,
            PermissionEnum.PORTAL_PROFILE_EDIT,
            PermissionEnum.PORTAL_CERTIFICADO_UPLOAD,
            PermissionEnum.PORTAL_TRAINING_VIEW,
            PermissionEnum.PORTAL_PHYSICAL_EVALUATION_VIEW))),

    FAMILIAR(new HashSet<>(Arrays.asList(
            PermissionEnum.PORTAL_CARD_VIEW,
            PermissionEnum.PORTAL_FEE_VIEW,
            PermissionEnum.PORTAL_MEMBERSHIPS_VIEW,
            PermissionEnum.PORTAL_ACTIVITIES_VIEW,
            PermissionEnum.PORTAL_PROFILE_EDIT,
            PermissionEnum.PORTAL_CERTIFICADO_UPLOAD,
            PermissionEnum.PORTAL_TRAINING_VIEW,
            PermissionEnum.PORTAL_PHYSICAL_EVALUATION_VIEW)));

    private final Set<? extends IPermission> permissions;

    @Override
    public Set<? extends IPermission> getPermissions() {
        return permissions;
    }
}
