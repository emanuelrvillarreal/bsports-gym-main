package com.blue.open.Code.Controller;

import java.time.LocalDate;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.format.annotation.DateTimeFormat;
import com.blue.open.Code.Service.Actividades.AsistenciaUmbralesService;
import com.blue.open.Code.Service.Actividades.AsistenciaEstadisticasService;
import com.blue.open.Code.Entity.DTO.TrayectoriaDeportivaDTO;
import com.blue.open.Code.Entity.DTO.AsistenciaUmbralesDTO;
import com.blue.open.Code.Entity.DTO.AsistenciaEstadisticasRequestDTO;
import com.blue.open.Code.Entity.DTO.AsistenciaEstadisticasDTO;
import com.blue.open.Code.Entity.DTO.AsistenciaAlumnoDetalleDTO;
import com.blue.open.Code.Auth.PermissionEnum;
import com.blue.open.Code.Entity.DTO.InscripcionActividadDTO;
import com.blue.open.Code.Entity.DTO.PortalActividadInscripcionCommand;
import com.blue.open.Code.Service.Actividades.PortalMemberActividadInscripcionService;
import com.blue.open.Core.Service.Auth.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("portal/member/actividad")
public class PortalMemberActividadController {

    @Autowired
    private AuthService authService;

    @Autowired
    private PortalMemberActividadInscripcionService portalMemberActividadInscripcionService;

    @Autowired
    private AsistenciaEstadisticasService estadisticasService;

    @PostMapping("/inscripcion")
    public ResponseEntity<InscripcionActividadDTO> inscribir(
            @Valid @RequestBody PortalActividadInscripcionCommand command) {
        authService.valid(List.of(PermissionEnum.PORTAL_ACTIVITIES_ENROLL));
        return ResponseEntity.ok(portalMemberActividadInscripcionService.inscribir(command));
    }

    @PutMapping("/inscripcion/horarios")
    public ResponseEntity<InscripcionActividadDTO> cambiarHorarios(
            @Valid @RequestBody PortalActividadInscripcionCommand command) {
        authService.valid(List.of(PermissionEnum.PORTAL_ACTIVITIES_ENROLL));
        return ResponseEntity.ok(portalMemberActividadInscripcionService.cambiarHorarios(command));
    }

    @PostMapping("/inscripcion/baja")
    public ResponseEntity<InscripcionActividadDTO> darBaja(
            @Valid @RequestBody PortalActividadInscripcionCommand command) {
        authService.valid(List.of(PermissionEnum.PORTAL_ACTIVITIES_UNENROLL));
        return ResponseEntity.ok(portalMemberActividadInscripcionService.darBaja(command));
    }

    @GetMapping("/inscripcion")
    public ResponseEntity<List<InscripcionActividadDTO>> misInscripciones() {
        authService.valid(List.of(PermissionEnum.PORTAL_ACTIVITIES_VIEW));
        return ResponseEntity.ok(portalMemberActividadInscripcionService.misInscripciones());
    }

    /** Asistencias del socio (o de un familiar con memberUuid) en todas sus actividades. */
    @GetMapping("/asistencia")
    public ResponseEntity<AsistenciaEstadisticasDTO> misAsistencias(
            @RequestParam(required = false) String memberUuid,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ResponseEntity.ok(estadisticasService.asistenciasSocio(memberUuid, desde, hasta));
    }

    @GetMapping("/asistencia/inscripcion/{inscripcionUuid}")
    public ResponseEntity<AsistenciaAlumnoDetalleDTO> detalleAsistencia(
            @PathVariable String inscripcionUuid,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ResponseEntity.ok(estadisticasService.detalleSocio(inscripcionUuid, desde, hasta));
    }

    @GetMapping("/trayectoria")
    public ResponseEntity<TrayectoriaDeportivaDTO> miTrayectoria(@RequestParam(required = false) String memberUuid) {
        return ResponseEntity.ok(estadisticasService.trayectoriaSocio(memberUuid));
    }

    @GetMapping("/inscripcion/familiar/{memberUuid}")
    public ResponseEntity<List<InscripcionActividadDTO>> inscripcionesDeFamiliar(
            @PathVariable String memberUuid) {
        authService.valid(List.of(PermissionEnum.PORTAL_ACTIVITIES_VIEW));
        return ResponseEntity.ok(portalMemberActividadInscripcionService.inscripcionesDeFamiliar(memberUuid));
    }
}
