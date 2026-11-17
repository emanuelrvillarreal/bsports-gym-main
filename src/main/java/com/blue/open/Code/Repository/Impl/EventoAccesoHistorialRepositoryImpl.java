package com.blue.open.Code.Repository.Impl;

import com.blue.open.Code.Entity.Db.EventoAccesoHistorial;
import com.blue.open.Code.Repository.Custom.EventoAccesoHistorialRepositoryCustom;
import com.blue.open.Core.Entity.DTO.PaginateList;
import com.blue.open.Core.Repository.Impl.RepositoryImpl;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;

public class EventoAccesoHistorialRepositoryImpl extends RepositoryImpl<EventoAccesoHistorial> implements EventoAccesoHistorialRepositoryCustom {

    @Override
    @Transactional
    public Page<EventoAccesoHistorial> paginate(PaginateList paginateList) {
        return super.createSql(paginateList);
    }
}
