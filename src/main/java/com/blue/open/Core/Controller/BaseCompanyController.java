package com.blue.open.Core.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;

import com.blue.open.Code.Entity.DTO.CompanyContextDTO;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Core.Entity.DTO.BaseCompanyDTO;
import com.blue.open.Core.Service.Auth.AuthService;
import com.blue.open.Core.Service.BaseCompanyService;

public abstract class BaseCompanyController<DTO extends BaseCompanyDTO, S extends BaseCompanyService<DTO>>
                extends AbstractController<Company, DTO, DTO, S> {

    @Autowired
    protected AuthService authService;

    @Autowired
    protected S service;

    /**
     * Expone el modo multi-empresa / mono-empresa y la company resuelta para la petición actual.
     * Público: no requiere autenticación. Si hay sesión, incluye currentCompany.
     */
    @GetMapping("/context")
    public ResponseEntity<CompanyContextDTO> getContext() {
        authService.isSysAdmin();
        return ResponseEntity.ok(service.getContext());
    }
}
