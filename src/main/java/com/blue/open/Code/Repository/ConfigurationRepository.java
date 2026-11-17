package com.blue.open.Code.Repository;

import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.Configuration;
import com.blue.open.Code.Repository.Custom.ConfigurationRepositoryCustom;
import com.blue.open.Core.Repository.EntityRepository;

import java.util.List;

public interface ConfigurationRepository extends EntityRepository<Configuration>, ConfigurationRepositoryCustom {
    List<Configuration> findByGlobal(boolean publico);
    List<Configuration> findByNameIn(List<String> llaves);
    Configuration findFirstByName(String llave);
    Configuration findFirstByNameAndCompany(String llave, Company company);
    List<Configuration> findByNameInAndCompany(List<String> llaves, Company company);
    List<Configuration> findByCompany(Company company);
}
