package com.blue.open.Code.Entity.DTO;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class MemberDescuentoActividadDTO {

    private String actividadUuid;
    private String actividadNombre;
    private List<DescuentoAsignadoDTO> descuentos;
}
