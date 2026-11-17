package com.blue.open.Code.Service.Actividades;

import com.blue.open.Code.Entity.DTO.ActividadInscripcionCommand;
import com.blue.open.Code.Entity.DTO.InscripcionActividadDTO;
import com.blue.open.Code.Entity.Db.Actividad;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.InscripcionActividad;
import com.blue.open.Code.Entity.Db.Member;
import com.blue.open.Code.Entity.Db.User;
import com.blue.open.Code.Repository.ActividadRepository;
import com.blue.open.Code.Repository.InscripcionActividadRepository;
import com.blue.open.Code.Service.MemberAuthService;
import com.blue.open.Core.Exceptions.CustomException;
import com.blue.open.Core.Service.Auth.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class InscripcionActividadBajaService {

    private static final int STATUS_ACTIVE = 0;

    @Autowired
    private ActividadHorariosService actividadHorariosService;

    @Autowired
    private AuthService authService;

    @Autowired
    private ActividadRepository actividadRepository;

    @Autowired
    private InscripcionActividadRepository inscripcionActividadRepository;

    @Autowired
    private MemberAuthService memberAuthService;

    @Autowired
    private MemberCategoriaInscripcionService memberCategoriaInscripcionService;

    @Transactional
    public InscripcionActividadDTO darBaja(String actividadUuid, ActividadInscripcionCommand command) {
        Company company = authService.getCurrentCompanyOrThrow();

        Actividad actividad = actividadRepository.findByUuidAndCompany_IdAndStatusForUpdate(actividadUuid, company.getId())
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Actividad no encontrada",
                        "InscripcionActividadBajaService | actividadUuid: " + actividadUuid));

        Member member = memberAuthService.resolveMemberByUuid(command.getMemberUuid(), company.getId());

        InscripcionActividad inscripcion = inscripcionActividadRepository
                .findByActividad_IdAndMember_IdAndStatus(actividad.getId(), member.getId(), STATUS_ACTIVE)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND,
                        "El socio no está inscripto en esta actividad",
                        "InscripcionActividadBajaService | actividadId: " + actividad.getId()
                                + " memberId: " + member.getId()));

        inscripcion.cerrarPeriodo(command.getMotivoBaja());
        inscripcion = inscripcionActividadRepository.save(inscripcion);

        memberCategoriaInscripcionService.desasignarSiCorresponde(member, actividad);

        sincronizarCompletaActividad(actividad);

        return toDto(inscripcion, actividad, member);
    }

    @Transactional
    public void bajaTodasActivasPorMember(Long memberId, Long companyId) {
        List<InscripcionActividad> inscripciones = inscripcionActividadRepository
                .findActivasByMemberIdAndCompanyIdWithActividad(memberId, companyId, STATUS_ACTIVE);
        if (inscripciones.isEmpty()) {
            return;
        }

        Set<Long> actividadIds = new LinkedHashSet<>();
        for (InscripcionActividad inscripcion : inscripciones) {
            inscripcion.cerrarPeriodo("Baja del socio en el club");
            actividadIds.add(inscripcion.getActividad().getId());
        }
        inscripcionActividadRepository.saveAll(inscripciones);

        for (Long actividadId : actividadIds) {
            actividadRepository.findByIdAndCompany_IdAndStatusForUpdate(actividadId, companyId)
                    .ifPresent(this::sincronizarCompletaActividad);
        }

        Member member = inscripciones.get(0).getMember();
        memberCategoriaInscripcionService.limpiar(member);
    }

    public void bajaTodasActivasDelGrupo(List<Member> grupo, Long companyId) {
        if (grupo == null || grupo.isEmpty() || companyId == null) {
            return;
        }
        for (Member member : grupo) {
            if (member.getId() != null) {
                bajaTodasActivasPorMember(member.getId(), companyId);
            }
        }
    }

    // * Con cupo por horario, la actividad esta completa solo si todos sus horarios estan llenos.
    private void sincronizarCompletaActividad(Actividad actividad) {
        actividadHorariosService.sincronizarCompleta(actividad);
    }

    private InscripcionActividadDTO toDto(
            InscripcionActividad inscripcion,
            Actividad actividad,
            Member member) {
        User user = member.getUser();

        InscripcionActividadDTO dto = new InscripcionActividadDTO();
        dto.setInscripcionUuid(inscripcion.getUuid());
        dto.setActividadUuid(actividad.getUuid());
        dto.setActividadNombre(actividad.getNombre());
        dto.setMemberUuid(member.getUuid());
        dto.setMemberNombre(user != null ? user.getNombre() : null);
        dto.setMemberApellido(user != null ? user.getApellido() : null);
        dto.setFechaInscripcion(inscripcion.getFechaInscripcion());
        dto.setActividadCompleta(actividad.isCompleta());
        return dto;
    }
}
