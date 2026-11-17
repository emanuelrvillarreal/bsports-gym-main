package com.blue.open.Code.Controller;

import com.blue.open.Code.Auth.PermissionEnum;
import com.blue.open.Code.Entity.DTO.ActividadAltaCommand;
import com.blue.open.Code.Entity.DTO.ActividadDTO;
import com.blue.open.Code.Entity.DTO.ActividadEdicionCommand;
import com.blue.open.Code.Entity.DTO.ActividadInscripcionCommand;
import com.blue.open.Code.Entity.DTO.ActividadPaginateDTO;
import com.blue.open.Code.Entity.DTO.ActividadPaginateRequestDTO;
import com.blue.open.Code.Entity.DTO.InscripcionActividadDTO;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Service.Actividades.ActividadAltaService;
import com.blue.open.Code.Service.Actividades.ActividadBajaService;
import com.blue.open.Code.Service.Actividades.ActividadEdicionService;
import com.blue.open.Code.Service.Actividades.ActividadInscripcionService;
import com.blue.open.Code.Service.Actividades.ActividadQueryService;
import com.blue.open.Code.Service.Actividades.InscripcionActividadBajaService;
import com.blue.open.Core.Service.Auth.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("actividad")
public class ActividadController {

    @Autowired
    private AuthService authService;

    @Autowired
    private ActividadAltaService actividadAltaService;

    @Autowired
    private ActividadEdicionService actividadEdicionService;

    @Autowired
    private ActividadBajaService actividadBajaService;

    @Autowired
    private ActividadInscripcionService actividadInscripcionService;

    @Autowired
    private InscripcionActividadBajaService inscripcionActividadBajaService;

    @Autowired
    private ActividadQueryService actividadQueryService;

    @PostMapping
    public ResponseEntity<ActividadDTO> alta(@Valid @RequestBody ActividadAltaCommand command) {
        return ResponseEntity.ok(actividadAltaService.alta(command));
    }

    @PutMapping("/{actividadUuid}")
    public ResponseEntity<ActividadDTO> editar(
            @PathVariable String actividadUuid,
            @Valid @RequestBody ActividadEdicionCommand command) {
        return ResponseEntity.ok(actividadEdicionService.editar(actividadUuid, command));
    }

    @GetMapping("/{actividadUuid}")
    public ResponseEntity<ActividadDTO> obtener(@PathVariable String actividadUuid) {
        authService.valid(List.of(PermissionEnum.ACTIVITY_VIEW));
        Company company = authService.getCurrentCompanyOrThrow();
        return ResponseEntity.ok(actividadQueryService.findByUuid(actividadUuid, company.getId()));
    }

    @PostMapping("/paginate")
    public ResponseEntity<Page<ActividadPaginateDTO>> paginate(
            @RequestBody(required = false) ActividadPaginateRequestDTO request) {
        authService.valid(List.of(PermissionEnum.ACTIVITY_VIEW));
        Company company = authService.getCurrentCompanyOrThrow();
        return ResponseEntity.ok(actividadQueryService.paginar(request, company.getId()));
    }

    @PostMapping("/{actividadUuid}/inscripcion")
    public ResponseEntity<InscripcionActividadDTO> inscribir(
            @PathVariable String actividadUuid,
            @Valid @RequestBody ActividadInscripcionCommand command) {
        authService.valid(List.of(PermissionEnum.ACTIVITY_ENROLL));
        return ResponseEntity.ok(actividadInscripcionService.inscribir(actividadUuid, command));
    }

    @PutMapping("/{actividadUuid}/inscripcion/horarios")
    public ResponseEntity<InscripcionActividadDTO> cambiarHorariosInscripcion(
            @PathVariable String actividadUuid,
            @Valid @RequestBody ActividadInscripcionCommand command) {
        authService.valid(List.of(PermissionEnum.ACTIVITY_ENROLL));
        return ResponseEntity.ok(actividadInscripcionService.cambiarHorarios(
                actividadUuid, command.getMemberUuid(), command.getHorarioUuids()));
    }

    @PostMapping("/{actividadUuid}/inscripcion/baja")
    public ResponseEntity<InscripcionActividadDTO> darBajaInscripcion(
            @PathVariable String actividadUuid,
            @Valid @RequestBody ActividadInscripcionCommand command) {
        authService.valid(List.of(PermissionEnum.ACTIVITY_UNENROLL));
        return ResponseEntity.ok(inscripcionActividadBajaService.darBaja(actividadUuid, command));
    }

    @DeleteMapping("/{actividadUuid}")
    public ResponseEntity<Void> bajaLogica(@PathVariable String actividadUuid) {
        actividadBajaService.bajaLogica(actividadUuid);
        return ResponseEntity.noContent().build();
    }
}
