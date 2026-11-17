package com.blue.open.Code.Service.Actividades;

import com.blue.open.Code.Entity.Db.Actividad;
import com.blue.open.Code.Entity.Db.ActividadHorario;
import com.blue.open.Code.Entity.Db.Slot;
import com.blue.open.Code.Entity.Enums.SlotEstadoEnum;
import com.blue.open.Code.Repository.ActividadHorarioRepository;
import com.blue.open.Code.Repository.SlotRepository;
import com.blue.open.Code.Util.DiasSemanaUtil;
import com.blue.open.Core.Exceptions.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Genera las clases (Slot) de una actividad a partir de sus horarios: una clase por cada
 * ocurrencia semanal de cada ActividadHorario activo, en una ventana de 3 meses.
 *
 * Depende de: ActividadHorarioRepository para leer los horarios y SlotRepository para persistir.
 */
@Service
public class SlotGeneracionService {

    @Autowired
    private SlotRepository slotRepository;

    @Autowired
    private ActividadHorarioRepository actividadHorarioRepository;

    /**
     * Genera las clases de todos los horarios activos desde {@code desde} (hoy si es null) hasta 3
     * meses despues. Las que ya existen no se duplican.
     * Responde: cantidad de clases creadas.
     */
    @Transactional
    public int generarVentanaCompleta(Actividad actividad, LocalDate desde) {
        validateActividad(actividad);
        LocalDate inicioVentana = desde == null ? LocalDate.now() : desde;
        LocalDate finVentana = inicioVentana.plusMonths(3);

        int creados = 0;
        for (ActividadHorario horario : actividadHorarioRepository.findActivosByActividadId(actividad.getId())) {
            creados += generarYPersistir(actividad, horario, calcularFechasSemanales(horario, inicioVentana, finVentana));
        }
        return creados;
    }

    /**
     * Rehace las clases futuras tras cambiar los horarios: borra las que todavia no empezaron y no
     * tienen asistencia, y vuelve a generar la ventana con los horarios vigentes.
     * Responde: cantidad de clases creadas.
     */
    @Transactional
    public int regenerarTrasCambioHorarioODias(Actividad actividad) {
        validateActividad(actividad);
        slotRepository.deleteFuturosSinAsistenciaByActividadId(actividad.getId(), LocalDateTime.now());
        return generarVentanaCompleta(actividad, LocalDate.now());
    }

    /**
     * Extiende la ventana un dia (lo usa el job diario): crea las clases de {@code diaObjetivo}
     * para los horarios que caen ese dia de la semana.
     * Responde: true si creo al menos una clase.
     */
    @Transactional
    public boolean generarDiaExtension(Actividad actividad, LocalDate diaObjetivo) {
        validateActividad(actividad);
        if (diaObjetivo == null) {
            return false;
        }
        int creados = 0;
        for (ActividadHorario horario : actividadHorarioRepository.findActivosByActividadId(actividad.getId())) {
            if (horario.getDiaSemana().toDayOfWeek() == diaObjetivo.getDayOfWeek()) {
                creados += generarYPersistir(actividad, horario, List.of(diaObjetivo));
            }
        }
        return creados > 0;
    }

    private int generarYPersistir(Actividad actividad, ActividadHorario horario, List<LocalDate> fechas) {
        assertHorarioValido(horario);
        if (fechas.isEmpty()) {
            return 0;
        }
        LocalDateTime ventanaInicio = fechas.get(0).atTime(horario.getHoraInicio());
        LocalDateTime ventanaFin = fechas.get(fechas.size() - 1).atTime(horario.getHoraInicio());
        Set<LocalDateTime> existentes = new HashSet<>(
                slotRepository.findIniciosByHorarioIdAndInicioBetween(horario.getId(), ventanaInicio, ventanaFin));

        List<Slot> aPersistir = new ArrayList<>();
        for (LocalDate dia : fechas) {
            LocalDateTime inicio = dia.atTime(horario.getHoraInicio());
            if (existentes.contains(inicio)) {
                continue;
            }
            Slot slot = new Slot();
            slot.setCompany(actividad.getCompany());
            slot.setActividad(actividad);
            slot.setHorario(horario);
            slot.setInicio(inicio);
            slot.setFin(dia.atTime(horario.getHoraFin()));
            slot.setEstado(SlotEstadoEnum.CONFIRMADO);
            slot.setAmenity(null);
            aPersistir.add(slot);
        }
        slotRepository.saveAll(aPersistir);
        return aPersistir.size();
    }

    private List<LocalDate> calcularFechasSemanales(ActividadHorario horario, LocalDate desde, LocalDate hasta) {
        List<LocalDate> fechas = new ArrayList<>();
        LocalDate ocurrencia = DiasSemanaUtil.primeraOcurrenciaDesde(horario.getDiaSemana().toDayOfWeek(), desde);
        while (!ocurrencia.isAfter(hasta)) {
            fechas.add(ocurrencia);
            ocurrencia = ocurrencia.plusWeeks(1);
        }
        return fechas;
    }

    private void assertHorarioValido(ActividadHorario horario) {
        if (!horario.getHoraFin().isAfter(horario.getHoraInicio())) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "La hora de fin debe ser posterior a la de inicio",
                    "SlotGeneracionService | horarioId: " + horario.getId());
        }
    }

    private void validateActividad(Actividad actividad) {
        if (actividad == null || actividad.getId() == null) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Actividad inválida",
                    "SlotGeneracionService | actividad null o sin id");
        }
        if (actividad.getStatus() != null && actividad.getStatus() != 0) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "La actividad no está activa",
                    "SlotGeneracionService | status: " + actividad.getStatus());
        }
    }
}
