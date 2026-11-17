package com.blue.open.Code.Entity.DTO;

import com.blue.open.Code.Entity.Enums.FacturaEstadoEnum;
import com.blue.open.Code.Entity.Enums.FacturaOrdenEnum;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class FacturaPaginateRequestDTO {

    private int page = 1;
    private int size = 10;
    private List<FacturaEstadoEnum> estados = new ArrayList<>();
    private FacturaOrdenEnum orden = FacturaOrdenEnum.FECHA_VENCIMIENTO_DESC;

    // Solo lo usa sysadmin: el admin de club resuelve su company por el header X-Company-Uuid
    private String companyUuid;
}
