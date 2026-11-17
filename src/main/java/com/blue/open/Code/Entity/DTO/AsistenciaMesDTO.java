package com.blue.open.Code.Entity.DTO;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Asistencia de un mes. periodo con formato yyyy-MM. porcentaje es null si no hubo clases. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AsistenciaMesDTO {
    private String periodo;
    private int clases;
    private int presentes;
    private int ausentes;
    private int justificadas;
    private Double porcentaje;
}
