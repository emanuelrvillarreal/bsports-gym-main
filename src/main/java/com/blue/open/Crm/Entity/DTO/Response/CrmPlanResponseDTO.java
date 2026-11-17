package com.blue.open.Crm.Entity.DTO.Response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Plan comercializable publicado por el CRM.
 * <p>
 * Se usa en dos lugares, porque el CRM serializa el mismo objeto en ambos:
 * - es el elemento de la lista que responde GET integraciones/obtener-planes;
 * - es el contenido (data) del webhook PLAN_CAMBIO, que el CRM emite al dar de alta o
 * modificar un plan.
 * <p>
 * El CRM filtra los planes por el sistema externo del token utilizado. Tener en cuenta que
 * ese endpoint esta cacheado del lado del CRM y la cache no distingue empresa.
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class CrmPlanResponseDTO {

    private String nombre;

    private String descripcion;

    // Codigo con el que se referencia el plan al suscribir o desuscribir un cliente
    private String codigo;

    // Precio vigente del plan. Llega null si el plan no tiene un precio vigente
    private BigDecimal precio;

    // Indica si el precio ya incluye impuestos. Llega null si no hay precio vigente
    private Boolean precioEsFinal;

    // Porcentaje de IVA aplicable al plan
    private BigDecimal alicuotaIva;

    private CrmPeriodicidadDTO periodicidad;

    private List<CrmPlanProductoDTO> productos = new ArrayList<>();
}
