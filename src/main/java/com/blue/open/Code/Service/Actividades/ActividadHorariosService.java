package com.blue.open.Code.Service.Actividades;

import com.blue.open.Code.Entity.DTO.ActividadHorarioCommand;
import com.blue.open.Code.Entity.DTO.ActividadHorarioDTO;
import com.blue.open.Code.Entity.Db.Actividad;
import com.blue.open.Code.Entity.Db.ActividadHorario;
import com.blue.open.Code.Entity.Db.InscripcionActividad;
import com.blue.open.Code.Entity.Enums.DiaSemanaEnum;
import com.blue.open.Code.Entity.Enums.PeriodicidadCuotaEnum;
import com.blue.open.Code.Repository.ActividadHorarioRepository;
import com.blue.open.Code.Repository.ActividadRepository;
import com.blue.open.Code.Util.DiasSemanaUtil;
import com.blue.open.Core.Exceptions.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Reglas de los horarios de una actividad: alta, edicion y baja de horarios, cupo por horario y
 * eleccion de horarios en una inscripcion.
 *
 * Mantiene en Actividad los campos resumen que usa el resto del sistema (dias_semana, primera
 * hora de inicio, ultima hora de fin, cupo total y "completa") para no tener que cambiar cada
 * listado: se recalculan cada vez que cambian los horarios o las inscripciones.
 */
@Service
public class ActividadHorariosService {

    private static final int STATUS_ACTIVO = 0;
    private static final int STATUS_BAJA = 1;
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    @Autowired
    private ActividadHorarioRepository actividadHorarioRepository;

    @Autowired
    private ActividadRepository actividadRepository;

    /**
     * Reemplaza los horarios de la actividad por los recibidos.
     *
     * Espera: actividad ya persistida y la lista completa de horarios deseados (al menos uno).
     * Los que traen uuid se actualizan, los que no se crean y los existentes que faltan se dan
     * de baja.
     * Responde: los horarios activos resultantes.
     * Lanza 400 si la lista esta vacia, si hay dos horarios iguales (mismo dia y hora de inicio),
     * si un uuid no pertenece a la actividad, si se baja el cupo por debajo de los inscriptos o si
     * se quita un horario que todavia tiene inscriptos.
     */
    @Transactional
    public List<ActividadHorario> sincronizar(Actividad actividad, List<ActividadHorarioCommand> commands) {
        if (commands == null || commands.isEmpty()) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "La actividad debe tener al menos un horario",
                    "ActividadHorariosService | sincronizar | actividadId: " + actividad.getId());
        }
        validarSinRepetidos(commands);

        Map<String, ActividadHorario> actuales = new HashMap<>();
        for (ActividadHorario horario : actividadHorarioRepository.findActivosByActividadId(actividad.getId())) {
            actuales.put(horario.getUuid(), horario);
        }
        Map<Long, Long> inscriptos = contarInscriptos(actuales.values());

        List<ActividadHorario> resultado = new ArrayList<>();
        Set<String> conservados = new HashSet<>();
        for (ActividadHorarioCommand command : commands) {
            ActividadHorario horario;
            if (command.getUuid() == null || command.getUuid().isBlank()) {
                horario = new ActividadHorario();
                horario.setCompany(actividad.getCompany());
                horario.setActividad(actividad);
            } else {
                horario = actuales.get(command.getUuid());
                if (horario == null) {
                    throw new CustomException(HttpStatus.BAD_REQUEST, "El horario no pertenece a la actividad",
                            "ActividadHorariosService | sincronizar | horarioUuid: " + command.getUuid());
                }
                long ocupados = inscriptos.getOrDefault(horario.getId(), 0L);
                if (command.getCupoMaximo() < ocupados) {
                    throw new CustomException(HttpStatus.BAD_REQUEST,
                            "El cupo de " + describir(horario) + " no puede ser menor a "
                                    + (ocupados == 1 ? "su inscripto" : "sus " + ocupados + " inscriptos"),
                            "ActividadHorariosService | sincronizar | horarioId: " + horario.getId());
                }
                conservados.add(horario.getUuid());
            }
            horario.setDiaSemana(command.getDiaSemana());
            horario.setHoraInicio(command.getHoraInicio());
            horario.setHoraFin(command.getHoraFin());
            horario.setCupoMaximo(command.getCupoMaximo());
            resultado.add(horario);
        }

        for (ActividadHorario horario : actuales.values()) {
            if (conservados.contains(horario.getUuid())) {
                continue;
            }
            long ocupados = inscriptos.getOrDefault(horario.getId(), 0L);
            if (ocupados > 0) {
                throw new CustomException(HttpStatus.BAD_REQUEST,
                        "No se puede quitar " + describir(horario) + " porque tiene "
                                + (ocupados == 1 ? "1 inscripto" : ocupados + " inscriptos")
                                + ". Pasalos a otro horario primero.",
                        "ActividadHorariosService | sincronizar | horarioId: " + horario.getId());
            }
            horario.setStatus(STATUS_BAJA);
            actividadHorarioRepository.save(horario);
        }

        actividadHorarioRepository.saveAll(resultado);
        actualizarResumen(actividad, resultado);
        return resultado;
    }

    /**
     * Resuelve los horarios elegidos para una inscripcion y valida que tengan lugar.
     *
     * Espera: actividad bloqueada para escritura (el llamador la obtiene FOR UPDATE), uuids de
     * horarios activos de esa actividad y, si se edita una inscripcion, la inscripcion actual (sus
     * horarios ya ocupados no cuentan como lugar nuevo).
     * Responde: los horarios elegidos, sin repetidos.
     * Lanza 400 si no se elige ninguno, si alguno no es de la actividad o si alguno esta lleno.
     */
    @Transactional(readOnly = true)
    public Set<ActividadHorario> resolverEleccion(
            Actividad actividad,
            Collection<String> horarioUuids,
            InscripcionActividad inscripcionActual) {
        if (horarioUuids == null || horarioUuids.isEmpty()) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Elegí al menos un horario",
                    "ActividadHorariosService | resolverEleccion | actividadId: " + actividad.getId());
        }
        Map<String, ActividadHorario> disponibles = actividadHorarioRepository
                .findActivosByActividadId(actividad.getId()).stream()
                .collect(Collectors.toMap(ActividadHorario::getUuid, h -> h));
        Set<Long> yaOcupados = inscripcionActual == null ? Set.of()
                : inscripcionActual.getHorarios().stream().map(ActividadHorario::getId).collect(Collectors.toSet());

        Set<ActividadHorario> elegidos = new LinkedHashSet<>();
        for (String uuid : new LinkedHashSet<>(horarioUuids)) {
            ActividadHorario horario = disponibles.get(uuid);
            if (horario == null) {
                throw new CustomException(HttpStatus.BAD_REQUEST, "El horario elegido no pertenece a la actividad",
                        "ActividadHorariosService | resolverEleccion | horarioUuid: " + uuid);
            }
            elegidos.add(horario);
        }

        Map<Long, Long> inscriptos = contarInscriptos(elegidos);
        for (ActividadHorario horario : elegidos) {
            if (yaOcupados.contains(horario.getId())) {
                continue;
            }
            if (inscriptos.getOrDefault(horario.getId(), 0L) >= horario.getCupoMaximo()) {
                throw new CustomException(HttpStatus.BAD_REQUEST,
                        "No quedan lugares en " + describir(horario),
                        "ActividadHorariosService | resolverEleccion | horarioId: " + horario.getId());
            }
        }
        return elegidos;
    }

    /**
     * Marca la actividad como completa cuando todos sus horarios estan llenos. Se llama despues de
     * inscribir, cambiar horarios o dar de baja.
     */
    @Transactional
    public void sincronizarCompleta(Actividad actividad) {
        List<ActividadHorario> horarios = actividadHorarioRepository.findActivosByActividadId(actividad.getId());
        Map<Long, Long> inscriptos = contarInscriptos(horarios);
        boolean completa = !horarios.isEmpty() && horarios.stream()
                .allMatch(h -> inscriptos.getOrDefault(h.getId(), 0L) >= h.getCupoMaximo());
        if (actividad.isCompleta() != completa) {
            actividad.setCompleta(completa);
            actividadRepository.save(actividad);
        }
    }

    /**
     * Horarios activos con cupo, agrupados por actividad. Para armar los DTO de listados y detalle.
     * Responde: mapa actividadId -> horarios ordenados por dia y hora (vacio si no hay ids).
     */
    @Transactional(readOnly = true)
    public Map<Long, List<ActividadHorarioDTO>> obtenerPorActividad(Collection<Long> actividadIds) {
        if (actividadIds == null || actividadIds.isEmpty()) {
            return Map.of();
        }
        List<ActividadHorario> horarios = actividadHorarioRepository.findActivosByActividadIdIn(actividadIds);
        Map<Long, Long> inscriptos = contarInscriptos(horarios);
        Map<Long, List<ActividadHorarioDTO>> resultado = new LinkedHashMap<>();
        for (ActividadHorario horario : horarios) {
            resultado.computeIfAbsent(horario.getActividad().getId(), id -> new ArrayList<>())
                    .add(toDto(horario, inscriptos.getOrDefault(horario.getId(), 0L)));
        }
        return resultado;
    }

    public ActividadHorarioDTO toDto(ActividadHorario horario, long inscriptos) {
        ActividadHorarioDTO dto = new ActividadHorarioDTO();
        dto.setUuid(horario.getUuid());
        dto.setDiaSemana(horario.getDiaSemana());
        dto.setHoraInicio(horario.getHoraInicio());
        dto.setHoraFin(horario.getHoraFin());
        dto.setCupoMaximo(horario.getCupoMaximo());
        dto.setInscriptos(inscriptos);
        dto.setDisponibles(Math.max(0, horario.getCupoMaximo() - inscriptos));
        return dto;
    }

    private Map<Long, Long> contarInscriptos(Collection<ActividadHorario> horarios) {
        List<Long> ids = horarios.stream().map(ActividadHorario::getId).filter(id -> id != null).toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        Map<Long, Long> conteo = new HashMap<>();
        for (Object[] fila : actividadHorarioRepository.countInscripcionesActivasByHorarioIdIn(ids)) {
            conteo.put((Long) fila[0], ((Number) fila[1]).longValue());
        }
        return conteo;
    }

    private void validarSinRepetidos(List<ActividadHorarioCommand> commands) {
        Set<String> claves = new HashSet<>();
        for (ActividadHorarioCommand command : commands) {
            String clave = command.getDiaSemana() + "|" + command.getHoraInicio();
            if (!claves.add(clave)) {
                throw new CustomException(HttpStatus.BAD_REQUEST,
                        "Hay dos horarios el " + nombreDia(command.getDiaSemana()) + " a las "
                                + command.getHoraInicio().format(HORA),
                        "ActividadHorariosService | validarSinRepetidos | clave: " + clave);
            }
        }
    }

    /**
     * Completa los campos resumen de una actividad nueva antes de su primer guardado (son NOT NULL),
     * a partir de los horarios pedidos. No persiste nada.
     */
    public void prepararResumen(Actividad actividad, List<ActividadHorarioCommand> commands) {
        if (commands == null || commands.isEmpty()) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "La actividad debe tener al menos un horario",
                    "ActividadHorariosService | prepararResumen");
        }
        List<ActividadHorario> provisorios = new ArrayList<>();
        for (ActividadHorarioCommand command : commands) {
            ActividadHorario horario = new ActividadHorario();
            horario.setDiaSemana(command.getDiaSemana());
            horario.setHoraInicio(command.getHoraInicio());
            horario.setHoraFin(command.getHoraFin());
            horario.setCupoMaximo(command.getCupoMaximo());
            provisorios.add(horario);
        }
        aplicarResumen(actividad, provisorios);
    }

    // * Los campos resumen de Actividad siguen alimentando listados, calendario y portal del profesor.
    private void actualizarResumen(Actividad actividad, List<ActividadHorario> horarios) {
        aplicarResumen(actividad, horarios);
        actividadRepository.save(actividad);
    }

    private void aplicarResumen(Actividad actividad, List<ActividadHorario> horarios) {
        List<DiaSemanaEnum> dias = horarios.stream()
                .map(ActividadHorario::getDiaSemana)
                .distinct()
                .sorted()
                .toList();
        actividad.setDiasSemana(DiasSemanaUtil.toStorage(dias));
        actividad.setHoraInicio(horarios.stream().map(ActividadHorario::getHoraInicio)
                .min(Comparator.naturalOrder()).orElse(LocalTime.MIDNIGHT));
        actividad.setHoraFin(horarios.stream().map(ActividadHorario::getHoraFin)
                .max(Comparator.naturalOrder()).orElse(LocalTime.MIDNIGHT));
        actividad.setCupoMaximo(horarios.stream().mapToInt(ActividadHorario::getCupoMaximo).sum());
    }

    // * El dia fijo se alinea al mes: no tiene sentido con cuotas semanales.
    static void validarDiaVencimientoFijo(Actividad actividad) {
        if (actividad.tieneDiaVencimientoFijo()
                && actividad.getPeriodicidadCuota() == PeriodicidadCuotaEnum.SEMANAL) {
            throw new CustomException(HttpStatus.BAD_REQUEST,
                    "El día fijo de vencimiento no se puede usar con cuotas semanales",
                    "ActividadHorariosService | validarDiaVencimientoFijo | actividadId: " + actividad.getId());
        }
    }

    private String describir(ActividadHorario horario) {
        return "el horario del " + nombreDia(horario.getDiaSemana()) + " de "
                + horario.getHoraInicio().format(HORA) + " a " + horario.getHoraFin().format(HORA);
    }

    private String nombreDia(DiaSemanaEnum dia) {
        return switch (dia) {
            case LUNES -> "lunes";
            case MARTES -> "martes";
            case MIERCOLES -> "miércoles";
            case JUEVES -> "jueves";
            case VIERNES -> "viernes";
            case SABADO -> "sábado";
            case DOMINGO -> "domingo";
        };
    }
}
