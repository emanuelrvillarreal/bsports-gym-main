package com.blue.open.Crm.Entity.DTO.Request;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Pedido de creacion de un pago (Mercado Pago) para una o mas facturas de un cliente del CRM.
 * Se envia en POST integraciones/pagos/crear.
 * <p>
 * facturasUuid son los uuids de factura DEL CRM (Factura.uuidFacturaCrm de este lado, no el
 * uuid local): el CRM resuelve cada factura por ese identificador.
 */
@Getter
@Setter
public class PagoIntegracionRequestDTO {

    private List<String> facturasUuid = new ArrayList<>();

    // CUIT sin guiones, 11 digitos
    private String cuit;

    // Referencia externa del pago. La define quien pide el pago (el front)
    private String referencia;

    private LocalDate fechaPago;

    // URLs de retorno de Mercado Pago tras el pago. Mismo dominio que los pagos de cuotas
    // (FrontUrlProperties), pero con el path propio de facturas
    private String urlExito;

    private String urlError;

    private String urlPendiente;
}
