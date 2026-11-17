package com.blue.open.Code.Service.Actividades;

import com.blue.open.Code.Auth.RoleEnum;
import com.blue.open.Code.Entity.DTO.ActividadDTO;
import com.blue.open.Code.Entity.DTO.ActividadHorarioDTO;
import com.blue.open.Code.Entity.DTO.ActividadPaginateDTO;
import com.blue.open.Code.Entity.DTO.ActividadPaginateRequestDTO;
import com.blue.open.Code.Entity.Db.Actividad;
import com.blue.open.Code.Entity.Db.ActividadProfesor;
import com.blue.open.Code.Entity.Db.InscripcionActividad;
import com.blue.open.Code.Entity.Db.Member;
import com.blue.open.Code.Entity.Db.Profesor;
import com.blue.open.Code.Mapper.ActividadMapper;
import com.blue.open.Code.Repository.ActividadProfesorRepository;
import com.blue.open.Code.Repository.ActividadRepository;
import com.blue.open.Code.Repository.ActividadSpecifications;
import com.blue.open.Code.Repository.InscripcionActividadRepository;
import com.blue.open.Code.Repository.ProfesorRepository;
import com.blue.open.Code.Service.ProfesorAuthService;
import com.blue.open.Core.Exceptions.CustomException;
import com.blue.open.Core.Service.Auth.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ActividadQueryService {

    private static final int MAX_PAGE_SIZE = 100;

    @Autowired
    private ActividadRepository actividadRepository;

    @Autowired
    private ActividadProfesorRepository actividadProfesorRepository;

    @Autowired
    private ProfesorRepository profesorRepository;

    @Autowired
    private InscripcionActividadRepository inscripcionActividadRepository;

    @Autowired
    private ActividadMapper actividadMapper;

    @Autowired
    private AuthService authService;

    @Autowired
    private ProfesorAuthService profesorAuthService;

    @Autowired
    private ActividadHorariosService actividadHorariosService;

    @Transactional(readOnly = true)
    public Page<ActividadPaginateDTO> paginar(ActividadPaginateRequestDTO request, Long companyId) {
        ActividadPaginateRequestDTO req = request != null ? request : new ActividadPaginateRequestDTO();
        applyProfesorFilterIfNeeded(req, companyId);
        int page = Math.max(req.getPage(), 1);
        int size = req.getSize() <= 0 ? 10 : Math.min(req.getSize(), MAX_PAGE_SIZE);

        Specification<Actividad> spec = Specification.where(ActividadSpecifications.forCompany(companyId))
                .and(ActividadSpecifications.withPaginateRequest(req));

        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Order.asc("nombre"), Sort.Order.asc("id")));
        Page<Actividad> pageResult = actividadRepository.findAll(spec, pageable);

        List<Long> actividadIds = pageResult.getContent().stream().map(Actividad::getId).toList();
        Map<Long, Long> cantidadInscriptosByActividad = loadCantidadInscriptosByActividadIds(actividadIds);
        Map<Long, List<ActividadHorarioDTO>> horariosByActividad = actividadHorariosService.obtenerPorActividad(actividadIds);

        return pageResult.map(actividad -> actividadMapper.modelToPaginateDto(
                actividad,
                cantidadInscriptosByActividad.getOrDefault(actividad.getId(), 0L),
                horariosByActividad.getOrDefault(actividad.getId(), List.of())));
    }

    @Transactional(readOnly = true)
    public ActividadDTO findByUuid(String actividadUuid, Long companyId) {
        Actividad actividad = actividadRepository
                .findByUuidAndCompanyIdAndStatusActiveWithCategoria(actividadUuid, companyId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Actividad no encontrada",
                        "ActividadQueryService | uuid: " + actividadUuid));

        List<Profesor> profesores = loadProfesoresByActividadIds(List.of(actividad.getId()))
                .getOrDefault(actividad.getId(), List.of());

        List<InscripcionActividad> inscripciones = inscripcionActividadRepository
                .findActivasByActividadIdAndCompanyIdWithMember(actividad.getId(), companyId);

        return actividadMapper.modelToDto(actividad, profesores, inscripciones);
    }

    private Map<Long, Long> loadCantidadInscriptosByActividadIds(List<Long> actividadIds) {
        if (actividadIds.isEmpty()) {
            return Collections.emptyMap();
        }

        return inscripcionActividadRepository.countActivasByActividadIdIn(actividadIds).stream()
                .collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));
    }

    private Map<Long, List<Profesor>> loadProfesoresByActividadIds(List<Long> actividadIds) {
        if (actividadIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<ActividadProfesor> links = actividadProfesorRepository.findActivasByActividadIdIn(actividadIds);
        if (links.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Long> profesorIds = links.stream()
                .map(ActividadProfesor::getProfesorId)
                .distinct()
                .toList();

        Map<Long, Profesor> profesorById = profesorRepository.findByIdInWithUser(profesorIds).stream()
                .collect(Collectors.toMap(Profesor::getId, p -> p));

        Map<Long, List<Profesor>> result = new java.util.HashMap<>();
        for (ActividadProfesor link : links) {
            Profesor profesor = profesorById.get(link.getProfesorId());
            if (profesor == null) {
                continue;
            }
            result.computeIfAbsent(link.getActividadId(), k -> new ArrayList<>()).add(profesor);
        }
        return result;
    }

    private void applyProfesorFilterIfNeeded(ActividadPaginateRequestDTO req, Long companyId) {
        boolean isProfesor = authService.hasAnyRoleOrPermission(authService.getActualOrThrow(), List.of(RoleEnum.PROFESOR));
        if (!isProfesor) {
            return;
        }

        try {
            if (req.getProfesorUuid() != null && !req.getProfesorUuid().isBlank()) {
                return;
            }
            
            Profesor profesor = profesorAuthService.getCurrentProfesorOrThrow(companyId);
            req.setProfesorUuid(profesor.getUuid());
        } catch (Exception e) {
            // No es profesor o no existe en esta company; continúa sin filtro
        }
    }
}
