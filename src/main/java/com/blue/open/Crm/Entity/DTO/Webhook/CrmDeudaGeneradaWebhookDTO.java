package com.blue.open.Crm.Entity.DTO.Webhook;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Contenido del webhook DEUDA_GENERADA del CRM (blue-crm): se genero deuda para el cliente.
 * Es el campo data de CrmWebhookEventoDTO.
 * <p>
 * Lo genera DeudaGeneradaIntegracionService del CRM, que emite un evento por cada sistema
 * externo alcanzado por los items de la factura. Es decir que una misma factura puede disparar
 * varios eventos, y cada uno trae solo los codigos que le corresponden a ese sistema.
 * <p>
 * A diferencia de PAGO_IMPUTADO, el ACK de este evento no exige numeroFactura.
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class CrmDeudaGeneradaWebhookDTO {

    // Identificador del cliente en el CRM. Reemplaza al CUIT en los eventos nuevos
    private String uuidCliente;

    // CUIT del cliente, 11 digitos sin guiones. Solo llega en eventos emitidos antes de que el
    // CRM pasara a informar uuidCliente
    private String cuit;

    // Uuid de la factura que genero la deuda. Es tambien la referencia con la que el CRM
    // deduplica el evento, asi que identifica la factura de forma estable entre reintentos
    private String uuidFactura;

    // Codigos de los planes que originaron la deuda
    private List<String> codigosPlanes = new ArrayList<>();

    // Codigos de los productos que originaron la deuda
    private List<String> codigosProductos = new ArrayList<>();

    // Importe total de la factura en el CRM, con IVA y con el descuento de la suscripcion ya
    // aplicado. Puede venir null en eventos emitidos antes de que el CRM lo informara
    private BigDecimal importeTotal;
}
