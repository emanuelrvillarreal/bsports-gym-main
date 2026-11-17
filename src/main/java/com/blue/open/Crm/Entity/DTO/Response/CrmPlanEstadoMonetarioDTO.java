package com.blue.open.Crm.Entity.DTO.Response;

import com.blue.open.Crm.Util.CrmEpochMillisDeserializer;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Plan asociado al estado monetario de un cliente.
 * Se anida dentro de CrmEstadoMonetarioResponseDTO
 * (GET integraciones/obtener-estado-monetario/{cuit}).
 * <p>
 * Atencion: hoy el CRM nunca completa esta lista, siempre llega vacia.
 * Se mantiene el DTO para no romper si el CRM la vuelve a poblar.
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class CrmPlanEstadoMonetarioDTO {

    private String nombre;

    private String uuid;

    private BigDecimal precio;

    @JsonDeserialize(using = CrmEpochMillisDeserializer.class)
    private LocalDateTime fechaInicio;
}
