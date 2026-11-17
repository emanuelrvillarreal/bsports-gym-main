package com.blue.open.Code.Service.Membresias;

import com.blue.open.Code.Entity.Db.Actividad;
import com.blue.open.Code.Entity.Db.Amenity;
import com.blue.open.Code.Entity.Db.Membresia;
import com.blue.open.Code.Entity.Db.MembresiaCongelamiento;
import com.blue.open.Code.Entity.Db.Pase;
import com.blue.open.Code.Entity.Enums.CuotaEstadoEnum;
import com.blue.open.Code.Entity.Enums.DiaSemanaEnum;
import com.blue.open.Code.Entity.Enums.MembresiaEstadoEnum;
import com.blue.open.Code.Entity.Enums.MembresiaSituacionEnum;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;

/**
 * Reglas puras de las membresias (sin base de datos), para poder probarlas aisladas. La cuota de la
 * membresia se recibe como su estado: solo PAGADA habilita el pase.
 */
public final class MembresiaReglas {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    private MembresiaReglas() {
    }

    /** Situacion de la membresia en {@code hoy}. El orden importa: la primera condicion que falla manda. */
    public static MembresiaSituacionEnum situacion(Membresia m, CuotaEstadoEnum estadoCuota, LocalDate hoy) {
        if (m.getEstado() == MembresiaEstadoEnum.BAJA) return MembresiaSituacionEnum.BAJA;
        if (m.getEstado() == MembresiaEstadoEnum.REEMPLAZADA) return MembresiaSituacionEnum.REEMPLAZADA;
        if (hoy.isAfter(m.getFechaFin())) return MembresiaSituacionEnum.VENCIDA;
        if (estadoCuota != null && estadoCuota != CuotaEstadoEnum.PAGADA) return MembresiaSituacionEnum.PENDIENTE_PAGO;
        if (hoy.isBefore(m.getFechaInicio())) return MembresiaSituacionEnum.PROGRAMADA;
        if (congeladaEl(m, hoy)) return MembresiaSituacionEnum.CONGELADA;
        Integer disponibles = m.ingresosDisponibles();
        if (disponibles != null && disponibles <= 0) return MembresiaSituacionEnum.AGOTADA;
        return MembresiaSituacionEnum.VIGENTE;
    }

    public static boolean congeladaEl(Membresia m, LocalDate fecha) {
        return m.congelamientosActivos().stream().anyMatch(c -> c.cubre(fecha));
    }

    /**
     * Motivo por el que la membresia no habilita un ingreso en {@code ahora}, o null si lo habilita.
     * {@code actividad} / {@code amenity} son opcionales: solo se controlan si el ingreso es a una de ellas.
     */
    public static String motivoRechazo(Membresia m, CuotaEstadoEnum estadoCuota, LocalDateTime ahora,
                                       Actividad actividad, Amenity amenity) {
        LocalDate hoy = ahora.toLocalDate();
        MembresiaSituacionEnum situacion = situacion(m, estadoCuota, hoy);
        Pase pase = m.getPase();
        switch (situacion) {
            case VIGENTE -> { }
            case PENDIENTE_PAGO -> { return "El pase " + pase.getNombre() + " no está pago"; }
            case PROGRAMADA -> { return "El pase " + pase.getNombre() + " empieza el " + FECHA.format(m.getFechaInicio()); }
            case VENCIDA -> { return "El pase " + pase.getNombre() + " venció el " + FECHA.format(m.getFechaFin()); }
            case CONGELADA -> { return "El pase " + pase.getNombre() + " está congelado" + hastaCongelamiento(m, hoy); }
            case AGOTADA -> { return "El pase " + pase.getNombre() + " no tiene ingresos disponibles"; }
            case BAJA -> { return "El pase " + pase.getNombre() + " fue dado de baja"; }
            case REEMPLAZADA -> { return "El pase " + pase.getNombre() + " fue reemplazado por otro plan"; }
        }

        DiaSemanaEnum dia = Arrays.stream(DiaSemanaEnum.values())
                .filter(d -> d.toDayOfWeek() == ahora.getDayOfWeek()).findFirst().orElseThrow();
        if (!pase.dias().contains(dia)) {
            return "El pase " + pase.getNombre() + " no habilita los " + dia.name().toLowerCase();
        }
        if (!dentroDeHorario(pase.getHoraDesde(), pase.getHoraHasta(), ahora.toLocalTime())) {
            return "El pase " + pase.getNombre() + " habilita de " + HORA.format(pase.getHoraDesde())
                    + " a " + HORA.format(pase.getHoraHasta());
        }
        if (actividad != null && !pase.getActividades().isEmpty()
                && pase.getActividades().stream().noneMatch(a -> a.getId().equals(actividad.getId()))) {
            return "La actividad " + actividad.getNombre() + " no está incluida en el pase " + pase.getNombre();
        }
        if (amenity != null && !pase.getAmenities().isEmpty()
                && pase.getAmenities().stream().noneMatch(a -> a.getId().equals(amenity.getId()))) {
            return "La instalación " + amenity.getNombre() + " no está incluida en el pase " + pase.getNombre();
        }
        return null;
    }

    /** Sin horario configurado vale todo el dia. Soporta franjas que cruzan la medianoche (22:00 a 02:00). */
    static boolean dentroDeHorario(LocalTime desde, LocalTime hasta, LocalTime hora) {
        if (desde == null || hasta == null) return true;
        if (!desde.isAfter(hasta)) return !hora.isBefore(desde) && !hora.isAfter(hasta);
        return !hora.isBefore(desde) || !hora.isAfter(hasta);
    }

    /** Dias de un congelamiento, contando ambos extremos. */
    public static int diasEntre(LocalDate desde, LocalDate hasta) {
        return (int) ChronoUnit.DAYS.between(desde, hasta) + 1;
    }

    /** Un congelamiento no puede pisarse con otro de la misma membresia. */
    public static boolean seSuperpone(Membresia m, LocalDate desde, LocalDate hasta) {
        return m.congelamientosActivos().stream()
                .anyMatch(c -> !desde.isAfter(c.getFechaHasta()) && !hasta.isBefore(c.getFechaDesde()));
    }

    private static String hastaCongelamiento(Membresia m, LocalDate hoy) {
        return m.congelamientosActivos().stream().filter(c -> c.cubre(hoy))
                .map(MembresiaCongelamiento::getFechaHasta).max(LocalDate::compareTo)
                .map(f -> " hasta el " + FECHA.format(f)).orElse("");
    }
}
