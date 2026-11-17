package com.blue.open.Code.Service.Actividades;

import com.blue.open.Code.Entity.DTO.ActividadInscripcionCommand;
import com.blue.open.Code.Entity.DTO.InscripcionActividadDTO;
import com.blue.open.Code.Entity.DTO.PortalActividadInscripcionCommand;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.Member;
import com.blue.open.Code.Repository.InscripcionActividadRepository;
import com.blue.open.Code.Service.MemberAuthService;
import com.blue.open.Core.Service.Auth.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PortalMemberActividadInscripcionService {

    @Autowired
    private AuthService authService;

    @Autowired
    private MemberAuthService memberAuthService;

    @Autowired
    private ActividadInscripcionService actividadInscripcionService;

    @Autowired
    private InscripcionActividadBajaService inscripcionActividadBajaService;

    @Autowired
    private InscripcionActividadRepository inscripcionActividadRepository;

    @Transactional
    public InscripcionActividadDTO inscribir(PortalActividadInscripcionCommand command) {
        Company company = authService.getCurrentCompanyOrThrow();

        Member target = resolveTarget(command, company);

        ActividadInscripcionCommand altaCommand = new ActividadInscripcionCommand();
        altaCommand.setMemberUuid(target.getUuid());
        altaCommand.setHorarioUuids(command.getHorarioUuids());

        return actividadInscripcionService.inscribir(command.getActividadUuid(), altaCommand);
    }

    /** El socio (o un familiar a su cargo) cambia los horarios a los que asiste sin darse de baja. */
    @Transactional
    public InscripcionActividadDTO cambiarHorarios(PortalActividadInscripcionCommand command) {
        Company company = authService.getCurrentCompanyOrThrow();
        Member target = resolveTarget(command, company);
        return actividadInscripcionService.cambiarHorarios(
                command.getActividadUuid(), target.getUuid(), command.getHorarioUuids());
    }

    @Transactional
    public InscripcionActividadDTO darBaja(PortalActividadInscripcionCommand command) {
        Company company = authService.getCurrentCompanyOrThrow();

        Member target = resolveTarget(command, company);

        ActividadInscripcionCommand bajaCommand = new ActividadInscripcionCommand();
        bajaCommand.setMemberUuid(target.getUuid());

        return inscripcionActividadBajaService.darBaja(command.getActividadUuid(), bajaCommand);
    }

    private Member resolveTarget(PortalActividadInscripcionCommand command, Company company) {
        return (command.getMemberUuid() == null || command.getMemberUuid().isBlank())
                ? memberAuthService.getCurrentMemberOrThrow(company.getId())
                : memberAuthService.resolveMemberOfFamilyOrThrow(command.getMemberUuid(), company.getId());
    }

    @Transactional(readOnly = true)
    public List<InscripcionActividadDTO> misInscripciones() {
        Company company = authService.getCurrentCompanyOrThrow();
        Member member = memberAuthService.getCurrentMemberOrThrow(company.getId());
        return listarInscripciones(member, company.getId());
    }

    @Transactional(readOnly = true)
    public List<InscripcionActividadDTO> inscripcionesDeFamiliar(String memberUuid) {
        Company company = authService.getCurrentCompanyOrThrow();
        Member member = memberAuthService.resolveMemberOfFamilyOrThrow(memberUuid, company.getId());
        return listarInscripciones(member, company.getId());
    }

    private List<InscripcionActividadDTO> listarInscripciones(Member member, Long companyId) {
        return inscripcionActividadRepository
                .findActivasByMemberIdAndCompanyIdWithActividad(member.getId(), companyId, 0)
                .stream()
                .map(i -> actividadInscripcionService.toDto(i, i.getActividad(), member, null))
                .toList();
    }
}
