package com.blue.open.Code.Repository.Impl;

import com.blue.open.Code.Entity.Db.MercadoPagoCobroItem;
import com.blue.open.Code.Repository.Custom.MercadoPagoCobroItemRepositoryCustom;
import com.blue.open.Core.Entity.DTO.PaginateList;
import com.blue.open.Core.Repository.Impl.RepositoryImpl;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;

public class MercadoPagoCobroItemRepositoryImpl extends RepositoryImpl<MercadoPagoCobroItem>
        implements MercadoPagoCobroItemRepositoryCustom {

    @Override
    @Transactional
    public Page<MercadoPagoCobroItem> paginate(PaginateList paginateList) {
        return super.createSql(paginateList);
    }
}
