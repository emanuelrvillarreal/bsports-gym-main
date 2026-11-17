package com.blue.open.Code.Repository.Impl;

import com.blue.open.Code.Entity.Db.MercadoPagoTraza;
import com.blue.open.Code.Repository.Custom.MercadoPagoTrazaRepositoryCustom;
import com.blue.open.Core.Entity.DTO.PaginateList;
import com.blue.open.Core.Repository.Impl.RepositoryImpl;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;

public class MercadoPagoTrazaRepositoryImpl extends RepositoryImpl<MercadoPagoTraza>
        implements MercadoPagoTrazaRepositoryCustom {

    @Override
    @Transactional
    public Page<MercadoPagoTraza> paginate(PaginateList paginateList) {
        return super.createSql(paginateList);
    }
}
