package com.blue.open.Crm.Entity.DTO.Request;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

/**
 * Contacto principal del cliente que se envia al CRM al crearlo.
 * Se anida dentro de CrmClienteRequestDTO (POST integraciones/cliente/crear).
 * <p>
 * Restricciones que aplica el CRM: nombre obligatorio (max 150), email valido (max 150),
 * telefono con formato ^[0-9+\-\s]{6,20}$, rol (max 100), descripcion (max 500).
 */
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CrmContactoRequestDTO {

    private String nombre;

    private String email;

    private String telefono;

    // Rol del contacto dentro de la empresa cliente, ej: "Administrativo"
    private String rol;

    private String descripcion;
}
