package com.blue.open.Code.Entity.DTO;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ActividadInscripcionCommand {

    @NotBlank(message = "Member requerido")
    private String memberUuid;

    @Valid
    private List<InscripcionActividadDescuentoCommand> descuentos;

    /**
     * Horarios de la actividad a los que va a asistir. Obligatorio al inscribir (al menos uno);
     * se ignora al dar de baja, que reutiliza este mismo comando.
     */
    @Size(max = 50, message = "Demasiados horarios elegidos")
    private List<@NotBlank(message = "UUID de horario inválido") @Size(max = 36, message = "UUID de horario inválido") String> horarioUuids;

    /** Motivo opcional al dar de baja; queda en el historial de inscripciones. */
    @Size(max = 255, message = "El motivo puede tener hasta 255 caracteres")
    private String motivoBaja;
}
