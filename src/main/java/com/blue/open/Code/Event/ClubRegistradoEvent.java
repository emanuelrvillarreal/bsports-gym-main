package com.blue.open.Code.Event;

import lombok.Getter;

/**
 * Publicado al terminar el alta de un club.
 * {@link ClubRegistradoEventListener} le asocia de forma asíncrona las facturas que el CRM informó
 * para su cliente (uuidCliente o CUIT) antes de que el club existiera.
 */
@Getter
public class ClubRegistradoEvent {

    private final Long companyId;

    // Null en el alta por sysadmin: ese club todavia no conoce su uuid del CRM
    private final String uuidCliente;

    private final String cuit;

    public ClubRegistradoEvent(Long companyId, String uuidCliente, String cuit) {
        this.companyId = companyId;
        this.uuidCliente = uuidCliente;
        this.cuit = cuit;
    }
}
