package com.blue.open.Code.Repository;

import com.blue.open.Code.Entity.Db.ProductoCrm;
import com.blue.open.Code.Repository.Custom.ProductoCrmRepositoryCustom;
import com.blue.open.Core.Repository.EntityRepository;

import java.util.Optional;

public interface ProductoCrmRepository extends EntityRepository<ProductoCrm>, ProductoCrmRepositoryCustom {

    Optional<ProductoCrm> findFirstByCodigoAndStatus(String codigo, int status);
}
