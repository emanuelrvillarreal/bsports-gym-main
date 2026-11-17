package com.blue.open.Code.Entity.DTO;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class EventoActividadEdicionCommand {

    @NotBlank(message = "Título requerido")
    @Size(max = 255, message = "Título demasiado largo")
    private String titulo;

    @NotBlank(message = "Descripción requerida")
    private String descripcion;

    @NotBlank(message = "Ubicación requerida")
    @Size(max = 255, message = "Ubicación demasiado larga")
    private String ubicacion;

    @NotNull(message = "Inicio requerido")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy HH:mm:ss")
    private LocalDateTime inicio;

    @NotNull(message = "Fin requerido")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy HH:mm:ss")
    private LocalDateTime fin;

    @AssertTrue(message = "fin debe ser posterior a inicio")
    public boolean isRangoFechasValido() {
        if (inicio == null || fin == null) {
            return true;
        }
        return fin.isAfter(inicio);
    }
}
