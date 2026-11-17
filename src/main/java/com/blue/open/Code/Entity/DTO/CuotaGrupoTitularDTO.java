package com.blue.open.Code.Entity.DTO;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class CuotaGrupoTitularDTO {

    private String titularMemberUuid;
    private String titularNombre;
    private String titularApellido;
    private BigDecimal montoTotalGrupo;
    private BigDecimal montoDeudaGrupo;
    private BigDecimal totalPorPagarGrupo;
    private List<CuotaPersonaGrupoDTO> personas = new ArrayList<>();
}
