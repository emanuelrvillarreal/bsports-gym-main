package com.blue.open.Code.Repository.Impl;

import com.blue.open.Code.Entity.Db.Movimiento;
import com.blue.open.Code.Repository.Custom.MovimientoRepositoryCustom;
import com.blue.open.Core.Entity.DTO.PaginateList;
import com.blue.open.Core.Repository.Impl.RepositoryImpl;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;

public class MovimientoRepositoryImpl extends RepositoryImpl<Movimiento> implements MovimientoRepositoryCustom {

    @Override
    @Transactional
    public Page<Movimiento> paginate(PaginateList paginateList) {
        return super.createSql(paginateList);
    }
}
