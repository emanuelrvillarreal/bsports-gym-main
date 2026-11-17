package com.blue.open.Crm.Entity.Enums;

/**
 * Tipos de descuento que acepta el CRM (blue-crm) al crear una suscripcion.
 * <p>
 * El name() del enum coincide con lo que espera el CRM: un valor distinto hace que el CRM
 * rechace el body por error de deserializacion.
 */
public enum CrmTipoDescuentoEnum {

    // Importe fijo que se resta al total de cada factura
    MONTO,

    // Porcentaje del total de cada factura, entre 1 y 100
    PORCENTAJE
}
