package com.blue.open.Code.Service.Actividades;

import com.blue.open.Code.Auth.PermissionEnum;
import com.blue.open.Code.Entity.DTO.ActividadAltaCommand;
import com.blue.open.Code.Entity.DTO.ActividadDTO;
import com.blue.open.Code.Entity.Db.Actividad;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.Profesor;
import com.blue.open.Code.Entity.Enums.ProfesorEstadoEnum;
import com.blue.open.Code.Mapper.ActividadMapper;
import com.blue.open.Code.Repository.ActividadRepository;
import com.blue.open.Code.Repository.ProfesorRepository;
import com.blue.open.Core.Exceptions.CustomException;
import com.blue.open.Core.Service.Auth.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class ActividadAltaService {

    @Autowired
    private AuthService authService;

    @Autowired
    private ActividadRepository actividadRepository;

    @Autowired
    private ProfesorRepository profesorRepository;

    @Autowired
    private ActividadProfesorAsignacionService actividadProfesorAsignacionService;

    @Autowired
    private SlotGeneracionService slotGeneracionService;

    @Autowired
    private ActividadHorariosService actividadHorariosService;

    @Autowired
    private ActividadMapper actividadMapper;

    @Autowired
    private ActividadCategoriaResolver actividadCategoriaResolver;

    @Transactional
    public ActividadDTO alta(ActividadAltaCommand command) {
        authService.valid(List.of(PermissionEnum.ACTIVITY_CREATE));
        Company company = authService.getCurrentCompanyOrThrow();

        Actividad actividad = new Actividad();
        actividad.setCompany(company);
        actividad.setNombre(command.getNombre().trim());
        actividad.setUbicacion(command.getUbicacion().trim());
        actividadHorariosService.prepararResumen(actividad, command.getHorarios());
        actividad.setImporteCuota(command.getImporteCuota());
        if (command.getPeriodicidadCuota() != null) {
            actividad.setPeriodicidadCuota(command.getPeriodicidadCuota());
        }
        if (command.getDiasVencimientoCuota() != null) {
            actividad.setDiasVencimientoCuota(command.getDiasVencimientoCuota());
        }
        actividad.setDiaVencimientoFijo(command.getDiaVencimientoFijo());
        // El corte solo tiene sentido con dia fijo (cuotas alineadas al mes).
        actividad.setDiaCorteInscripcion(command.getDiaVencimientoFijo() != null ? command.getDiaCorteInscripcion() : null);
        ActividadHorariosService.validarDiaVencimientoFijo(actividad);
        actividad.getRecargo().configurar(command.getRecargoHabilitado(), command.getRecargoTipo(),
                command.getRecargoValor(), command.getRecargoDiasGracia(), java.time.LocalDate.now());
        actividad.setRequiereCertificado(Boolean.TRUE.equals(command.getRequiereCertificado()));
        actividad.setCompleta(false);
        actividad.setCategorias(actividadCategoriaResolver.resolve(company.getId(), command.getCategoriaUuids()));

        actividad = actividadRepository.save(actividad);
        actividadHorariosService.sincronizar(actividad, command.getHorarios());

        List<Profesor> profesoresAsignados = asignarProfesoresOpcionales(actividad, command.getProfesorUuids());

        slotGeneracionService.generarVentanaCompleta(actividad, LocalDate.now());

        return actividadMapper.modelToDto(actividad, profesoresAsignados);
    }

    private List<Profesor> asignarProfesoresOpcionales(Actividad actividad, List<String> profesorUuids) {
        if (profesorUuids == null || profesorUuids.isEmpty()) {
            return List.of();
        }

        Set<String> uuidsUnicos = new LinkedHashSet<>(profesorUuids);
        List<Profesor> asignados = new ArrayList<>();

        for (String profesorUuid : uuidsUnicos) {
            Profesor profesor = profesorRepository.findByUuidAndCompanyIdWithUser(profesorUuid, actividad.getCompany().getId())
                    .orElseThrow(() -> new CustomException(HttpStatus.BAD_REQUEST, "Profesor no encontrado",
                            "ActividadAltaService | profesorUuid: " + profesorUuid));

            if (profesor.getEstado() != ProfesorEstadoEnum.ACTIVO) {
                throw new CustomException(HttpStatus.BAD_REQUEST, "El profesor no está activo",
                        "ActividadAltaService | profesorUuid: " + profesorUuid);
            }

            actividadProfesorAsignacionService.asignar(actividad, profesor);
            asignados.add(profesor);
        }

        return asignados;
    }
}
