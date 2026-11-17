package com.blue.open.Code.Controller;

import com.blue.open.Code.Entity.DTO.RecepcionistaAltaCommand;
import com.blue.open.Code.Entity.DTO.RecepcionistaDTO;
import com.blue.open.Code.Entity.DTO.RecepcionistaPaginateRequestDTO;
import com.blue.open.Code.Entity.DTO.StaffProfileUpdateCommand;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Service.Recepcionista.RecepcionistaAltaService;
import com.blue.open.Code.Service.Recepcionista.RecepcionistaBajaService;
import com.blue.open.Code.Service.Recepcionista.RecepcionistaPortalProfileService;
import com.blue.open.Code.Service.Recepcionista.RecepcionistaQueryService;
import com.blue.open.Code.Service.Recepcionista.RecepcionistaUpdateService;
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

@RestController
@RequestMapping("recepcionista")
public class RecepcionistaController {

    @Autowired
    private AuthService authService;

    @Autowired
    private RecepcionistaAltaService recepcionistaAltaService;

    @Autowired
    private RecepcionistaQueryService recepcionistaQueryService;

    @Autowired
    private RecepcionistaBajaService recepcionistaBajaService;

    @Autowired
    private RecepcionistaUpdateService recepcionistaUpdateService;

    @Autowired
    private RecepcionistaPortalProfileService recepcionistaPortalProfileService;

    @PostMapping
    public ResponseEntity<RecepcionistaDTO> alta(@Valid @RequestBody RecepcionistaAltaCommand command) {
        return ResponseEntity.ok(recepcionistaAltaService.alta(command));
    }

    @PutMapping("/uuid/{recepcionistaUuid}")
    public ResponseEntity<RecepcionistaDTO> actualizar(
            @PathVariable String recepcionistaUuid,
            @Valid @RequestBody StaffProfileUpdateCommand command) {
        return ResponseEntity.ok(recepcionistaUpdateService.actualizar(recepcionistaUuid, command));
    }

    @PostMapping("/paginate")
    public ResponseEntity<Page<RecepcionistaDTO>> paginate(
            @RequestBody(required = false) RecepcionistaPaginateRequestDTO request) {
        Company company = authService.getCurrentCompanyOrThrow();
        return ResponseEntity.ok(recepcionistaQueryService.paginar(request, company.getId()));
    }

    @DeleteMapping("/{recepcionistaUuid}")
    public ResponseEntity<Void> bajaLogica(@PathVariable String recepcionistaUuid) {
        recepcionistaBajaService.bajaLogica(recepcionistaUuid);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/perfil")
    public ResponseEntity<RecepcionistaDTO> obtenerPerfil() {
        return ResponseEntity.ok(recepcionistaPortalProfileService.obtenerPerfil());
    }

    @PutMapping("/perfil")
    public ResponseEntity<RecepcionistaDTO> actualizarPerfil(@Valid @RequestBody StaffProfileUpdateCommand command) {
        return ResponseEntity.ok(recepcionistaPortalProfileService.actualizarPerfil(command));
    }
}
