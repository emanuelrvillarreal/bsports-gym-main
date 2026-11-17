package com.blue.open.Code.Service.Actividades;

import com.blue.open.Code.Auth.PermissionEnum;
import com.blue.open.Code.Entity.DTO.CalendarioSlotDTO;
import com.blue.open.Code.Entity.DTO.CalendarioSlotTipoEnum;
import com.blue.open.Code.Entity.Db.Actividad;
import com.blue.open.Code.Entity.Db.Amenity;
import com.blue.open.Code.Entity.Db.EventoActividad;
import com.blue.open.Code.Entity.Db.Member;
import com.blue.open.Code.Entity.Db.Profesor;
import com.blue.open.Code.Entity.Db.Reserva;
import com.blue.open.Code.Entity.Db.Slot;
import com.blue.open.Code.Entity.Db.User;
import com.blue.open.Code.Entity.Enums.AsistenciaActividadEstadoEnum;
import com.blue.open.Code.Entity.Enums.ReservaEstadoEnum;
import com.blue.open.Code.Mapper.EventoActividadMapper;
import com.blue.open.Code.Repository.ActividadProfesorRepository;
import com.blue.open.Code.Repository.AsistenciaActividadRepository;
import com.blue.open.Code.Repository.EventoActividadRepository;
import com.blue.open.Code.Repository.InscripcionActividadRepository;
import com.blue.open.Code.Repository.ReservaRepository;
import com.blue.open.Code.Repository.SlotRepository;
import com.blue.open.Code.Service.MemberAuthService;
import com.blue.open.Code.Service.ProfesorAuthService;
import com.blue.open.Core.Exceptions.CustomException;
import com.blue.open.Core.Service.Auth.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CalendarioFiltradoQueryService {

    private static final List<ReservaEstadoEnum> ESTADOS_RESERVA_VISIBLES = List.of(
            ReservaEstadoEnum.PENDIENTE_SENA,
            ReservaEstadoEnum.CONFIRMADA);

    private static final int STATUS_INSCRIPCION_ACTIVA = 0;
    private static final int STATUS_INSCRIPCION_BAJA = -1;

    @Autowired
    private AuthService authService;

    @Autowired
    private AsistenciaActividadRepository asistenciaActividadRepository;

    @Autowired
    private MemberAuthService memberAuthService;

    @Autowired
    private ProfesorAuthService profesorAuthService;

    @Autowired
    private InscripcionActividadRepository inscripcionActividadRepository;

    @Autowired
    private ActividadProfesorRepository actividadProfesorRepository;

    @Autowired
    private SlotRepository slotRepository;

    @Autowired
    private EventoActividadRepository eventoActividadRepository;

    @Autowired
    private ReservaRepository reservaRepository;

    @Autowired
    private EventoActividadMapper eventoActividadMapper;

    @Transactional(readOnly = true)
    public List<CalendarioSlotDTO> listarSocio(LocalDateTime desde, LocalDateTime hasta) {
        authService.valid(List.of(PermissionEnum.PORTAL_ACTIVITIES_VIEW));
        Long companyId = authService.getCurrentCompanyOrThrow().getId();
        validarRango(desde, hasta);

        Member member = memberAuthService.getCurrentMemberOrThrow(companyId);
        List<Long> actividadIds = inscripcionActividadRepository.findActividadIdsByMemberIdAndCompanyId(
                member.getId(), companyId);

        List<CalendarioSlotDTO> resultado = new ArrayList<>();
        if (actividadIds != null && !actividadIds.isEmpty()) {
            // El socio solo ve las clases de los horarios que eligio en cada actividad.
            Set<String> horariosDelSocio = new HashSet<>(
                    inscripcionActividadRepository.findHorarioUuidsActivosByMemberIdAndCompanyId(member.getId(), companyId));
            listarPorActividadIds(companyId, actividadIds, desde, hasta).stream()
                    .filter(item -> item.getHorarioUuid() == null || horariosDelSocio.contains(item.getHorarioUuid()))
                    .forEach(resultado::add);
        }
        resultado.addAll(listarAmenitiesSocio(companyId, resolveTitularMemberId(member), desde, hasta));

        resultado.sort(Comparator.comparing(CalendarioSlotDTO::getInicio));
        return resultado;
    }

    @Transactional(readOnly = true)
    public List<CalendarioSlotDTO> listarProfesor(LocalDateTime desde, LocalDateTime hasta) {
        authService.valid(List.of(PermissionEnum.ACTIVITY_VIEW));
        Long companyId = authService.getCurrentCompanyOrThrow().getId();
        validarRango(desde, hasta);

        Profesor profesor = profesorAuthService.requirePortalEligible(
                profesorAuthService.getCurrentProfesorOrThrow(companyId));
        List<Long> actividadIds = actividadProfesorRepository.findActividadIdsByProfesorId(profesor.getId());

        List<CalendarioSlotDTO> calendario = listarPorActividadIds(companyId, actividadIds, desde, hasta);
        completarPorcentajeAsistencia(companyId, calendario);
        return calendario;
    }

    private void completarPorcentajeAsistencia(Long companyId, List<CalendarioSlotDTO> calendario) {
        List<CalendarioSlotDTO> slotsActividad = calendario.stream()
                .filter(item -> item.getTipo() == CalendarioSlotTipoEnum.ACTIVIDAD)
                .toList();
        if (slotsActividad.isEmpty()) {
            return;
        }

        Set<String> slotUuids = slotsActividad.stream()
                .map(CalendarioSlotDTO::getSlotUuid)
                .collect(Collectors.toSet());
        Set<String> actividadUuids = slotsActividad.stream()
                .map(CalendarioSlotDTO::getReferenciaUuid)
                .collect(Collectors.toSet());

        Map<String, Long> presentesPorSlot = contarPresentesPorSlot(slotUuids);
        Map<String, List<InscripcionAlumno>> alumnosPorActividad = agruparAlumnosPorActividad(companyId, actividadUuids);

        for (CalendarioSlotDTO slot : slotsActividad) {
            List<InscripcionAlumno> inscripciones = alumnosPorActividad.getOrDefault(slot.getReferenciaUuid(), List.of());
            long alumnos = inscripciones.stream()
                    .filter(inscripcion -> slot.getHorarioUuid() == null
                            || slot.getHorarioUuid().equals(inscripcion.horarioUuid()))
                    .filter(inscripcion -> estabaInscripto(inscripcion, slot.getInicio()))
                    .map(InscripcionAlumno::memberId)
                    .distinct()
                    .count();
            if (alumnos == 0) {
                continue;
            }

            long presentes = presentesPorSlot.getOrDefault(slot.getSlotUuid(), 0L);
            slot.setPorcentajeAsistencia(calcularPorcentaje(presentes, alumnos));
        }
    }

    private Map<String, Long> contarPresentesPorSlot(Set<String> slotUuids) {
        List<Object[]> filas = asistenciaActividadRepository.countBySlotUuidInAndEstado(
                slotUuids, AsistenciaActividadEstadoEnum.PRESENTE);
        Map<String, Long> presentesPorSlot = new HashMap<>();
        for (Object[] fila : filas) {
            presentesPorSlot.put((String) fila[0], ((Number) fila[1]).longValue());
        }
        return presentesPorSlot;
    }

    private Map<String, List<InscripcionAlumno>> agruparAlumnosPorActividad(Long companyId, Set<String> actividadUuids) {
        List<Object[]> filas = inscripcionActividadRepository.findAlumnosByActividadUuidIn(
                companyId, actividadUuids, List.of(STATUS_INSCRIPCION_ACTIVA, STATUS_INSCRIPCION_BAJA));
        Map<String, List<InscripcionAlumno>> alumnosPorActividad = new HashMap<>();
        for (Object[] fila : filas) {
            InscripcionAlumno inscripcion = new InscripcionAlumno(
                    (Long) fila[1],
                    (Integer) fila[2],
                    ((Timestamp) fila[3]).toLocalDateTime(),
                    (String) fila[4]);
            alumnosPorActividad.computeIfAbsent((String) fila[0], actividadUuid -> new ArrayList<>()).add(inscripcion);
        }
        return alumnosPorActividad;
    }

    /** Una inscripción dada de baja cuenta solo si la baja fue posterior al inicio del slot. */
    private boolean estabaInscripto(InscripcionAlumno inscripcion, LocalDateTime inicioSlot) {
        if (inscripcion.status() == STATUS_INSCRIPCION_ACTIVA) {
            return true;
        }
        return inscripcion.fechaActualizacion().isAfter(inicioSlot);
    }

    private double calcularPorcentaje(long presentes, long alumnos) {
        double porcentaje = Math.round(presentes * 1000.0 / alumnos) / 10.0;
        return Math.min(porcentaje, 100.0);
    }

    private record InscripcionAlumno(
            Long memberId,
            Integer status,
            LocalDateTime fechaActualizacion,
            String horarioUuid) {
    }

    private List<CalendarioSlotDTO> listarPorActividadIds(
            Long companyId,
            List<Long> actividadIds,
            LocalDateTime desde,
            LocalDateTime hasta) {
        if (actividadIds == null || actividadIds.isEmpty()) {
            return List.of();
        }

        List<Slot> slots = slotRepository.findByActividadIdsAndInicioBetweenWithDetails(
                companyId, actividadIds, desde, hasta);
        List<EventoActividad> eventos = eventoActividadRepository.findByActividadIdsAndInicioBetweenWithDetails(
                companyId, actividadIds, desde, hasta);

        List<CalendarioSlotDTO> resultado = new ArrayList<>();
        slots.forEach(slot -> resultado.add(toDtoActividad(slot)));
        eventos.forEach(evento -> resultado.add(eventoActividadMapper.toCalendarioDto(evento)));

        resultado.sort(Comparator.comparing(CalendarioSlotDTO::getInicio));
        return resultado;
    }

    private CalendarioSlotDTO toDtoActividad(Slot slot) {
        CalendarioSlotDTO dto = new CalendarioSlotDTO();
        dto.setSlotUuid(slot.getUuid());
        dto.setInicio(slot.getInicio());
        dto.setFin(slot.getFin());
        dto.setEstado(slot.getEstado());

        Actividad actividad = slot.getActividad();
        if (actividad != null) {
            dto.setTipo(CalendarioSlotTipoEnum.ACTIVIDAD);
            dto.setReferenciaUuid(actividad.getUuid());
            dto.setReferenciaNombre(actividad.getNombre());
        }
        if (slot.getHorario() != null) {
            dto.setHorarioUuid(slot.getHorario().getUuid());
        }
        return dto;
    }

    private List<CalendarioSlotDTO> listarAmenitiesSocio(
            Long companyId,
            Long titularMemberId,
            LocalDateTime desde,
            LocalDateTime hasta) {
        List<Reserva> reservas = reservaRepository.findActivasByCompanyIdAndMemberIdAndSlotInicioBetween(
                companyId, titularMemberId, ESTADOS_RESERVA_VISIBLES, desde, hasta);
        List<CalendarioSlotDTO> resultado = new ArrayList<>();
        reservas.forEach(reserva -> resultado.add(toDtoAmenity(reserva)));
        return resultado;
    }

    private CalendarioSlotDTO toDtoAmenity(Reserva reserva) {
        Slot slot = reserva.getSlot();
        CalendarioSlotDTO dto = new CalendarioSlotDTO();
        dto.setSlotUuid(slot.getUuid());
        dto.setInicio(slot.getInicio());
        dto.setFin(slot.getFin());
        dto.setEstado(slot.getEstado());

        Amenity amenity = reserva.getAmenity();
        if (amenity != null) {
            dto.setTipo(CalendarioSlotTipoEnum.AMENITY);
            dto.setReferenciaUuid(amenity.getUuid());
            dto.setReferenciaNombre(amenity.getNombre());
        }

        Member titular = reserva.getMember();
        if (titular != null && titular.getUser() != null) {
            User user = titular.getUser();
            dto.setTitularNombre(user.getNombre());
            dto.setTitularApellido(user.getApellido());
        }
        return dto;
    }

    private Long resolveTitularMemberId(Member member) {
        if (member.isTitular()) {
            return member.getId();
        }
        Member titular = member.getTitular();
        return titular != null ? titular.getId() : member.getId();
    }

    private void validarRango(LocalDateTime desde, LocalDateTime hasta) {
        if (desde == null || hasta == null || !hasta.isAfter(desde)) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Rango de fechas inválido",
                    "CalendarioFiltradoQueryService | desde/hasta inválidos");
        }
    }
}
