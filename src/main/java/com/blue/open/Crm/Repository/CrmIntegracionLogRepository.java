package com.blue.open.Crm.Repository;

import com.blue.open.Core.Repository.EntityRepository;
import com.blue.open.Crm.Entity.Db.CrmIntegracionLog;
import com.blue.open.Crm.Repository.Custom.CrmIntegracionLogRepositoryCustom;

/**
 * Persistencia del log de interacciones con el CRM (blue-crm).
 * Lo usa CrmIntegracionLogService. Solo escribe: la lectura es por consulta directa a la base.
 */
public interface CrmIntegracionLogRepository
        extends EntityRepository<CrmIntegracionLog>, CrmIntegracionLogRepositoryCustom {
}
