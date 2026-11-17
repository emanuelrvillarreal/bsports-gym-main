package com.blue.open.Code.Repository.Impl;

import com.blue.open.Code.Entity.Db.Configuration;
import com.blue.open.Code.Repository.Custom.ConfigurationRepositoryCustom;
import com.blue.open.Core.Entity.DTO.PaginateList;
import com.blue.open.Core.Repository.Impl.RepositoryImpl;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;

public class ConfigurationRepositoryImpl extends RepositoryImpl<Configuration> implements ConfigurationRepositoryCustom {
    @Override
    @Transactional
    public Page<Configuration> paginate(PaginateList paginateList) {
        return super.createSql(paginateList);
    }
}
