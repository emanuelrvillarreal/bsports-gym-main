package com.blue.open.Code.Service.Actividades;

import com.blue.open.Code.Auth.PermissionEnum;
import com.blue.open.Code.Auth.RoleEnum;
import com.blue.open.Code.Entity.DTO.EventoActividadDetalleDTO;
import com.blue.open.Code.Entity.DTO.EventoActividadPaginateRequestDTO;
import com.blue.open.Code.Entity.Db.EventoActividad;
import com.blue.open.Code.Entity.Db.Profesor;
import com.blue.open.Code.Mapper.EventoActividadMapper;
import com.blue.open.Code.Repository.EventoActividadRepository;
import com.blue.open.Code.Repository.EventoActividadSpecifications;
import com.blue.open.Code.Service.ProfesorAuthService;
import com.blue.open.Core.Service.Auth.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class EventoActividadQueryService {

    private static final int MAX_PAGE_SIZE = 100;

    @Autowired
    private AuthService authService;

    @Autowired
    private EventoActividadRepository eventoActividadRepository;

    @Autowired
    private EventoActividadMapper eventoActividadMapper;

    @Autowired
    private ProfesorAuthService profesorAuthService;

    @Transactional(readOnly = true)
    public Page<EventoActividadDetalleDTO> paginar(EventoActividadPaginateRequestDTO request) {
        authService.valid(List.of(
                PermissionEnum.ACTIVITY_VIEW,
                PermissionEnum.PORTAL_ACTIVITIES_VIEW));
        Long companyId = authService.getCurrentCompanyOrThrow().getId();

        EventoActividadPaginateRequestDTO req = request != null ? request : new EventoActividadPaginateRequestDTO();
        int page = Math.max(req.getPage(), 1);
        int size = req.getSize() <= 0 ? 10 : Math.min(req.getSize(), MAX_PAGE_SIZE);

        Specification<EventoActividad> spec = Specification.where(EventoActividadSpecifications.forCompany(companyId))
                .and(EventoActividadSpecifications.withPaginateRequest(req));
        spec = aplicarFiltroProfesor(spec, companyId);

        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Order.asc("inicio"), Sort.Order.asc("id")));
        Page<EventoActividad> pageResult = eventoActividadRepository.findAll(spec, pageable);

        return pageResult.map(eventoActividadMapper::toDetalleDto);
    }

    private Specification<EventoActividad> aplicarFiltroProfesor(Specification<EventoActividad> spec, Long companyId) {
        if (!authService.hasAnyRoleOrPermission(authService.getActualOrThrow(), List.of(RoleEnum.PROFESOR))) {
            return spec;
        }
        Profesor profesor = profesorAuthService.getCurrentProfesorOrThrow(companyId);
        return spec.and(EventoActividadSpecifications.forProfesor(profesor.getId()));
    }
}
