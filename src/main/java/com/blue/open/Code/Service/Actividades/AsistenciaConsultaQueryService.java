package com.blue.open.Code.Service.Actividades;

import com.blue.open.Code.Auth.PermissionEnum;
import com.blue.open.Code.Entity.DTO.AsistenciaActividadConsultaDTO;
import com.blue.open.Code.Entity.DTO.AsistenciaPaginateRequestDTO;
import com.blue.open.Code.Entity.Db.Actividad;
import com.blue.open.Code.Entity.Db.AsistenciaActividad;
import com.blue.open.Code.Entity.Db.Member;
import com.blue.open.Code.Mapper.AsistenciaActividadMapper;
import com.blue.open.Code.Repository.AsistenciaActividadRepository;
import com.blue.open.Code.Repository.AsistenciaActividadSpecifications;
import com.blue.open.Code.Repository.InscripcionActividadRepository;
import com.blue.open.Code.Service.MemberAuthService;
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

import java.time.LocalDate;
import java.util.List;

@Service
public class AsistenciaConsultaQueryService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;
    private static final int STATUS_ACTIVE = 0;

    @Autowired
    private AuthService authService;

    @Autowired
    private AsistenciaAutorizacionService autorizacionService;

    @Autowired
    private MemberAuthService memberAuthService;

    @Autowired
    private AsistenciaActividadRepository asistenciaActividadRepository;

    @Autowired
    private InscripcionActividadRepository inscripcionActividadRepository;

    @Autowired
    private AsistenciaActividadMapper asistenciaActividadMapper;

    @Transactional(readOnly = true)
    public Page<AsistenciaActividadConsultaDTO> paginar(AsistenciaPaginateRequestDTO request, boolean portalProfesor) {
        authService.valid(List.of(portalProfesor
                ? PermissionEnum.ACTIVITY_ATTENDANCE_MANAGE
                : PermissionEnum.ACTIVITY_ATTENDANCE_VIEW));
        Long companyId = authService.getCurrentCompanyOrThrow().getId();

        AsistenciaPaginateRequestDTO req = request != null ? request : new AsistenciaPaginateRequestDTO();
        validarRequest(req);

        Actividad actividad = autorizacionService.requireActividadConsulta(req.getActividadUuid(), companyId, portalProfesor);

        Long memberId = null;
        if (req.getMemberUuid() != null && !req.getMemberUuid().isBlank()) {
            Member member = memberAuthService.resolveMemberByUuid(req.getMemberUuid(), companyId);
            memberId = member.getId();
            if (portalProfesor) {
                validarMemberInscriptoEnActividad(actividad.getId(), memberId);
            }
        }

        int page = Math.max(req.getPage(), 1);
        int size = req.getSize() <= 0 ? DEFAULT_PAGE_SIZE : Math.min(req.getSize(), MAX_PAGE_SIZE);

        Specification<AsistenciaActividad> spec = Specification
                .where(AsistenciaActividadSpecifications.forCompany(companyId))
                .and(AsistenciaActividadSpecifications.forActividadId(actividad.getId()))
                .and(AsistenciaActividadSpecifications.withPaginateRequest(req, memberId))
                .and(AsistenciaActividadSpecifications.fetchConsultaDetails());

        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(
                Sort.Order.desc("fecha"),
                Sort.Order.desc("slot.inicio")));

        return asistenciaActividadRepository.findAll(spec, pageable)
                .map(asistenciaActividadMapper::toConsultaDto);
    }

    private void validarRequest(AsistenciaPaginateRequestDTO req) {
        if (req.getActividadUuid() == null || req.getActividadUuid().isBlank()) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Actividad requerida",
                    "AsistenciaConsultaQueryService | actividadUuid vacío");
        }

        LocalDate fechaDesde = req.getFechaDesde();
        LocalDate fechaHasta = req.getFechaHasta();
        if (fechaDesde != null && fechaHasta != null && fechaHasta.isBefore(fechaDesde)) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Rango de fechas inválido",
                    "AsistenciaConsultaQueryService | fechaHasta anterior a fechaDesde");
        }
    }

    private void validarMemberInscriptoEnActividad(Long actividadId, Long memberId) {
        if (!inscripcionActividadRepository.existsByActividad_IdAndMember_IdAndStatus(
                actividadId, memberId, STATUS_ACTIVE)) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Socio no inscripto en la actividad",
                    "AsistenciaConsultaQueryService | member no inscripto actividadId: " + actividadId
                            + " memberId: " + memberId);
        }
    }
}
