package com.blue.open.Code.Entity.DTO;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Una fila del reporte de movimientos (pantalla y hoja "Movimientos" del Excel). */
@Getter
@Setter
public class ReporteMovimientoRowDTO {
    private String uuid;
    /** Identificador legible, ej. MOV-000123. */
    private String operacion;
    private LocalDate fecha;
    private String tipo;
    /** Codigo de procedencia (CUOTA_SOCIAL, CUOTA_ACTIVIDAD, AMENITY, EVENTO, MANUAL). */
    private String categoria;
    private String categoriaTexto;
    private String concepto;
    private String actividad;
    private String socio;
    private String dni;
    private String categoriaSocio;
    private String medioPago;
    private BigDecimal importe;
    private String estado;
    private String registradoPor;
    private String observaciones;
}
