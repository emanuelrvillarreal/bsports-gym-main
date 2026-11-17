package com.blue.open.Crm.Entity.DTO.Request;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Pedido de generacion de una factura de productos (pago unico, sin suscripcion) en el CRM.
 * Se envia en POST integraciones/facturas/productos.
 * <p>
 * El CRM valida que todos los codigos de producto existan; si alguno no existe responde 400
 * "Productos no encontrados" detallando cuales.
 */
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CrmFacturaProductosRequestDTO {

    // CUIT sin guiones, 11 digitos
    private String cuit;

    // Productos a facturar. Debe tener al menos un item
    private List<CrmItemProductoRequestDTO> items = new ArrayList<>();
}
