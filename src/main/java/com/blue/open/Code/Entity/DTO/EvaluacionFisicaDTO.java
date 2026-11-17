package com.blue.open.Code.Entity.DTO;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Evaluaciones fisicas: configuracion del modulo, alta/edicion y respuestas. */
public final class EvaluacionFisicaDTO {

    private EvaluacionFisicaDTO() {
    }

    /** Configuracion por club. Por defecto el modulo esta apagado. */
    @Getter
    @Setter
    public static class Config {
        private boolean habilitado;
        /** Si los profesores pueden registrar y ver evaluaciones de sus alumnos. */
        private boolean profesoresPuedenRegistrar;
    }

    /** Lo que puede hacer quien consulta (para mostrar u ocultar la pantalla). */
    @Getter
    @Setter
    public static class Acceso {
        private boolean habilitado;
        private boolean puedeRegistrar;
        private boolean esAdmin;
        private Config config;
    }

    @Getter
    @Setter
    public static class Command {
        @NotNull(message = "La fecha es obligatoria")
        private LocalDate fecha;

        @Size(max = 120)
        private String profesional;

        @DecimalMin(value = "1", message = "El peso tiene que ser mayor a 1 kg")
        @DecimalMax(value = "400", message = "El peso no puede superar 400 kg")
        private BigDecimal pesoKg;

        @DecimalMin(value = "40", message = "La altura tiene que ser de al menos 40 cm")
        @DecimalMax(value = "260", message = "La altura no puede superar 260 cm")
        private BigDecimal alturaCm;

        @DecimalMin(value = "1", message = "El porcentaje de grasa va de 1 a 75")
        @DecimalMax(value = "75", message = "El porcentaje de grasa va de 1 a 75")
        private BigDecimal grasaCorporalPct;

        @DecimalMin(value = "1", message = "La masa muscular tiene que ser mayor a 1 kg")
        @DecimalMax(value = "200", message = "La masa muscular no puede superar 200 kg")
        private BigDecimal masaMuscularKg;

        @DecimalMin("10") @DecimalMax("250")
        private BigDecimal cinturaCm;
        @DecimalMin("10") @DecimalMax("250")
        private BigDecimal caderaCm;
        @DecimalMin("10") @DecimalMax("250")
        private BigDecimal pechoCm;
        @DecimalMin("5") @DecimalMax("100")
        private BigDecimal brazoCm;
        @DecimalMin("10") @DecimalMax("150")
        private BigDecimal musloCm;
        @DecimalMin("5") @DecimalMax("100")
        private BigDecimal pantorrillaCm;

        @Size(max = 4000)
        private String observaciones;

        @Size(max = 4000)
        private String recomendaciones;

        private LocalDate proximaEvaluacion;
    }

    @Getter
    @Setter
    public static class Respuesta {
        private String uuid;
        private LocalDate fecha;
        private String profesional;
        private String registradaPor;
        private BigDecimal pesoKg;
        private BigDecimal alturaCm;
        private BigDecimal imc;
        private BigDecimal grasaCorporalPct;
        private BigDecimal masaMuscularKg;
        private BigDecimal cinturaCm;
        private BigDecimal caderaCm;
        private BigDecimal pechoCm;
        private BigDecimal brazoCm;
        private BigDecimal musloCm;
        private BigDecimal pantorrillaCm;
        private String observaciones;
        private String recomendaciones;
        private LocalDate proximaEvaluacion;
        private boolean editable;
    }

    /** Historial de un socio, del mas nuevo al mas viejo. */
    @Getter
    @Setter
    public static class Historial {
        private String memberUuid;
        private String nombre;
        private String apellido;
        private String dni;
        private boolean puedeRegistrar;
        private List<Respuesta> evaluaciones = new ArrayList<>();
    }

    /** Fila del listado: socio con su ultima evaluacion. */
    @Getter
    @Setter
    public static class Fila {
        private String memberUuid;
        private String nombre;
        private String apellido;
        private String dni;
        private String actividades;
        private int cantidad;
        private LocalDate ultimaEvaluacion;
        private LocalDate proximaEvaluacion;
        private boolean vencida;
    }
}
