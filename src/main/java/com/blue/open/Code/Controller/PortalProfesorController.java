package com.blue.open.Code.Controller;

import com.blue.open.Code.Entity.DTO.PortalProfesorCredencialDTO;
import com.blue.open.Code.Entity.DTO.ProfesorDTO;
import com.blue.open.Code.Entity.DTO.StaffProfileUpdateCommand;
import com.blue.open.Code.Service.Actividades.ProfesorPortalCredencialService;
import com.blue.open.Code.Service.Actividades.ProfesorPortalProfileService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("portal/profesor")
public class PortalProfesorController {

    @Autowired
    private ProfesorPortalCredencialService profesorPortalCredencialService;

    @Autowired
    private ProfesorPortalProfileService profesorPortalProfileService;

    @GetMapping("/credencial")
    public ResponseEntity<PortalProfesorCredencialDTO> obtenerCredencial() {
        return ResponseEntity.ok(profesorPortalCredencialService.obtenerCredencial());
    }

    @GetMapping("/perfil")
    public ResponseEntity<ProfesorDTO> obtenerPerfil() {
        return ResponseEntity.ok(profesorPortalProfileService.obtenerPerfil());
    }

    @PutMapping("/perfil")
    public ResponseEntity<ProfesorDTO> actualizarPerfil(@Valid @RequestBody StaffProfileUpdateCommand command) {
        return ResponseEntity.ok(profesorPortalProfileService.actualizarPerfil(command));
    }
}
