package com.blue.open.Code.Entity.DTO;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * Datos para dar de alta un club.
 * <p>
 * Ademas de los campos del perfil que hereda de ClubProfileFieldsDTO, suma los que solo hacen
 * falta en el alta: el administrador inicial y los codigos que necesita el CRM (blue-crm) para
 * crear el cliente. No lleva codigo de plan: el alta no suscribe, eso es un segundo request
 * (SysadminSuscripcionRequestDTO).
 * <p>
 * Los codigos del CRM van aca y no en ClubProfileFieldsDTO porque esa clase la comparte
 * ClubProfileUpdateRequestDTO, es decir la edicion del club, que no tiene por que pedirlos.
 */
@Getter
@Setter
public class ClubRegistrationRequestDTO extends ClubProfileFieldsDTO {
    @NotNull(message = "Debe proporcionar los datos del administrador inicial.")
    @Valid
    private ClubAdminRequestDTO admin;

    // Codigo de condicion impositiva segun el catalogo del CRM. El CRM lo valida y responde 400
    // si no existe
    @NotBlank(message = "El código de condición impositiva del CRM es obligatorio.")
    private String codigoCondicionImpositiva;

    // Codigo de provincia segun el catalogo del CRM. El CRM lo valida y responde 400 si no existe
    @NotBlank(message = "El código de provincia del CRM es obligatorio.")
    private String codigoProvincia;
}
