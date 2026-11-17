package com.blue.open.Code.Entity.Enums;

public enum MedioPagoEnum {
    EFECTIVO,
    TRANSFERENCIA,
    MERCADO_PAGO,
    OTRO,
    // Detalle de "Otro" (cobro manual): se guardan tal cual en movimientos.medio_pago (varchar).
    DEBITO,
    DEBITO_AUTOMATICO,
    CREDITO,
    CHEQUE,
    NFC
}
