package com.blue.open.Code.Repository.Impl;

import com.blue.open.Code.Entity.Db.MercadoPagoCobro;
import com.blue.open.Code.Repository.Custom.MercadoPagoCobroRepositoryCustom;
import com.blue.open.Core.Entity.DTO.PaginateList;
import com.blue.open.Core.Repository.Impl.RepositoryImpl;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;

public class MercadoPagoCobroRepositoryImpl extends RepositoryImpl<MercadoPagoCobro>
        implements MercadoPagoCobroRepositoryCustom {

    @Override
    @Transactional
    public Page<MercadoPagoCobro> paginate(PaginateList paginateList) {
        return super.createSql(paginateList);
    }
}
