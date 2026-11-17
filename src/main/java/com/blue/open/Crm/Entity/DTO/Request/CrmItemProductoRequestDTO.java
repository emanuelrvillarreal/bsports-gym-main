package com.blue.open.Crm.Entity.DTO.Request;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

/**
 * Item de producto de una factura de pago unico enviada al CRM.
 * Se anida dentro de CrmFacturaProductosRequestDTO (POST integraciones/facturas/productos).
 * <p>
 * El CRM normaliza el codigo (trim y mayusculas) y suma las cantidades si el mismo codigo
 * aparece repetido en la lista.
 */
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CrmItemProductoRequestDTO {

    // Codigo del producto segun el catalogo del CRM (ver obtenerProductos)
    private String codigoProducto;

    // Debe ser mayor a cero. Si viaja null, el CRM asume 1
    private Integer cantidad;
}
