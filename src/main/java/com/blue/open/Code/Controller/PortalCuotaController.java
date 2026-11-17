package com.blue.open.Code.Controller;

import com.blue.open.Code.Auth.PermissionEnum;
import com.blue.open.Code.Entity.DTO.CuotaFiltroDTO;
import com.blue.open.Code.Entity.DTO.CuotaGrupoTitularDTO;
import com.blue.open.Code.Entity.DTO.CuotaPersonaListDTO;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.Member;
import com.blue.open.Code.Entity.Enums.CuotaEstadoEnum;
import com.blue.open.Code.Entity.Enums.CuotaTipoEnum;
import com.blue.open.Code.Service.MemberAuthService;
import com.blue.open.Code.Service.Tesoreria.CuotaQueryService;
import com.blue.open.Core.Service.Auth.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("portal/cuota")
public class PortalCuotaController {

    @Autowired
    private AuthService authService;

    @Autowired
    private CuotaQueryService cuotaQueryService;

    @Autowired
    private MemberAuthService memberAuthService;

    @GetMapping("/mias")
    public ResponseEntity<CuotaPersonaListDTO> listarMias(
            @RequestParam(required = false) List<CuotaEstadoEnum> estado,
            @RequestParam(required = false) List<CuotaTipoEnum> tipo,
            @RequestParam(required = false) Integer ultimosMeses) {
        authService.valid(List.of(PermissionEnum.PORTAL_FEE_VIEW));
        Company company = authService.getCurrentCompanyOrThrow();
        Member member = memberAuthService.getCurrentMemberOrThrow(company.getId());
        CuotaFiltroDTO filtro = buildFiltro(estado, tipo);
        filtro.setUltimosMeses(ultimosMeses);
        return ResponseEntity.ok(
                cuotaQueryService.listarPorPersona(member.getId(), filtro, company.getId()));
    }

    @GetMapping("/grupo")
    public ResponseEntity<CuotaGrupoTitularDTO> listarGrupo(
            @RequestParam(required = false) List<CuotaEstadoEnum> estado,
            @RequestParam(required = false) List<CuotaTipoEnum> tipo) {
        authService.valid(List.of(PermissionEnum.PORTAL_FEE_VIEW));
        authService.valid(List.of(PermissionEnum.PORTAL_FAMILY_VIEW));
        Company company = authService.getCurrentCompanyOrThrow();
        Member titular = memberAuthService.requireTitular(
                memberAuthService.getCurrentMemberOrThrow(company.getId()));
        return ResponseEntity.ok(
                cuotaQueryService.listarGrupoPorTitular(titular.getId(), buildFiltro(estado, tipo), company.getId()));
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
