package com.blue.open.Crm.Entity.DTO.Webhook;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Contenido del webhook PAGO_IMPUTADO del CRM (blue-crm): un pago fue imputado a una factura
 * del cliente. Es el campo data de CrmWebhookEventoDTO.
 * <p>
 * Lo genera PagoImputadoIntegracionService del CRM.
 * <p>
 * Atencion: numeroFactura es obligatorio en el ACK de este tipo de evento y el CRM valida que
 * coincida con el que guardo, si no responde 400.
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class CrmPagoImputadoWebhookDTO {

    // Numero de la factura pagada. El CRM cae al uuid de la factura si no tiene numero, asi que
    // es ambiguo para identificarla: sirve para el ACK, no para buscar
    private String numeroFactura;

    // Uuid de la factura en el CRM. Es el identificador univoco y el que se usa para encontrar
    // la Factura local, que lo guardo al procesar el DEUDA_GENERADA
    private String uuidFactura;

    // Identificador del cliente en el CRM. Reemplaza al CUIT en los eventos nuevos
    private String uuidCliente;

    // CUIT del cliente, 11 digitos sin guiones. Solo llega en eventos emitidos antes de que el
    // CRM pasara a informar uuidCliente
    private String cuit;

    // Codigos de los planes alcanzados por el pago
    private List<String> codigosPlanes = new ArrayList<>();

    // Codigos de los productos alcanzados por el pago
    private List<String> codigosProductos = new ArrayList<>();
}
