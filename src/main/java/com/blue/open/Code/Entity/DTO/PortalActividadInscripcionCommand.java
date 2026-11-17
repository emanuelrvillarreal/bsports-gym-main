package com.blue.open.Code.Entity.DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class PortalActividadInscripcionCommand {

    @NotBlank(message = "Actividad requerida")
    private String actividadUuid;

    private String memberUuid;

    /** Horarios elegidos: obligatorio al inscribir y al cambiar horarios; se ignora en la baja. */
    @Size(max = 50, message = "Demasiados horarios elegidos")
    private List<@NotBlank(message = "UUID de horario inválido") @Size(max = 36, message = "UUID de horario inválido") String> horarioUuids;
}
