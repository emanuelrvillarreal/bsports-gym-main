package com.blue.open.Code.Controller;

import com.blue.open.Code.Entity.DTO.AdminAltaCommand;
import com.blue.open.Code.Entity.DTO.AdminDTO;
import com.blue.open.Code.Entity.DTO.AdminPaginateRequestDTO;
import com.blue.open.Code.Entity.DTO.StaffProfileUpdateCommand;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Service.Admin.AdminAltaService;
import com.blue.open.Code.Service.Admin.AdminBajaService;
import com.blue.open.Code.Service.Admin.AdminQueryService;
import com.blue.open.Code.Service.Admin.AdminUpdateService;
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

@RestController
@RequestMapping("admin")
public class AdminController {

    @Autowired
    private AuthService authService;

    @Autowired
    private AdminAltaService adminAltaService;

    @Autowired
    private AdminQueryService adminQueryService;

    @Autowired
    private AdminUpdateService adminUpdateService;

    @Autowired
    private AdminBajaService adminBajaService;

    @PostMapping
    public ResponseEntity<AdminDTO> alta(@Valid @RequestBody AdminAltaCommand command) {
        return ResponseEntity.ok(adminAltaService.alta(command));
    }

    @PutMapping("/uuid/{userUuid}")
    public ResponseEntity<AdminDTO> actualizar(
            @PathVariable String userUuid,
            @Valid @RequestBody StaffProfileUpdateCommand command) {
        return ResponseEntity.ok(adminUpdateService.actualizar(userUuid, command));
    }

    @PostMapping("/paginate")
    public ResponseEntity<Page<AdminDTO>> paginate(
            @RequestBody(required = false) AdminPaginateRequestDTO request) {
        Company company = authService.getCurrentCompanyOrThrow();
        return ResponseEntity.ok(adminQueryService.paginar(request, company.getId()));
    }

    @DeleteMapping("/{userUuid}")
    public ResponseEntity<Void> bajaLogica(@PathVariable String userUuid) {
        adminBajaService.bajaLogica(userUuid);
        return ResponseEntity.noContent().build();
    }
}
