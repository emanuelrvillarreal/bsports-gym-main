package com.blue.open.Crm.Repository.Impl;

import com.blue.open.Core.Entity.DTO.PaginateList;
import com.blue.open.Core.Repository.Impl.RepositoryImpl;
import com.blue.open.Crm.Entity.Db.CrmIntegracionLog;
import com.blue.open.Crm.Repository.Custom.CrmIntegracionLogRepositoryCustom;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;

/**
 * Implementacion de las consultas propias de CrmIntegracionLogRepository.
 * Delega la paginacion en RepositoryImpl del Core, igual que el resto de los repositorios.
 */
public class CrmIntegracionLogRepositoryImpl extends RepositoryImpl<CrmIntegracionLog>
        implements CrmIntegracionLogRepositoryCustom {

    @Override
    @Transactional
    public Page<CrmIntegracionLog> paginate(PaginateList paginateList) {
        return super.createSql(paginateList);
    }
}
