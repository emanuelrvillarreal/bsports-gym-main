package com.blue.open.Code.Entity.DTO;

import com.blue.open.Code.Entity.Enums.DificultadEjercicioEnum;
import com.blue.open.Code.Entity.Enums.TipoEjercicioEnum;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/** Ejercicios de la biblioteca del club: alta/edicion y respuesta. */
public final class EjercicioDTO {

    private EjercicioDTO() {
    }

    @Getter
    @Setter
    public static class Command {
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 120, message = "El nombre puede tener hasta 120 caracteres")
        private String nombre;

        @Size(max = 4000)
        private String descripcion;

        @Size(max = 80)
        private String disciplina;

        @Size(max = 120)
        private String grupoMuscular;

        @NotNull(message = "Elegí el tipo de ejercicio")
        private TipoEjercicioEnum tipo;

        @NotNull(message = "Elegí la dificultad")
        private DificultadEjercicioEnum dificultad;

        @Size(max = 500)
        @Pattern(regexp = "^$|^https?://.+", message = "La imagen tiene que ser un enlace http o https")
        private String imagenUrl;

        @Size(max = 500)
        @Pattern(regexp = "^$|^https?://.+", message = "El video tiene que ser un enlace http o https")
        private String videoUrl;

        @Size(max = 4000)
        private String indicaciones;
    }

    @Getter
    @Setter
    public static class Respuesta {
        private String uuid;
        private String nombre;
        private String descripcion;
        private String disciplina;
        private String grupoMuscular;
        private TipoEjercicioEnum tipo;
        private DificultadEjercicioEnum dificultad;
        private String imagenUrl;
        private String videoUrl;
        private String indicaciones;
        private String creadoPor;
        /** El usuario actual puede editarlo o borrarlo (lo cargo el o es ADMIN). */
        private boolean editable;
    }
}
