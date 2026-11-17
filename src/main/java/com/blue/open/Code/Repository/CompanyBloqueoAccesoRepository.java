package com.blue.open.Code.Repository;

import com.blue.open.Code.Entity.Db.CompanyBloqueoAcceso;
import com.blue.open.Code.Repository.Custom.CompanyBloqueoAccesoRepositoryCustom;
import com.blue.open.Core.Repository.EntityRepository;

import java.util.Optional;

public interface CompanyBloqueoAccesoRepository extends EntityRepository<CompanyBloqueoAcceso>,
        CompanyBloqueoAccesoRepositoryCustom {

    Optional<CompanyBloqueoAcceso> findFirstByCompanyIdAndStatus(Long companyId, int status);
}
