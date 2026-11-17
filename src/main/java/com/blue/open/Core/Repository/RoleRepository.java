package com.blue.open.Core.Repository;

import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Core.Entity.Db.Role;
import com.blue.open.Core.Repository.Custom.RoleRepositoryCustom;

import java.util.List;

import org.springframework.stereotype.Repository;

@Repository
public interface RoleRepository extends EntityRepository<Role>, RoleRepositoryCustom {
    Role findFirstByName(String name);

    Role findFirstByNameAndCompanyIsNull(String name);

    Role findFirstByNameAndCompany(String name, Company company);

    Role findFirstByCompanyAndIsDefault(Company company, Integer isDefault);

    List<Role> findAllByCompanyAndIsDefault(Company company, Integer isDefault);
}
