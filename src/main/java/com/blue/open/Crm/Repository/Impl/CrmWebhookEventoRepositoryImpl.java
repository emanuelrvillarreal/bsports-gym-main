package com.blue.open.Crm.Repository.Impl;

import com.blue.open.Core.Entity.DTO.PaginateList;
import com.blue.open.Core.Repository.Impl.RepositoryImpl;
import com.blue.open.Crm.Entity.Db.CrmWebhookEvento;
import com.blue.open.Crm.Repository.Custom.CrmWebhookEventoRepositoryCustom;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;

/**
 * Implementacion de las consultas propias de CrmWebhookEventoRepository.
 * Delega la paginacion en RepositoryImpl del Core, igual que el resto de los repositorios.
 */
public class CrmWebhookEventoRepositoryImpl extends RepositoryImpl<CrmWebhookEvento>
        implements CrmWebhookEventoRepositoryCustom {

    @Override
    @Transactional
    public Page<CrmWebhookEvento> paginate(PaginateList paginateList) {
        return super.createSql(paginateList);
    }
}
