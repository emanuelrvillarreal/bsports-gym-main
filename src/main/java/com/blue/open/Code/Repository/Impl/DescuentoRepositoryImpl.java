package com.blue.open.Code.Repository.Impl;

import com.blue.open.Code.Entity.Db.Descuento;
import com.blue.open.Code.Repository.Custom.DescuentoRepositoryCustom;
import com.blue.open.Core.Entity.DTO.PaginateList;
import com.blue.open.Core.Repository.Impl.RepositoryImpl;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;

public class DescuentoRepositoryImpl extends RepositoryImpl<Descuento> implements DescuentoRepositoryCustom {

    @Override
    @Transactional
    public Page<Descuento> paginate(PaginateList paginateList) {
        return super.createSql(paginateList);
    }
}
