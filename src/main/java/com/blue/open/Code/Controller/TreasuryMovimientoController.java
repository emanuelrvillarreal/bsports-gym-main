package com.blue.open.Code.Controller;

import com.blue.open.Code.Auth.PermissionEnum;
import com.blue.open.Code.Entity.DTO.MovimientoAnularCommand;
import com.blue.open.Code.Entity.DTO.MovimientoDetalleDTO;
import com.blue.open.Code.Entity.DTO.MovimientoManualCommand;
import com.blue.open.Code.Entity.DTO.MovimientoPaginateRequestDTO;
import com.blue.open.Code.Entity.DTO.MovimientoRowDTO;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Service.Tesoreria.MovimientoAnulacionService;
import com.blue.open.Code.Service.Tesoreria.MovimientoManualService;
import com.blue.open.Code.Service.Tesoreria.MovimientoQueryService;
import com.blue.open.Core.Service.Auth.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("treasury/movimiento")
public class TreasuryMovimientoController {

    @Autowired
    private AuthService authService;

    @Autowired
    private MovimientoQueryService movimientoQueryService;

    @Autowired
    private MovimientoManualService movimientoManualService;

    @Autowired
    private MovimientoAnulacionService movimientoAnulacionService;

    @PostMapping("/paginate")
    public ResponseEntity<Page<MovimientoRowDTO>> paginate(
            @RequestBody(required = false) MovimientoPaginateRequestDTO request) {
        authService.valid(List.of(PermissionEnum.TREASURY_VIEW));
        Company company = authService.getCurrentCompanyOrThrow();
        return ResponseEntity.ok(movimientoQueryService.paginar(request, company.getId()));
    }

    @GetMapping("/uuid/{movimientoUuid}")
    public ResponseEntity<MovimientoDetalleDTO> obtenerPorUuid(@PathVariable String movimientoUuid) {
        authService.valid(List.of(PermissionEnum.TREASURY_VIEW));
        Company company = authService.getCurrentCompanyOrThrow();
        return ResponseEntity.ok(movimientoQueryService.obtenerPorUuid(movimientoUuid, company.getId()));
    }

    @PostMapping
    public ResponseEntity<MovimientoDetalleDTO> registrarManual(@Valid @RequestBody MovimientoManualCommand command) {
        return ResponseEntity.status(HttpStatus.CREATED).body(movimientoManualService.registrar(command));
    }

    @PostMapping("/uuid/{movimientoUuid}/anular")
    public ResponseEntity<MovimientoDetalleDTO> anular(
            @PathVariable String movimientoUuid,
            @Valid @RequestBody MovimientoAnularCommand command) {
        return ResponseEntity.ok(movimientoAnulacionService.anular(movimientoUuid, command));
    }
}
