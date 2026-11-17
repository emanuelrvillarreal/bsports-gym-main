package com.blue.open.Crm.Entity.DTO.Response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

/**
 * Producto incluido dentro de un plan del CRM, con las unidades que otorga.
 * Se anida dentro de CrmPlanResponseDTO (GET integraciones/obtener-planes).
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class CrmPlanProductoDTO {

    private String nombre;

    // Codigo del producto (ej. bsports-socios). Lo usa MemberCupoService para identificar el
    // producto que define el limite de socios del plan, sin depender del nombre (texto libre)
    private String codigo;

    // Cantidad de unidades del producto que incluye el plan
    private Integer unidades;
}
