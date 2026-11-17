package com.blue.open.Core.Repository.Impl;

import com.blue.open.Core.Entity.Db.SecurityCode;
import com.blue.open.Core.Entity.DTO.PaginateList;
import com.blue.open.Core.Repository.Custom.SecurityCodeRepositoryCustom;

import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;

public class SecurityCodeRepositoryImpl extends RepositoryImpl<SecurityCode> implements SecurityCodeRepositoryCustom {
    @Override
    @Transactional
    public Page<SecurityCode> paginate(PaginateList paginateList) {
        return super.createSql(paginateList);
    }
}
