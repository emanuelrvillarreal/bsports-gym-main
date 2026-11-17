package com.blue.open.Code.Entity.DTO;

import com.blue.open.Code.Entity.Enums.MemberEstadoEnum;
import com.blue.open.Code.Entity.Enums.ParentescoEnum;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class CuotaPersonaGrupoDTO {

    private String personaMemberUuid;
    private String nombre;
    private String apellido;
    private ParentescoEnum parentesco;
    private MemberEstadoEnum estadoMember;
    private List<CuotaResumenDTO> cuotas = new ArrayList<>();
    private BigDecimal subtotalPersona;
    private BigDecimal subtotalDeudaPersona;
    private BigDecimal totalPorPagar;
}
