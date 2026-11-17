package com.blue.open.Core.Repository;

import com.blue.open.Code.Entity.Db.Company;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface BaseCompanyRepository extends EntityRepository<Company> {
    Company findFirstByNameAndStatus(String name, Integer status);

    Company findFirstByCuit(String cuit);

    Company findFirstByPath(String path);
}