package com.blue.open.Crm.Entity.DTO.Response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Producto comercializable publicado por el CRM.
 * <p>
 * Se usa en dos lugares, porque el CRM serializa el mismo objeto en ambos:
 * - es el elemento de la lista que responde GET integraciones/obtener-productos;
 * - es el contenido (data) del webhook PRODUCTO_CAMBIO, que el CRM emite al dar de alta o
 * modificar un producto.
 * <p>
 * El CRM filtra los productos por el sistema externo del token utilizado. Tener en cuenta que
 * ese endpoint esta cacheado del lado del CRM y la cache no distingue empresa.
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class CrmProductoResponseDTO {

    private String nombre;

    private String descripcion;

    // Codigo con el que se referencia el producto al facturar un pago unico
    private String codigo;

    // Precio base del producto
    private BigDecimal precio;

    // Porcentaje de IVA aplicable. Llega null si el producto no tiene IVA asociado
    private BigDecimal alicuotaIva;

    // Unidad de medida del producto, ej: unidad, hora
    private String unidad;
}
