package com.blue.open.Code.Repository.Impl;

import com.blue.open.Code.Entity.Db.InscripcionActividadDescuento;
import com.blue.open.Code.Repository.Custom.InscripcionActividadDescuentoRepositoryCustom;
import com.blue.open.Core.Entity.DTO.PaginateList;
import com.blue.open.Core.Repository.Impl.RepositoryImpl;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;

public class InscripcionActividadDescuentoRepositoryImpl extends RepositoryImpl<InscripcionActividadDescuento> implements InscripcionActividadDescuentoRepositoryCustom {

    @Override
    @Transactional
    public Page<InscripcionActividadDescuento> paginate(PaginateList paginateList) {
        return super.createSql(paginateList);
    }
}
