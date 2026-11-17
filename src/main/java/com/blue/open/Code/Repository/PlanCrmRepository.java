package com.blue.open.Code.Repository;

import com.blue.open.Code.Entity.Db.PlanCrm;
import com.blue.open.Code.Repository.Custom.PlanCrmRepositoryCustom;
import com.blue.open.Core.Repository.EntityRepository;

import java.util.Optional;

public interface PlanCrmRepository extends EntityRepository<PlanCrm>, PlanCrmRepositoryCustom {

    Optional<PlanCrm> findFirstByCodigoAndStatus(String codigo, int status);
}
