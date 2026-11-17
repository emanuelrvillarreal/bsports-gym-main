package com.blue.open.Code.Repository.Impl;

import com.blue.open.Code.Entity.Db.Actividad;
import com.blue.open.Code.Repository.Custom.ActividadRepositoryCustom;
import com.blue.open.Core.Entity.DTO.PaginateList;
import com.blue.open.Core.Repository.Impl.RepositoryImpl;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;

public class ActividadRepositoryImpl extends RepositoryImpl<Actividad> implements ActividadRepositoryCustom {

    @Override
    @Transactional
    public Page<Actividad> paginate(PaginateList paginateList) {
        return super.createSql(paginateList);
    }
}
