package com.blue.open.Code.Entity.Db;

import com.blue.open.Core.Entity.Db.AbstractEntity;
import com.blue.open.Core.Exceptions.CustomException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;

/**
 * Asignación recurrente de un {@link Descuento} a una inscripción de actividad, para la cuota
 * de esa actividad. Mismo comportamiento que {@link MemberDescuento}, contra
 * {@link InscripcionActividad} en vez de {@link Member}.
 */
@Getter
@Setter(AccessLevel.NONE)
@Table(name = "inscripcion_actividad_descuentos")
@Entity
public class InscripcionActividadDescuento extends AbstractEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inscripcion_actividad_id", nullable = false)
    private InscripcionActividad inscripcionActividad;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "descuento_id", nullable = false)
    private Descuento descuento;

    @Column(name = "fecha_desde", nullable = false)
    private LocalDate fechaDesde;

    @Column(name = "fecha_hasta")
    private LocalDate fechaHasta;

    protected InscripcionActividadDescuento() {
    }

    public static InscripcionActividadDescuento asignar(
            InscripcionActividad inscripcionActividad, Descuento descuento,
            LocalDate fechaDesde, LocalDate fechaHasta) {

        if (inscripcionActividad == null) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Inscripción requerida",
                    "InscripcionActividadDescuento | asignar | inscripcionActividad null");
        }
        if (descuento == null) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Descuento requerido",
                    "InscripcionActividadDescuento | asignar | descuento null");
        }
        if (inscripcionActividad.getCompany() == null || descuento.getCompany() == null
                || !inscripcionActividad.getCompany().getId().equals(descuento.getCompany().getId())) {
            throw new CustomException(HttpStatus.BAD_REQUEST,
                    "El descuento no pertenece al gimnasio de la inscripción",
                    "InscripcionActividadDescuento | asignar | inscripcionId: " + inscripcionActividad.getId()
                            + " descuentoId: " + descuento.getId());
        }
        if (fechaDesde == null) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "La fecha desde es requerida",
                    "InscripcionActividadDescuento | asignar | fechaDesde null");
        }
        if (fechaHasta != null && fechaHasta.isBefore(fechaDesde)) {
            throw new CustomException(HttpStatus.BAD_REQUEST,
                    "La fecha hasta no puede ser anterior a la fecha desde",
                    "InscripcionActividadDescuento | asignar | fechaDesde: " + fechaDesde
                            + " fechaHasta: " + fechaHasta);
        }

        InscripcionActividadDescuento asignacion = new InscripcionActividadDescuento();
        asignacion.inscripcionActividad = inscripcionActividad;
        asignacion.descuento = descuento;
        asignacion.fechaDesde = fechaDesde;
        asignacion.fechaHasta = fechaHasta;
        return asignacion;
    }

    public boolean estaVigente(LocalDate fecha) {
        return getStatus() == 0
                && !fecha.isBefore(fechaDesde)
                && (fechaHasta == null || !fecha.isAfter(fechaHasta));
    }

    /** Cierra la vigencia a partir de {@code fecha}, sin perder el historial de la asignación. */
    public void finalizar(LocalDate fecha) {
        if (fecha == null || fecha.isBefore(fechaDesde)) {
            throw new CustomException(HttpStatus.BAD_REQUEST,
                    "La fecha de finalización no puede ser anterior a la fecha desde",
                    "InscripcionActividadDescuento | finalizar | uuid: " + getUuid() + " fecha: " + fecha);
        }
        this.fechaHasta = fecha;
    }
}
