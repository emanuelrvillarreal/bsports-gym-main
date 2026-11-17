package com.blue.open.Code.Repository.Impl;

import com.blue.open.Code.Entity.Db.CompanySuscripcionCrm;
import com.blue.open.Code.Repository.Custom.CompanySuscripcionCrmRepositoryCustom;
import com.blue.open.Core.Entity.DTO.PaginateList;
import com.blue.open.Core.Repository.Impl.RepositoryImpl;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;

public class CompanySuscripcionCrmRepositoryImpl extends RepositoryImpl<CompanySuscripcionCrm>
        implements CompanySuscripcionCrmRepositoryCustom {

    @Override
    @Transactional
    public Page<CompanySuscripcionCrm> paginate(PaginateList paginateList) {
        return super.createSql(paginateList);
    }
}
