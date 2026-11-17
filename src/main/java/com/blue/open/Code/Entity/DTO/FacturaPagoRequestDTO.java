package com.blue.open.Code.Entity.DTO;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class FacturaPagoRequestDTO {

    // Uuids locales de Factura (los mismos que devuelve POST /factura/paginate)
    private List<String> facturasUuid = new ArrayList<>();

    private String referencia;

    private LocalDate fechaPago;
}
