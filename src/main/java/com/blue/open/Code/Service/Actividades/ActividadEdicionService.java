package com.blue.open.Code.Service.Actividades;

import com.blue.open.Code.Auth.PermissionEnum;
import com.blue.open.Code.Entity.DTO.ActividadDTO;
import com.blue.open.Code.Entity.DTO.ActividadEdicionCommand;
import com.blue.open.Code.Entity.Db.Actividad;
import com.blue.open.Code.Entity.Db.ActividadProfesor;
import com.blue.open.Code.Entity.Db.Profesor;
import com.blue.open.Code.Entity.Db.Cuota;
import com.blue.open.Code.Entity.Enums.CuotaEstadoEnum;
import com.blue.open.Code.Entity.Enums.ProfesorEstadoEnum;
import com.blue.open.Code.Mapper.ActividadMapper;
import com.blue.open.Code.Repository.ActividadProfesorRepository;
import com.blue.open.Code.Repository.ActividadRepository;
import com.blue.open.Code.Repository.CuotaRepository;
import com.blue.open.Code.Repository.ProfesorRepository;
import com.blue.open.Core.Exceptions.CustomException;
import com.blue.open.Core.Service.Auth.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ActividadEdicionService {

    @Autowired
    private AuthService authService;

    @Autowired
    private ActividadRepository actividadRepository;

    @Autowired
    private ActividadProfesorRepository actividadProfesorRepository;

    @Autowired
    private ProfesorRepository profesorRepository;

    @Autowired
    private CuotaRepository cuotaRepository;

    @Autowired
    private ActividadProfesorAsignacionService actividadProfesorAsignacionService;

    @Autowired
    private ActividadCategoriaResolver actividadCategoriaResolver;

    @Autowired
    private ActividadMapper actividadMapper;

    @Autowired
    private ActividadHorariosService actividadHorariosService;

    @Autowired
    private SlotGeneracionService slotGeneracionService;

    @Transactional
    public ActividadDTO editar(String actividadUuid, ActividadEdicionCommand command) {
        authService.valid(List.of(PermissionEnum.ACTIVITY_EDIT));
        Long companyId = authService.getCurrentCompanyOrThrow().getId();

        Actividad actividad = actividadRepository.findByUuidAndCompanyIdAndStatusActiveWithCategoria(actividadUuid, companyId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Actividad no encontrada",
                        "ActividadEdicionService | uuid: " + actividadUuid));

        BigDecimal importeAnterior = actividad.getImporteCuota();
        actividad.setNombre(command.getNombre().trim());
        actividad.setUbicacion(command.getUbicacion().trim());
        actividad.setImporteCuota(command.getImporteCuota());
        // Las inscripciones ya tienen calculada su próxima generación: el cambio de periodicidad
        // aplica desde el ciclo siguiente, cuando el puntero vuelva a avanzar.
        if (command.getPeriodicidadCuota() != null) {
            actividad.setPeriodicidadCuota(command.getPeriodicidadCuota());
        }
        if (command.getDiasVencimientoCuota() != null) {
            actividad.setDiasVencimientoCuota(command.getDiasVencimientoCuota());
        }
        // Aplica a las proximas generaciones: cada inscripcion se alinea al 1 del mes al avanzar.
        actividad.setDiaVencimientoFijo(command.getDiaVencimientoFijo());
        // El corte solo tiene sentido con dia fijo (cuotas alineadas al mes).
        actividad.setDiaCorteInscripcion(command.getDiaVencimientoFijo() != null ? command.getDiaCorteInscripcion() : null);
        ActividadHorariosService.validarDiaVencimientoFijo(actividad);
        if (command.getRequiereCertificado() != null) {
            actividad.setRequiereCertificado(command.getRequiereCertificado());
        }
        actividad.getRecargo().configurar(command.getRecargoHabilitado(), command.getRecargoTipo(),
                command.getRecargoValor(), command.getRecargoDiasGracia(), java.time.LocalDate.now());
        if (command.getCategoriaUuids() != null) {
            actividad.getCategorias().clear();
            actividad.getCategorias().addAll(actividadCategoriaResolver.resolve(companyId, command.getCategoriaUuids()));
        }

        actividad = actividadRepository.save(actividad);

        // Los horarios definen las clases: al cambiarlos se rehacen las futuras sin asistencia.
        actividadHorariosService.sincronizar(actividad, command.getHorarios());
        actividadHorariosService.sincronizarCompleta(actividad);
        slotGeneracionService.regenerarTrasCambioHorarioODias(actividad);

        reasignarProfesores(actividad, companyId, command.getProfesorUuids());

        int actualizadas = 0;
        int omitidas = 0;
        boolean cambioImporte = importeAnterior == null || importeAnterior.compareTo(command.getImporteCuota()) != 0;
        boolean actualizarPendientes = cambioImporte && Boolean.TRUE.equals(command.getAplicarImporteACuotasPendientes());
        if (actualizarPendientes) {
            for (Cuota cuota : cuotaRepository.findImpagasDeActividad(companyId, actividad.getId(),
                    List.of(CuotaEstadoEnum.PENDIENTE, CuotaEstadoEnum.VENCIDA))) {
                if (cuota.actualizarImporteImpago(command.getImporteCuota())) {
                    cuotaRepository.save(cuota);
                    actualizadas++;
                } else {
                    omitidas++;
                }
            }
        }

        List<Profesor> profesores = loadProfesoresActivos(actividad.getId());
        ActividadDTO dto = actividadMapper.modelToDto(actividad, profesores);
        if (actualizarPendientes) {
            dto.setCuotasPendientesActualizadas(actualizadas);
            dto.setCuotasPendientesOmitidas(omitidas);
        }
        return dto;
    }

    private void reasignarProfesores(Actividad actividad, Long companyId, List<String> profesorUuids) {
        if (profesorUuids == null) {
            return;
        }
        Set<String> uuidsDeseados = new LinkedHashSet<>(profesorUuids);

        List<ActividadProfesor> asignacionesActuales =
                actividadProfesorRepository.findActivasByActividadIdIn(List.of(actividad.getId()));
        List<Long> idsActuales = asignacionesActuales.stream()
                .map(ActividadProfesor::getProfesorId)
                .distinct()
                .toList();
        Map<Long, Profesor> profesoresActuales = idsActuales.isEmpty() ? Map.of()
                : profesorRepository.findByIdInWithUser(idsActuales).stream()
                        .collect(Collectors.toMap(Profesor::getId, p -> p));

        for (Profesor actual : profesoresActuales.values()) {
            if (!uuidsDeseados.contains(actual.getUuid())) {
                actividadProfesorAsignacionService.desasignar(actividad, actual);
            }
        }

        Set<String> uuidsActuales = profesoresActuales.values().stream()
                .map(Profesor::getUuid)
                .collect(Collectors.toSet());
        for (String profesorUuid : uuidsDeseados) {
            if (uuidsActuales.contains(profesorUuid)) {
                continue;
            }
            Profesor profesor = profesorRepository.findByUuidAndCompanyIdWithUser(profesorUuid, companyId)
                    .orElseThrow(() -> new CustomException(HttpStatus.BAD_REQUEST, "Profesor no encontrado",
                            "ActividadEdicionService | profesorUuid: " + profesorUuid));
            if (profesor.getEstado() != ProfesorEstadoEnum.ACTIVO) {
                throw new CustomException(HttpStatus.BAD_REQUEST, "El profesor no está activo",
                        "ActividadEdicionService | profesorUuid: " + profesorUuid);
            }
            actividadProfesorAsignacionService.asignar(actividad, profesor);
        }
    }

    private List<Profesor> loadProfesoresActivos(Long actividadId) {
        List<ActividadProfesor> links = actividadProfesorRepository.findActivasByActividadIdIn(List.of(actividadId));
        if (links.isEmpty()) {
            return List.of();
        }

        List<Long> profesorIds = links.stream()
                .map(ActividadProfesor::getProfesorId)
                .distinct()
                .toList();

        Map<Long, Profesor> profesorById = profesorRepository.findByIdInWithUser(profesorIds).stream()
                .collect(Collectors.toMap(Profesor::getId, p -> p));

        List<Profesor> result = new ArrayList<>();
        for (ActividadProfesor link : links) {
            Profesor profesor = profesorById.get(link.getProfesorId());
            if (profesor != null) {
                result.add(profesor);
            }
        }
        return result;
    }
}
