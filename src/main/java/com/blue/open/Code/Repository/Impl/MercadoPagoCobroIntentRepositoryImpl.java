package com.blue.open.Code.Repository.Impl;

import com.blue.open.Code.Entity.Db.MercadoPagoCobroIntent;
import com.blue.open.Code.Repository.Custom.MercadoPagoCobroIntentRepositoryCustom;
import com.blue.open.Core.Entity.DTO.PaginateList;
import com.blue.open.Core.Repository.Impl.RepositoryImpl;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;

public class MercadoPagoCobroIntentRepositoryImpl extends RepositoryImpl<MercadoPagoCobroIntent>
        implements MercadoPagoCobroIntentRepositoryCustom {

    @Override
    @Transactional
    public Page<MercadoPagoCobroIntent> paginate(PaginateList paginateList) {
        return super.createSql(paginateList);
    }
}
