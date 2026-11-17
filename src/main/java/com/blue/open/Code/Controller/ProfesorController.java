package com.blue.open.Code.Controller;

import com.blue.open.Code.Auth.PermissionEnum;
import com.blue.open.Code.Entity.DTO.ProfesorAltaCommand;
import com.blue.open.Code.Entity.DTO.ProfesorDTO;
import com.blue.open.Code.Entity.DTO.ProfesorPaginateRequestDTO;
import com.blue.open.Code.Entity.DTO.StaffProfileUpdateCommand;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Service.Actividades.ProfesorAltaService;
import com.blue.open.Code.Service.Actividades.ProfesorBajaService;
import com.blue.open.Code.Service.Actividades.ProfesorQueryService;
import com.blue.open.Code.Service.Actividades.ProfesorUpdateService;
import com.blue.open.Core.Service.Auth.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("profesor")
public class ProfesorController {

    @Autowired
    private AuthService authService;

    @Autowired
    private ProfesorQueryService profesorQueryService;

    @Autowired
    private ProfesorAltaService profesorAltaService;

    @Autowired
    private ProfesorBajaService profesorBajaService;

    @Autowired
    private ProfesorUpdateService profesorUpdateService;

    @PostMapping
    public ResponseEntity<ProfesorDTO> alta(@Valid @RequestBody ProfesorAltaCommand command) {
        return ResponseEntity.ok(profesorAltaService.alta(command));
    }

    @PutMapping("/uuid/{profesorUuid}")
    public ResponseEntity<ProfesorDTO> actualizar(
            @PathVariable String profesorUuid,
            @Valid @RequestBody StaffProfileUpdateCommand command) {
        return ResponseEntity.ok(profesorUpdateService.actualizar(profesorUuid, command));
    }

    @PostMapping("/paginate")
    public ResponseEntity<Page<ProfesorDTO>> paginate(
            @RequestBody(required = false) ProfesorPaginateRequestDTO request) {
        authService.valid(List.of(PermissionEnum.ACTIVITY_VIEW));
        Company company = authService.getCurrentCompanyOrThrow();
        return ResponseEntity.ok(profesorQueryService.paginar(request, company.getId()));
    }

    @DeleteMapping("/{profesorUuid}")
    public ResponseEntity<Void> bajaLogica(@PathVariable String profesorUuid) {
        profesorBajaService.bajaLogica(profesorUuid);
        return ResponseEntity.noContent().build();
    }
}
