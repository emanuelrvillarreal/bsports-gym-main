package com.blue.open.Code.Entity.Enums;

/** Cómo vence un certificado médico en el club. */
public enum CertificadoVigenciaModoEnum {
    /** Todos renuevan un día fijo del año (por ejemplo el 1 de enero): el certificado vence el día anterior. */
    FECHA_FIJA,
    /** Cada certificado vence una cantidad de meses después de su carga. */
    DURACION
}
