package com.blue.open.Crm.Entity.DTO.Request;

import com.blue.open.Crm.Entity.Enums.CrmTipoDescuentoEnum;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Descuento a aplicar sobre las facturas de una suscripcion del CRM.
 * Se anida dentro de CrmSuscripcionRequestDTO (POST integraciones/cliente/suscribir).
 * <p>
 * El CRM lo aplica sobre el total con IVA de cada factura, empezando por la factura inicial,
 * durante cantPeriodos facturas. Si el request suscribe a varios planes, cada suscripcion
 * recibe su propio descuento con estos mismos valores.
 */
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CrmDescuentoRequestDTO {

    // Define cual de los dos valores (monto o porcentaje) usa el CRM
    private CrmTipoDescuentoEnum tipoDescuento;

    // Solo con tipoDescuento MONTO. Mayor a cero. El CRM lo ignora con PORCENTAJE
    private BigDecimal monto;

    // Solo con tipoDescuento PORCENTAJE. Entre 1 y 100. El CRM lo ignora con MONTO
    private BigDecimal porcentaje;

    // Cantidad de facturas con descuento, contando la inicial. Mayor o igual a 1
    private Integer cantPeriodos;
}
