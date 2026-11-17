package com.blue.open.Code.Repository.Impl;

import com.blue.open.Code.Entity.Db.CompanyMercadoPago;
import com.blue.open.Code.Repository.Custom.CompanyMercadoPagoRepositoryCustom;
import com.blue.open.Core.Entity.DTO.PaginateList;
import com.blue.open.Core.Repository.Impl.RepositoryImpl;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;

public class CompanyMercadoPagoRepositoryImpl extends RepositoryImpl<CompanyMercadoPago>
        implements CompanyMercadoPagoRepositoryCustom {

    @Override
    @Transactional
    public Page<CompanyMercadoPago> paginate(PaginateList paginateList) {
        return super.createSql(paginateList);
    }
}
