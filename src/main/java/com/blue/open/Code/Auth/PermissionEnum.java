package com.blue.open.Code.Auth;

import com.blue.open.Core.Auth.IPermission;
import com.blue.open.Core.Auth.PermissionRegistry;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum PermissionEnum implements IPermission {

    CONFIGURATION_VIEW("configuration.view"),
    CONFIGURATION_CREATE("configuration.create"),

    DASHBOARD_VIEW("dashboard.view"),

    MEMBER_VIEW("member.view"),
    MEMBER_CREATE("member.create"),
    MEMBER_EDIT("member.edit"),
    MEMBER_CHANGE_STATUS("member.change_status"),
    MEMBER_CERTIFICADO_UPLOAD("member.certificado.upload"),
    MEMBER_CERTIFICADO_CHANGE_STATUS("member.certificado.change_status"),

    FAMILY_CREATE("family.create"),
    FAMILY_EDIT("family.edit"),
    FAMILY_CONVERT_TO_MEMBER("family.convert_to_member"),

    TREASURY_VIEW("treasury.view"),
    TREASURY_COLLECT("treasury.collect"),
    TREASURY_EXPENSE("treasury.expense"),
    TREASURY_VOID("treasury.void"),
    TREASURY_DEBT_CONSOLIDATED("treasury.debt_consolidated"),
    TREASURY_NOTIFY_DELINQUENT("treasury.notify_delinquent"),
    TREASURY_ACCESS_SWITCH("treasury.access_switch"),
    /** Reportes economicos, comparaciones y exportacion a Excel. Solo ADMIN (lo recibe por defecto). */
    TREASURY_REPORTS("treasury.reports"),

    ACTIVITY_VIEW("activity.view"),
    ACTIVITY_CREATE("activity.create"),
    ACTIVITY_EDIT("activity.edit"),
    ACTIVITY_DELETE("activity.delete"),
    ACTIVITY_ENROLL("activity.enroll"),
    ACTIVITY_UNENROLL("activity.unenroll"),
    ACTIVITY_VIEW_ENROLLED("activity.view_enrolled"),

    ACTIVITY_ATTENDANCE_MANAGE("activity.attendance.manage"),
    ACTIVITY_ATTENDANCE_VIEW("activity.attendance.view"),
    /** Estadisticas institucionales de asistencia y trayectoria deportiva. Solo ADMIN (lo recibe por defecto). */
    ACTIVITY_ATTENDANCE_STATS("activity.attendance.stats"),

    /** Biblioteca de ejercicios, rutinas, asignaciones y seguimiento. PROFESOR (sus alumnos) y ADMIN. */
    TRAINING_MANAGE("training.manage"),
    /** Inventario de equipamiento y mantenimientos. Solo ADMIN (lo recibe por defecto). */
    EQUIPMENT_MANAGE("equipment.manage"),
    /** Equipos, planteles y torneos: alta y gestion. Solo ADMIN (lo recibe por defecto). */
    TEAMS_MANAGE("teams.manage"),
    /** Consultar los equipos donde es entrenador y sus torneos. PROFESOR. */
    TEAMS_VIEW_ASSIGNED("teams.view_assigned"),
    /** Configurar la oferta de pases del club y si el QR exige pase. Solo ADMIN (lo recibe por defecto). */
    PASSES_MANAGE("passes.manage"),
    /** Vender, renovar, congelar y dar de baja membresías desde el legajo del socio. ADMIN y recepción. */
    MEMBERSHIPS_MANAGE("memberships.manage"),
    /** Portal: ver las membresías propias (y del grupo si es titular). */
    PORTAL_MEMBERSHIPS_VIEW("portal.memberships.view"),
    /** Portal: contratar o renovar pases (se pagan como cualquier cuota). Solo titular. */
    PORTAL_MEMBERSHIPS_BUY("portal.memberships.buy"),
    /** Evaluaciones fisicas. ADMIN siempre; PROFESOR solo si el club lo habilita y para sus alumnos. */
    PHYSICAL_EVALUATION_MANAGE("physical_evaluation.manage"),

    CATEGORIA_VIEW("categoria.view"),
    CATEGORIA_CREATE("categoria.create"),
    CATEGORIA_EDIT("categoria.edit"),
    CATEGORIA_DELETE("categoria.delete"),

    CUOTA_SOCIAL_RANGO_VIEW("cuota_social_rango.view"),
    CUOTA_SOCIAL_RANGO_CREATE("cuota_social_rango.create"),
    CUOTA_SOCIAL_RANGO_EDIT("cuota_social_rango.edit"),
    CUOTA_SOCIAL_RANGO_DELETE("cuota_social_rango.delete"),

    DESCUENTO_VIEW("descuento.view"),
    DESCUENTO_CREATE("descuento.create"),
    DESCUENTO_EDIT("descuento.edit"),
    DESCUENTO_DELETE("descuento.delete"),

    FACTURA_VIEW("factura.view"),
    FACTURA_PAGAR("factura.pagar"),

    AMENITY_VIEW("amenity.view"),
    AMENITY_CREATE("amenity.create"),
    AMENITY_EDIT("amenity.edit"),
    AMENITY_DELETE("amenity.delete"),
    AMENITY_RESERVATION_CREATE("amenity.reservation.create"),
    AMENITY_RESERVATION_COLLECT("amenity.reservation.collect"),
    AMENITY_RESERVATION_CANCEL("amenity.reservation.cancel"),

    CLUB_CONFIGURATION("club.configuration"),
    CLUB_INTERNAL_USER_MANAGE("club.internal_user.manage"),
    CLUB_ACCESS_REGISTER("club.access.register"),
    CLUB_ACCESS_VIEW("club.access.view"),

    PORTAL_CARD_VIEW("portal.card.view"),
    PORTAL_FEE_VIEW("portal.fee.view"),
    PORTAL_FEE_PAY_MERCADO_PAGO("portal.fee.pay_mercado_pago"),
    PORTAL_ACTIVITIES_VIEW("portal.activities.view"),
    PORTAL_ACTIVITIES_ENROLL("portal.activities.enroll"),
    PORTAL_ACTIVITIES_UNENROLL("portal.activities.unenroll"),
    PORTAL_AMENITIES_VIEW("portal.amenities.view"),
    PORTAL_AMENITIES_RESERVE("portal.amenities.reserve"),
    PORTAL_FAMILY_VIEW("portal.family.view"),
    PORTAL_FAMILY_CREATE("portal.family.create"),
    PORTAL_PROFILE_EDIT("portal.profile.edit"),
    PORTAL_CERTIFICADO_UPLOAD("portal.certificado.upload"),
    /** Portal: ver las rutinas asignadas (propias o de su grupo familiar). */
    PORTAL_TRAINING_VIEW("portal.training.view"),
    /** Portal: ver las evaluaciones fisicas propias o de su grupo familiar (si el club tiene el modulo activo). */
    PORTAL_PHYSICAL_EVALUATION_VIEW("portal.physical_evaluation.view");

    private final String name;

    static {
        PermissionRegistry.register(values());
    }
}
