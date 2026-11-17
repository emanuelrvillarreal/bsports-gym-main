package com.blue.open.Code.Entity.DTO;

import com.blue.open.Code.Entity.Enums.PeriodicidadCuotaEnum;
import com.blue.open.Code.Entity.Enums.RecargoTipoEnum;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

@Getter
@Setter
public class ActividadDTO {

    private String actividadUuid;
    private String nombre;
    private String ubicacion;
    private Integer cupoMaximo;
    private String diasSemana;
    private LocalTime horaInicio;
    private LocalTime horaFin;
    private BigDecimal importeCuota;
    private PeriodicidadCuotaEnum periodicidadCuota;
    private Integer diasVencimientoCuota;
    /** Dia del mes en que vencen las cuotas; null si vencen N dias despues de generadas. */
    private Integer diaVencimientoFijo;
    private Integer diaCorteInscripcion;
    private boolean requiereCertificado;
    private Boolean recargoHabilitado;
    private RecargoTipoEnum recargoTipo;
    private BigDecimal recargoValor;
    private Integer recargoDiasGracia;
    private boolean completa;
    /** Solo en la respuesta de una edición que pidió actualizar cuotas pendientes. */
    private Integer cuotasPendientesActualizadas;
    /** Cuotas impagas que no se tocaron por tener descuento o recargo ya aplicado. */
    private Integer cuotasPendientesOmitidas;
    private List<CategoriaDTO> categorias;
    private List<ProfesorDTO> profesores;
    private List<ActividadInscriptoDTO> inscriptos;

    /** Franjas de la actividad con su cupo disponible; los campos de horario de arriba son un resumen. */
    private List<ActividadHorarioDTO> horarios;
}
