package com.blue.open.Code.Repository.Impl;

import com.blue.open.Code.Entity.Db.EventoActividad;
import com.blue.open.Code.Repository.Custom.EventoActividadRepositoryCustom;
import com.blue.open.Core.Entity.DTO.PaginateList;
import com.blue.open.Core.Repository.Impl.RepositoryImpl;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;

public class EventoActividadRepositoryImpl extends RepositoryImpl<EventoActividad>
        implements EventoActividadRepositoryCustom {

    @Override
    @Transactional
    public Page<EventoActividad> paginate(PaginateList paginateList) {
        return super.createSql(paginateList);
    }
}
