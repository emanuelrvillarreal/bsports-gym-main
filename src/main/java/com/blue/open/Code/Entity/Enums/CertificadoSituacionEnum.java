package com.blue.open.Code.Entity.Enums;

/** Situación del certificado médico de un socio, para mostrar y para decidir si puede hacer una actividad. */
public enum CertificadoSituacionEnum {
    /** No cargó ninguno. */
    SIN_CERTIFICADO,
    /** Lo cargó el socio y el club todavía no lo confirmó: no cuenta como vigente. */
    PENDIENTE_VALIDACION,
    VIGENTE,
    /** Vigente, pero vence dentro de los próximos días. */
    POR_VENCER,
    VENCIDO,
    DENEGADO
}
