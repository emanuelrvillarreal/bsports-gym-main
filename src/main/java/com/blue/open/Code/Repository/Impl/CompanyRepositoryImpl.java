package com.blue.open.Code.Repository.Impl;

import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Repository.Custom.CompanyRepositoryCustom;
import com.blue.open.Core.Entity.DTO.PaginateList;
import com.blue.open.Core.Repository.Impl.RepositoryImpl;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;

public class CompanyRepositoryImpl extends RepositoryImpl<Company> implements CompanyRepositoryCustom {
    @Override
    @Transactional
    public Page<Company> paginate(PaginateList paginateList) {
        return super.createSql(paginateList);
    }
}
