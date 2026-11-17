package com.blue.open.Code.Controller;

import com.blue.open.Code.Auth.PermissionEnum;
import com.blue.open.Code.Entity.DTO.FacturaPaginateRequestDTO;
import com.blue.open.Code.Entity.DTO.FacturaPaginateRowDTO;
import com.blue.open.Code.Entity.DTO.FacturaPagoRequestDTO;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Service.FacturaAuthService;
import com.blue.open.Code.Service.FacturaQueryService;
import com.blue.open.Code.Service.FacturaService;
import com.blue.open.Core.Service.Auth.AuthService;
import com.blue.open.Crm.Entity.DTO.Response.MercadoPagoPreferenceResponseDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("factura")
public class FacturaController {

    @Autowired
    private AuthService authService;

    @Autowired
    private FacturaAuthService facturaAuthService;

    @Autowired
    private FacturaQueryService facturaQueryService;

    @Autowired
    private FacturaService facturaService;

    @PostMapping("/paginate")
    public ResponseEntity<Page<FacturaPaginateRowDTO>> paginate(@RequestBody FacturaPaginateRequestDTO request) {
        authService.valid(List.of(PermissionEnum.FACTURA_VIEW));
        Company company = facturaAuthService.resolveCompany(request.getCompanyUuid());
        return ResponseEntity.ok(facturaQueryService.paginar(request, company.getId()));
    }

    @PostMapping("/pago")
    public ResponseEntity<MercadoPagoPreferenceResponseDTO> pago(@RequestBody FacturaPagoRequestDTO request) {
        authService.valid(List.of(PermissionEnum.FACTURA_PAGAR));
        Company company = authService.getCurrentCompanyOrThrow();
        return ResponseEntity.ok(facturaService.crearPago(company, request));
    }
}
