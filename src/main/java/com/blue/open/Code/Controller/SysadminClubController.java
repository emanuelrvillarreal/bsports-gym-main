package com.blue.open.Code.Controller;

import com.blue.open.Code.Entity.DTO.ClubRegistrationRequestDTO;
import com.blue.open.Code.Entity.DTO.ClubRegistrationResponseDTO;
import com.blue.open.Code.Entity.DTO.SysadminCambioPlanRequestDTO;
import com.blue.open.Code.Entity.DTO.SysadminMercadoPagoConfigRequestDTO;
import com.blue.open.Code.Entity.DTO.SysadminMercadoPagoConfigResponseDTO;
import com.blue.open.Code.Entity.DTO.SysadminSuscripcionRequestDTO;
import com.blue.open.Code.Entity.DTO.SysadminSuscripcionResponseDTO;
import com.blue.open.Code.Service.SysadminClubService;
import com.blue.open.Code.Service.SysadminMercadoPagoConfigService;
import com.blue.open.Code.Service.SysadminSuscripcionService;
import com.blue.open.Core.Auth.BaseRoleEnum;
import com.blue.open.Core.Service.Auth.AuthService;
import com.blue.open.Crm.Entity.DTO.Response.CrmCancelarSuscripcionesResponseDTO;
import com.blue.open.Crm.Entity.DTO.Response.CrmCondicionImpositivaResponseDTO;
import com.blue.open.Crm.Entity.DTO.Response.CrmPlanResponseDTO;
import com.blue.open.Crm.Entity.DTO.Response.CrmProvinciaResponseDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

import java.util.List;

@RestController
@CrossOrigin
@RequestMapping(path = "/sysadmin/clubs")
public class SysadminClubController {

    @Autowired
    private SysadminClubService sysadminClubService;

    @Autowired
    private SysadminMercadoPagoConfigService sysadminMercadoPagoConfigService;

    @Autowired
    private SysadminSuscripcionService sysadminSuscripcionService;

    @Autowired
    private AuthService authService;

    @PostMapping
    public ResponseEntity<ClubRegistrationResponseDTO> registerClub(
            @Valid @RequestBody ClubRegistrationRequestDTO request) {
        authService.valid(List.of(BaseRoleEnum.SYSADMIN));
        ClubRegistrationResponseDTO response = sysadminClubService.registerClub(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/sync-roles")
    public ResponseEntity<Void> syncRolesForAllActiveCompanies() {
        authService.valid(List.of(BaseRoleEnum.SYSADMIN));
        sysadminClubService.syncRolesForAllActiveCompanies();
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{companyUuid}/mercado-pago")
    public ResponseEntity<SysadminMercadoPagoConfigResponseDTO> obtenerMercadoPago(
            @PathVariable String companyUuid) {
        authService.valid(List.of(BaseRoleEnum.SYSADMIN));
        return ResponseEntity.ok(sysadminMercadoPagoConfigService.obtener(companyUuid));
    }

    @PutMapping("/{companyUuid}/mercado-pago")
    public ResponseEntity<SysadminMercadoPagoConfigResponseDTO> guardarMercadoPago(
            @PathVariable String companyUuid,
            @Valid @RequestBody SysadminMercadoPagoConfigRequestDTO request) {
        authService.valid(List.of(BaseRoleEnum.SYSADMIN));
        return ResponseEntity.ok(sysadminMercadoPagoConfigService.guardar(companyUuid, request));
    }

    @PostMapping("/{companyUuid}/crm/suscribir")
    public ResponseEntity<SysadminSuscripcionResponseDTO> suscribir(
            @PathVariable String companyUuid,
            @Valid @RequestBody SysadminSuscripcionRequestDTO request) {
        authService.valid(List.of(BaseRoleEnum.SYSADMIN));
        return ResponseEntity.ok(sysadminSuscripcionService.suscribir(companyUuid, request));
    }

    @PostMapping("/{companyUuid}/crm/cambiar-plan")
    public ResponseEntity<SysadminSuscripcionResponseDTO> cambiarPlan(
            @PathVariable String companyUuid,
            @Valid @RequestBody SysadminCambioPlanRequestDTO request) {
        authService.valid(List.of(BaseRoleEnum.SYSADMIN));
        return ResponseEntity.ok(sysadminSuscripcionService.cambiarPlan(companyUuid, request));
    }

    @PostMapping("/{companyUuid}/crm/desuscribir")
    public ResponseEntity<CrmCancelarSuscripcionesResponseDTO> desuscribir(@PathVariable String companyUuid) {
        authService.valid(List.of(BaseRoleEnum.SYSADMIN));
        return ResponseEntity.ok(sysadminSuscripcionService.desuscribir(companyUuid));
    }

    @GetMapping("/crm-catalogo/planes")
    public ResponseEntity<List<CrmPlanResponseDTO>> obtenerPlanesCrm() {
        authService.valid(List.of(BaseRoleEnum.SYSADMIN));
        return ResponseEntity.ok(sysadminClubService.obtenerPlanesCrm());
    }

    @GetMapping("/crm-catalogo/provincias")
    public ResponseEntity<List<CrmProvinciaResponseDTO>> obtenerProvinciasCrm() {
        authService.valid(List.of(BaseRoleEnum.SYSADMIN));
        return ResponseEntity.ok(sysadminClubService.obtenerProvinciasCrm());
    }

    @GetMapping("/crm-catalogo/condiciones-impositivas")
    public ResponseEntity<List<CrmCondicionImpositivaResponseDTO>> obtenerCondicionesImpositivasCrm() {
        authService.valid(List.of(BaseRoleEnum.SYSADMIN));
        return ResponseEntity.ok(sysadminClubService.obtenerCondicionesImpositivasCrm());
    }
}
