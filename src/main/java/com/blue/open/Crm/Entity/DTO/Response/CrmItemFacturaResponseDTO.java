package com.blue.open.Crm.Entity.DTO.Response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Item de una factura de productos generada por el CRM.
 * Se anida dentro de CrmFacturaProductosResponseDTO (POST integraciones/facturas/productos).
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class CrmItemFacturaResponseDTO {

    private String codigoProducto;

    private String nombreProducto;

    private Integer cantidad;

    private BigDecimal precioUnitario;

    // Resultado de precio por cantidad menos el descuento aplicado por el CRM
    private BigDecimal subtotal;
}
