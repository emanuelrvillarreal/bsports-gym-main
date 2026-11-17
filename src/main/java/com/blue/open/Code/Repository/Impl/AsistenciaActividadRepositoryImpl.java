package com.blue.open.Code.Repository.Impl;

import com.blue.open.Code.Entity.Db.AsistenciaActividad;
import com.blue.open.Code.Repository.Custom.AsistenciaActividadRepositoryCustom;
import com.blue.open.Core.Entity.DTO.PaginateList;
import com.blue.open.Core.Repository.Impl.RepositoryImpl;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;

public class AsistenciaActividadRepositoryImpl extends RepositoryImpl<AsistenciaActividad>
        implements AsistenciaActividadRepositoryCustom {

    @Override
    @Transactional
    public Page<AsistenciaActividad> paginate(PaginateList paginateList) {
        return super.createSql(paginateList);
    }
}
