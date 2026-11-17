package com.blue.open.Code.Controller;

import com.blue.open.Code.Auth.PermissionEnum;
import com.blue.open.Code.Entity.DTO.CobroCuotaCommand;
import com.blue.open.Code.Entity.DTO.CobroCuotaResultDTO;
import com.blue.open.Code.Entity.DTO.CuotaAnularCommand;
import com.blue.open.Code.Entity.DTO.CuotaActividadGeneracionResultDTO;
import com.blue.open.Code.Entity.DTO.CuotaDetalleDTO;
import com.blue.open.Code.Entity.DTO.CuotaFiltroDTO;
import com.blue.open.Code.Entity.DTO.CuotaGrupoTitularDTO;
import com.blue.open.Code.Entity.DTO.CuotaPaginateRequestDTO;
import com.blue.open.Code.Entity.DTO.CuotaPaginateRowDTO;
import com.blue.open.Code.Entity.DTO.CuotaPersonaListDTO;
import com.blue.open.Code.Entity.DTO.CuotaSocialGeneracionResultDTO;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.Member;
import com.blue.open.Code.Entity.Enums.CuotaEstadoEnum;
import com.blue.open.Code.Entity.Enums.CuotaTipoEnum;
import com.blue.open.Code.Service.MemberAuthService;
import com.blue.open.Code.Service.Tesoreria.CobroCuotaService;
import com.blue.open.Code.Service.Tesoreria.CuotaAnulacionService;
import com.blue.open.Code.Service.Tesoreria.CuotaActividadGeneracionService;
import com.blue.open.Code.Service.Tesoreria.CuotaQueryService;
import com.blue.open.Code.Service.Tesoreria.CuotaSocialGeneracionService;
import com.blue.open.Core.Auth.BaseRoleEnum;
import com.blue.open.Core.Service.Auth.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("treasury/cuota")
public class TreasuryCuotaController {

    @Autowired
    private AuthService authService;

    @Autowired
    private CuotaQueryService cuotaQueryService;

    @Autowired
    private CobroCuotaService cobroCuotaService;

    @Autowired
    private MemberAuthService memberAuthService;

    @Autowired
    private CuotaActividadGeneracionService cuotaActividadGeneracionService;

    @Autowired
    private CuotaSocialGeneracionService cuotaSocialGeneracionService;

    @Autowired
    private CuotaAnulacionService cuotaAnulacionService;

    /**
     * Generación manual de cuotas de actividad del club: genera todas las pendientes hasta
     * {@code hasta} (formato dd/MM/yyyy). Con una fecha futura sirve para probar los ciclos.
     */
    @PostMapping("/generacion/actividad")
    public ResponseEntity<CuotaActividadGeneracionResultDTO> generarActividadHasta(
            @RequestParam @DateTimeFormat(pattern = "dd/MM/yyyy") LocalDate hasta) {
        authService.valid(List.of(BaseRoleEnum.ADMIN));
        Company company = authService.getCurrentCompanyOrThrow();

        return ResponseEntity.ok(
                cuotaActividadGeneracionService.generarHastaParaCompany(company.getId(), hasta));
    }

    /** Mismo criterio que {@link #generarActividadHasta}, para cuotas sociales. */
    @PostMapping("/generacion/social")
    public ResponseEntity<CuotaSocialGeneracionResultDTO> generarSocialHasta(
            @RequestParam @DateTimeFormat(pattern = "dd/MM/yyyy") LocalDate hasta) {
        authService.valid(List.of(BaseRoleEnum.ADMIN));
        Company company = authService.getCurrentCompanyOrThrow();

        return ResponseEntity.ok(
                cuotaSocialGeneracionService.generarHastaParaCompany(company.getId(), hasta));
    }

    @PostMapping("/cobro")
    public ResponseEntity<CobroCuotaResultDTO> registrarCobro(@Valid @RequestBody CobroCuotaCommand command) {
        return ResponseEntity.ok(cobroCuotaService.registrarCobro(command));
    }

    @PostMapping("/paginate")
    public ResponseEntity<Page<CuotaPaginateRowDTO>> paginate(@RequestBody CuotaPaginateRequestDTO request) {
        authService.valid(List.of(PermissionEnum.TREASURY_VIEW));
        Company company = authService.getCurrentCompanyOrThrow();
        return ResponseEntity.ok(cuotaQueryService.paginar(request, company.getId()));
    }

    @GetMapping("/persona/{personaMemberUuid}")
    public ResponseEntity<CuotaPersonaListDTO> listarPorPersona(
            @PathVariable String personaMemberUuid,
            @RequestParam(required = false) List<CuotaEstadoEnum> estado,
            @RequestParam(required = false) List<CuotaTipoEnum> tipo) {
        authService.valid(List.of(PermissionEnum.TREASURY_VIEW));
        Company company = authService.getCurrentCompanyOrThrow();
        Member persona = memberAuthService.resolveMemberByUuid(personaMemberUuid, company.getId());
        return ResponseEntity.ok(
                cuotaQueryService.listarPorPersona(persona.getId(), buildFiltro(estado, tipo), company.getId()));
    }

    @GetMapping("/titular/{titularMemberUuid}/grupo")
    public ResponseEntity<CuotaGrupoTitularDTO> listarGrupoPorTitular(
            @PathVariable String titularMemberUuid,
            @RequestParam(required = false) List<CuotaEstadoEnum> estado,
            @RequestParam(required = false) List<CuotaTipoEnum> tipo) {
        authService.valid(List.of(
                PermissionEnum.TREASURY_VIEW,
                PermissionEnum.TREASURY_DEBT_CONSOLIDATED));
        Company company = authService.getCurrentCompanyOrThrow();
        Member titular = memberAuthService.resolveTitularByUuid(titularMemberUuid, company.getId());
        return ResponseEntity.ok(
                cuotaQueryService.listarGrupoPorTitular(titular.getId(), buildFiltro(estado, tipo), company.getId()));
    }

    @GetMapping("/uuid/{cuotaUuid}")
    public ResponseEntity<CuotaDetalleDTO> obtenerPorUuid(@PathVariable String cuotaUuid) {
        authService.valid(List.of(PermissionEnum.TREASURY_VIEW));
        Company company = authService.getCurrentCompanyOrThrow();
        return ResponseEntity.ok(cuotaQueryService.obtenerPorUuid(cuotaUuid, company.getId()));
    }

    @PostMapping("/uuid/{cuotaUuid}/anular")
    public ResponseEntity<CuotaDetalleDTO> anular(
            @PathVariable String cuotaUuid,
            @Valid @RequestBody CuotaAnularCommand command) {
        return ResponseEntity.ok(cuotaAnulacionService.anular(cuotaUuid, command));
    }

    private CuotaFiltroDTO buildFiltro(List<CuotaEstadoEnum> estados, List<CuotaTipoEnum> tipos) {
        CuotaFiltroDTO filtro = new CuotaFiltroDTO();
        if (estados != null && !estados.isEmpty()) {
            filtro.setEstados(estados);
        }
        if (tipos != null && !tipos.isEmpty()) {
            filtro.setTipos(tipos);
        }
        return filtro;
    }
}
