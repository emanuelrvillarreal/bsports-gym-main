package com.blue.open.Core.Repository.Impl;

import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Core.Entity.DTO.PaginateList;
import com.blue.open.Core.Repository.Custom.BaseCompanyRepositoryCustom;

import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;

public class BaseCompanyRepositoryImpl extends RepositoryImpl<Company> implements BaseCompanyRepositoryCustom {
    @Override
    @Transactional
    public Page<Company> paginate(PaginateList paginateList) {
        return super.createSql(paginateList);
    }
}
