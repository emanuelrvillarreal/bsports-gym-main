package com.blue.open.Code.Entity.DTO;

import com.blue.open.Code.Entity.Enums.PartidoEstadoEnum;
import com.blue.open.Code.Entity.Enums.TorneoEstadoEnum;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Equipos, planteles y torneos: comandos y respuestas. */
public final class CompetenciasDTO {

    private CompetenciasDTO() {
    }

    // ================================================================ Comandos

    @Getter
    @Setter
    public static class EquipoCommand {
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 120)
        private String nombre;
        @NotBlank(message = "La disciplina es obligatoria")
        @Size(max = 80)
        private String disciplina;
        @Size(max = 36)
        private String categoriaUuid;
        @Size(max = 36)
        private String actividadUuid;
        @Size(max = 40)
        private String temporada;
        @Size(max = 4000)
        private String descripcion;
        private boolean activo = true;
        @Size(max = 10)
        private List<@NotBlank @Size(max = 36) String> entrenadorUuids;
    }

    @Getter
    @Setter
    public static class JugadorCommand {
        @NotBlank(message = "Elegí el socio")
        @Size(max = 36)
        private String memberUuid;
        @Min(value = 0, message = "El dorsal va de 0 a 999")
        @Max(value = 999, message = "El dorsal va de 0 a 999")
        private Integer dorsal;
        @Size(max = 60)
        private String posicion;
        private boolean capitan;
        private LocalDate fechaAlta;
    }

    @Getter
    @Setter
    public static class JugadorEdicionCommand {
        @Min(0) @Max(999)
        private Integer dorsal;
        @Size(max = 60)
        private String posicion;
        private boolean capitan;
    }

    @Getter
    @Setter
    public static class BajaJugadorCommand {
        @Size(max = 255)
        private String motivo;
        private LocalDate fecha;
    }

    @Getter
    @Setter
    public static class PaseCommand {
        @NotBlank(message = "Elegí el equipo de destino")
        @Size(max = 36)
        private String equipoDestinoUuid;
        @Min(0) @Max(999)
        private Integer dorsal;
        @Size(max = 60)
        private String posicion;
        private LocalDate fecha;
    }

    @Getter
    @Setter
    public static class TorneoCommand {
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 120)
        private String nombre;
        @NotBlank(message = "La disciplina es obligatoria")
        @Size(max = 80)
        private String disciplina;
        @Size(max = 36)
        private String categoriaUuid;
        @Size(max = 40)
        private String temporada;
        private LocalDate fechaInicio;
        private LocalDate fechaFin;
        @NotNull(message = "Elegí el estado")
        private TorneoEstadoEnum estado;
        @NotNull @Min(0) @Max(10)
        private Integer puntosVictoria = 3;
        @NotNull @Min(0) @Max(10)
        private Integer puntosEmpate = 1;
        @NotNull @Min(0) @Max(10)
        private Integer puntosDerrota = 0;
        @Size(max = 4000)
        private String descripcion;
    }

    /** Un equipo del club (equipoUuid) o un rival externo (solo nombre). */
    @Getter
    @Setter
    public static class ParticipanteCommand {
        @Size(max = 36)
        private String equipoUuid;
        @Size(max = 120)
        private String nombre;
    }

    @Getter
    @Setter
    public static class PartidoCommand {
        @NotBlank(message = "Indicá la fecha o ronda (ej. Fecha 1)")
        @Size(max = 60)
        private String ronda;
        private LocalDateTime fechaHora;
        @Size(max = 120)
        private String lugar;
        @NotBlank(message = "Elegí el local")
        @Size(max = 36)
        private String localUuid;
        @NotBlank(message = "Elegí el visitante")
        @Size(max = 36)
        private String visitanteUuid;
        @Min(value = 0, message = "El resultado no puede ser negativo")
        @Max(999)
        private Integer tantosLocal;
        @Min(value = 0, message = "El resultado no puede ser negativo")
        @Max(999)
        private Integer tantosVisitante;
        @NotNull(message = "Elegí el estado")
        private PartidoEstadoEnum estado;
        @Size(max = 2000)
        private String observaciones;
    }

    @Getter
    @Setter
    public static class EstadisticaCommand {
        @NotBlank
        @Size(max = 36)
        private String jugadorUuid;
        @Min(0) @Max(200)
        private int tantos;
        @Min(0) @Max(200)
        private int asistencias;
        @Min(0) @Max(10)
        private int amonestaciones;
        @Min(0) @Max(5)
        private int expulsiones;
        private boolean figura;
        @Size(max = 255)
        private String observaciones;
    }

    @Getter
    @Setter
    public static class EstadisticasCommand {
        @NotNull
        @Size(max = 100)
        @Valid
        private List<EstadisticaCommand> items;
    }

    @Getter
    @Setter
    public static class Filtro {
        @Size(max = 100)
        private String search;
        @Size(max = 80)
        private String disciplina;
        private boolean incluirInactivos;
    }

    // ================================================================ Respuestas

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Opcion {
        private String uuid;
        private String nombre;
    }

    @Getter
    @Setter
    public static class Opciones {
        private List<Opcion> profesores = new ArrayList<>();
        private List<Opcion> categorias = new ArrayList<>();
        private List<Opcion> actividades = new ArrayList<>();
        private List<Opcion> equipos = new ArrayList<>();
    }

    @Getter
    @Setter
    public static class SocioOpcion {
        private String memberUuid;
        private String nombre;
        private String apellido;
        private String dni;
        private String categoria;
    }

    @Getter
    @Setter
    public static class EquipoFila {
        private String uuid;
        private String nombre;
        private String disciplina;
        private String categoria;
        private String temporada;
        private List<String> entrenadores = new ArrayList<>();
        private int jugadores;
        private boolean activo;
    }

    @Getter
    @Setter
    public static class Jugador {
        private String uuid;
        private String memberUuid;
        private String nombre;
        private String apellido;
        private String dni;
        private Integer dorsal;
        private String posicion;
        private boolean capitan;
        private LocalDate fechaAlta;
        private LocalDate fechaBaja;
        private String motivoBaja;
        private int partidos;
        private int tantos;
        private int asistencias;
        private int amonestaciones;
        private int expulsiones;
        private int figuras;
    }

    @Getter
    @Setter
    public static class EquipoDetalle {
        private String uuid;
        private String nombre;
        private String disciplina;
        private String categoriaUuid;
        private String categoria;
        private String actividadUuid;
        private String actividad;
        private String temporada;
        private String descripcion;
        private boolean activo;
        private boolean editable;
        private List<Opcion> entrenadores = new ArrayList<>();
        private List<Jugador> plantel = new ArrayList<>();
        private List<Jugador> historial = new ArrayList<>();
        private List<TorneoFila> torneos = new ArrayList<>();
    }

    @Getter
    @Setter
    public static class TorneoFila {
        private String uuid;
        private String nombre;
        private String disciplina;
        private String categoria;
        private String temporada;
        private TorneoEstadoEnum estado;
        private LocalDate fechaInicio;
        private LocalDate fechaFin;
        private int participantes;
        private int partidosJugados;
        private int partidos;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Participante {
        private String uuid;
        private String nombre;
        private String equipoUuid;
        private boolean propio;
    }

    @Getter
    @Setter
    public static class Partido {
        private String uuid;
        private String ronda;
        private LocalDateTime fechaHora;
        private String lugar;
        private Participante local;
        private Participante visitante;
        private Integer tantosLocal;
        private Integer tantosVisitante;
        private PartidoEstadoEnum estado;
        private String observaciones;
        private boolean conEstadisticas;
        /** Algun participante es un equipo del club (se pueden cargar estadisticas individuales). */
        private boolean conEquipoPropio;
    }

    @Getter
    @Setter
    public static class Posicion {
        private String participanteUuid;
        private String nombre;
        private boolean propio;
        private int jugados;
        private int ganados;
        private int empatados;
        private int perdidos;
        private int favor;
        private int contra;
        private int diferencia;
        private int puntos;
    }

    @Getter
    @Setter
    public static class Goleador {
        private String jugadorUuid;
        private String nombre;
        private String equipo;
        private int tantos;
        private int asistencias;
        private int figuras;
    }

    @Getter
    @Setter
    public static class TorneoDetalle {
        private String uuid;
        private String nombre;
        private String disciplina;
        private String categoriaUuid;
        private String categoria;
        private String temporada;
        private LocalDate fechaInicio;
        private LocalDate fechaFin;
        private TorneoEstadoEnum estado;
        private int puntosVictoria;
        private int puntosEmpate;
        private int puntosDerrota;
        private String descripcion;
        private boolean editable;
        private List<Participante> participantes = new ArrayList<>();
        private List<Partido> partidos = new ArrayList<>();
        private List<Posicion> tabla = new ArrayList<>();
        private List<Goleador> goleadores = new ArrayList<>();
    }

    @Getter
    @Setter
    public static class EstadisticaJugador {
        private String jugadorUuid;
        private String nombre;
        private String equipo;
        private Integer dorsal;
        private int tantos;
        private int asistencias;
        private int amonestaciones;
        private int expulsiones;
        private boolean figura;
        private String observaciones;
    }

    @Getter
    @Setter
    public static class EstadisticasPartido {
        private Partido partido;
        private List<EstadisticaJugador> jugadores = new ArrayList<>();
    }
}
