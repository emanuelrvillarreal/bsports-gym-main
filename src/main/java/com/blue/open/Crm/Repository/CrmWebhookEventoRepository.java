package com.blue.open.Crm.Repository;

import com.blue.open.Core.Repository.EntityRepository;
import com.blue.open.Crm.Entity.Db.CrmWebhookEvento;
import com.blue.open.Crm.Repository.Custom.CrmWebhookEventoRepositoryCustom;

import java.util.Optional;

/**
 * Acceso a los webhooks del CRM (blue-crm) ya registrados.
 * Lo usa CrmWebhookEventoService para resolver la idempotencia de cada evento recibido.
 */
public interface CrmWebhookEventoRepository
        extends EntityRepository<CrmWebhookEvento>, CrmWebhookEventoRepositoryCustom {

    /**
     * Busca el registro de un evento por su identificador en el CRM.
     * <p>
     * No filtra por status: un evento dado de baja igual tiene que frenar el reproceso, si no
     * se perderia la garantia de idempotencia.
     */
    Optional<CrmWebhookEvento> findFirstByEventId(String eventId);
}
