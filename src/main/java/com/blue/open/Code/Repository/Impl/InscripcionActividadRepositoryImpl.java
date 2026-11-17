package com.blue.open.Code.Repository.Impl;

import com.blue.open.Code.Entity.Db.InscripcionActividad;
import com.blue.open.Code.Repository.Custom.InscripcionActividadRepositoryCustom;
import com.blue.open.Core.Entity.DTO.PaginateList;
import com.blue.open.Core.Repository.Impl.RepositoryImpl;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;

public class InscripcionActividadRepositoryImpl extends RepositoryImpl<InscripcionActividad>
        implements InscripcionActividadRepositoryCustom {

    @Override
    @Transactional
    public Page<InscripcionActividad> paginate(PaginateList paginateList) {
        return super.createSql(paginateList);
    }
}
