package com.blue.open.Crm.Entity.Enums;

/**
 * Tipos de evento de webhook que emite el CRM (blue-crm) hacia este sistema.
 * Lo usa AbstractCrmWebhookProcesador para saber a que DTO parsear el campo data del evento
 * y a que metodo derivarlo.
 * <p>
 * El CRM normaliza el valor recibido en el ACK (trim, guion medio a guion bajo, mayusculas),
 * por lo que el name() del enum coincide exactamente con lo que espera.
 */
public enum CrmTipoEventoEnum {

    // Pago imputado a una factura del cliente en el CRM
    PAGO_IMPUTADO,

    // Deuda generada para el cliente en el CRM
    DEUDA_GENERADA,

    // Alta o modificacion de un plan en el CRM
    PLAN_CAMBIO,

    // Alta o modificacion de un producto en el CRM
    PRODUCTO_CAMBIO,

    // Alta de un cliente en este sistema: se pago la factura inicial de su suscripcion de inscripcion
    ALTA_CLIENTE;

    /**
     * Resuelve el tipo a partir del string que mando el CRM.
     * <p>
     * Espera: el valor del campo tipo del evento, con o sin espacios.
     * Responde: el enum correspondiente, o null si el tipo es nulo, vacio o desconocido.
     * Devuelve null en lugar de lanzar excepcion para que el procesador pueda confirmarle al
     * CRM el evento con un error descriptivo en vez de cortar.
     */
    public static CrmTipoEventoEnum desdeTexto(String tipo) {
        if (tipo == null || tipo.isBlank()) {
            return null;
        }
        try {
            return CrmTipoEventoEnum.valueOf(tipo.trim().replace('-', '_').toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
