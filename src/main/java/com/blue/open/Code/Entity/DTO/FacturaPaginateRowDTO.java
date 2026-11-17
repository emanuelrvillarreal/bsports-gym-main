package com.blue.open.Code.Entity.DTO;

import com.blue.open.Code.Entity.Enums.FacturaEstadoEnum;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class FacturaPaginateRowDTO {

    private String facturaUuid;
    private String uuidFacturaCrm;
    private String codigoPlan;
    private List<String> codigosProductos;

    // Puede venir null: la factura se creo cuando el plan/producto que la origino todavia no
    // estaba sincronizado en el catalogo local, asi que no se pudo resolver su precio
    private BigDecimal importe;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy")
    private LocalDate fechaGeneracion;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd/MM/yyyy")
    private LocalDate fechaVencimiento;

    private FacturaEstadoEnum estado;
}
