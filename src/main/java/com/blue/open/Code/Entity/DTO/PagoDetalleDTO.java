package com.blue.open.Code.Entity.DTO;

import com.blue.open.Code.Entity.Enums.MercadoPagoCobroIntentEstadoEnum;
import com.blue.open.Code.Entity.Enums.MercadoPagoCobroTipoEnum;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class PagoDetalleDTO {

    private String uuid;
    private String externalReference;
    private String titulo;
    private String detalle;
    private BigDecimal montoTotal;
    private MercadoPagoCobroTipoEnum tipo;
    private MercadoPagoCobroIntentEstadoEnum estado;
    private Long paymentId;
    private List<PagoItemDTO> items = new ArrayList<>();
}
