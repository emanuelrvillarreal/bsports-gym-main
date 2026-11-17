package com.blue.open.Code.Entity.DTO;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Suscripcion del club a su plan del CRM, tal como esta reflejada en CompanySuscripcionCrm.
 * <p>
 * Expone los mismos campos que la entidad, salvo uuidFacturaInicialCrm: es un dato de auditoria
 * del alta que no aporta nada a una vista de listado/paginado.
 */
@Getter
@Setter
public class CompanySuscripcionCrmDTO {

    private String codigoPlan;
    private String nombrePlan;
    private String uuidSuscripcionCrm;
    private String estado;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy")
    private LocalDate fechaProximaFacturacion;
}
