package com.blue.open.Code.Controller;

import com.blue.open.Code.Auth.PermissionEnum;
import com.blue.open.Code.Entity.DTO.BalanceMesDTO;
import com.blue.open.Code.Entity.DTO.BalancePaginateRequestDTO;
import com.blue.open.Code.Entity.DTO.BalanceResumenMesDTO;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Service.Tesoreria.BalanceQueryService;
import com.blue.open.Core.Service.Auth.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("treasury/balance")
public class TreasuryBalanceController {

    @Autowired
    private AuthService authService;

    @Autowired
    private BalanceQueryService balanceQueryService;

    @GetMapping
    public ResponseEntity<BalanceMesDTO> obtenerMesActual() {
        authService.valid(List.of(PermissionEnum.TREASURY_VIEW));
        Company company = authService.getCurrentCompanyOrThrow();
        return ResponseEntity.ok(balanceQueryService.obtenerMesActual(company.getId()));
    }

    @PostMapping("/paginate")
    public ResponseEntity<Page<BalanceResumenMesDTO>> paginateHistorico(
            @RequestBody(required = false) BalancePaginateRequestDTO request) {
        authService.valid(List.of(PermissionEnum.TREASURY_VIEW));
        Company company = authService.getCurrentCompanyOrThrow();
        return ResponseEntity.ok(balanceQueryService.paginarHistorico(request, company.getId()));
    }
}
