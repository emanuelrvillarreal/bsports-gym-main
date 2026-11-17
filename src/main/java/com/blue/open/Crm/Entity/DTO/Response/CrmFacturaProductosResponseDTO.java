package com.blue.open.Crm.Entity.DTO.Response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Factura de productos (pago unico) generada por el CRM.
 * Responde POST integraciones/facturas/productos.
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class CrmFacturaProductosResponseDTO {

    private String uuidFactura;

    private String uuidCuentaCorriente;

    private String cuit;

    // Monto total de la factura
    private BigDecimal monto;

    // Se deja como String a proposito. Codigos conocidos: PEN (pendiente), PAG (pagada)
    private String estado;

    private List<CrmItemFacturaResponseDTO> items = new ArrayList<>();
}
