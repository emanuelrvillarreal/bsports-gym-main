package com.blue.open.Crm.Entity.Enums;

/**
 * Sentido de una interaccion con el CRM (blue-crm) registrada en crm_integracion_logs.
 */
public enum CrmLogDireccionEnum {

    // El CRM nos llamo a nosotros: webhooks recibidos
    ENTRADA,

    // Nosotros llamamos al CRM: ACKs y llamadas del cliente
    SALIDA
}
