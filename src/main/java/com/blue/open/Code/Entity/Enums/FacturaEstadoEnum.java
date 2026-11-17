package com.blue.open.Code.Entity.Enums;

/**
 * Estado de una factura del club.
 * Lo usa la entidad Factura para su maquina de estados: nace en PENDIENTE y desde ahi pasa a
 * PAGADA o VENCIDA.
 */
public enum FacturaEstadoEnum {

    // Generada y todavia impaga. Es el unico estado con el que se puede crear una factura
    PENDIENTE,

    // Cobrada
    PAGADA,

    // Paso su fecha de vencimiento sin cobrarse
    VENCIDA
}
