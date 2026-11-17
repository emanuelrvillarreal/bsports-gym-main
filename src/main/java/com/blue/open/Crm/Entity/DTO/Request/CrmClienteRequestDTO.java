package com.blue.open.Crm.Entity.DTO.Request;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

/**
 * Datos del cliente a dar de alta en el CRM.
 * Se envia en POST integraciones/cliente/crear.
 * <p>
 * Si el CUIT ya existe en el CRM, el alta no falla: responde estado CLIENTE_EXISTENTE.
 * <p>
 * Restricciones que aplica el CRM: razonSocial obligatoria (max 255), cuit obligatorio de
 * 11 digitos, codigoCondicionImpositiva y codigoProvincia obligatorios,
 * direccionFiscal (max 255), correoFacturacion con formato de email (max 255).
 */
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CrmClienteRequestDTO {

    private String razonSocial;

    // CUIT sin guiones, 11 digitos
    private String cuit;

    // Codigo de la condicion impositiva segun el catalogo del CRM, ej: "RI"
    private String codigoCondicionImpositiva;

    // Codigo de provincia segun el catalogo del CRM
    private String codigoProvincia;

    private String direccionFiscal;

    private String correoFacturacion;

    private CrmContactoRequestDTO contactoPrincipal;

    // Indica si el CRM debe crear una cuenta corriente para el cliente
    private Boolean tieneCuentaCorriente;
}
