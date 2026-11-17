package com.blue.open.Code.Entity.DTO;

import com.blue.open.Code.Entity.Enums.CuotaEstadoEnum;
import com.blue.open.Code.Entity.Enums.CuotaOrdenEnum;
import com.blue.open.Code.Entity.Enums.CuotaTipoEnum;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class CuotaFiltroDTO {

    private List<CuotaEstadoEnum> estados = new ArrayList<>();
    private List<CuotaTipoEnum> tipos = new ArrayList<>();
    private CuotaOrdenEnum orden = CuotaOrdenEnum.VENCIMIENTO_ASC;
    private Integer ultimosMeses;
}
