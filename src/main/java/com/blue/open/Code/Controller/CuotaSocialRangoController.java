package com.blue.open.Code.Controller;

import com.blue.open.Code.Auth.PermissionEnum;
import com.blue.open.Code.Entity.DTO.CuotaSocialRangoAltaCommand;
import com.blue.open.Code.Entity.DTO.CuotaSocialRangoDTO;
import com.blue.open.Code.Entity.DTO.CuotaSocialRangoEdicionCommand;
import com.blue.open.Code.Entity.DTO.CuotaSocialRangoPaginateRequestDTO;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Service.CuotaSocialRango.CuotaSocialRangoAltaService;
import com.blue.open.Code.Service.CuotaSocialRango.CuotaSocialRangoBajaService;
import com.blue.open.Code.Service.CuotaSocialRango.CuotaSocialRangoEdicionService;
import com.blue.open.Code.Service.CuotaSocialRango.CuotaSocialRangoQueryService;
import com.blue.open.Core.Service.Auth.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
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
@RequestMapping("cuota-social-rango")
public class CuotaSocialRangoController {

    @Autowired
    private AuthService authService;

    @Autowired
    private CuotaSocialRangoAltaService cuotaSocialRangoAltaService;

    @Autowired
    private CuotaSocialRangoEdicionService cuotaSocialRangoEdicionService;

    @Autowired
    private CuotaSocialRangoQueryService cuotaSocialRangoQueryService;

    @Autowired
    private CuotaSocialRangoBajaService cuotaSocialRangoBajaService;

    @PostMapping
    public ResponseEntity<CuotaSocialRangoDTO> alta(@Valid @RequestBody CuotaSocialRangoAltaCommand command) {
        authService.valid(List.of(PermissionEnum.CUOTA_SOCIAL_RANGO_CREATE));
        return ResponseEntity.status(HttpStatus.CREATED).body(cuotaSocialRangoAltaService.alta(command));
    }

    @PutMapping("/{cuotaSocialRangoUuid}")
    public ResponseEntity<CuotaSocialRangoDTO> editar(
            @PathVariable String cuotaSocialRangoUuid,
            @Valid @RequestBody CuotaSocialRangoEdicionCommand command) {
        authService.valid(List.of(PermissionEnum.CUOTA_SOCIAL_RANGO_EDIT));
        return ResponseEntity.ok(cuotaSocialRangoEdicionService.editar(cuotaSocialRangoUuid, command));
    }

    @DeleteMapping("/{cuotaSocialRangoUuid}")
    public ResponseEntity<Void> bajaLogica(@PathVariable String cuotaSocialRangoUuid) {
        authService.valid(List.of(PermissionEnum.CUOTA_SOCIAL_RANGO_DELETE));
        cuotaSocialRangoBajaService.bajaLogica(cuotaSocialRangoUuid);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/paginate")
    public ResponseEntity<Page<CuotaSocialRangoDTO>> paginate(
            @RequestBody(required = false) CuotaSocialRangoPaginateRequestDTO request) {
        authService.valid(List.of(PermissionEnum.CUOTA_SOCIAL_RANGO_VIEW));
        Company company = authService.getCurrentCompanyOrThrow();
        return ResponseEntity.ok(cuotaSocialRangoQueryService.paginar(request, company.getId()));
    }
}
