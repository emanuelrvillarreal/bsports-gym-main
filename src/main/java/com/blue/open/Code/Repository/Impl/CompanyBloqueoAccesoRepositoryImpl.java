package com.blue.open.Code.Repository.Impl;

import com.blue.open.Code.Entity.Db.CompanyBloqueoAcceso;
import com.blue.open.Code.Repository.Custom.CompanyBloqueoAccesoRepositoryCustom;
import com.blue.open.Core.Entity.DTO.PaginateList;
import com.blue.open.Core.Repository.Impl.RepositoryImpl;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;

public class CompanyBloqueoAccesoRepositoryImpl extends RepositoryImpl<CompanyBloqueoAcceso>
        implements CompanyBloqueoAccesoRepositoryCustom {

    @Override
    @Transactional
    public Page<CompanyBloqueoAcceso> paginate(PaginateList paginateList) {
        return super.createSql(paginateList);
    }
}
