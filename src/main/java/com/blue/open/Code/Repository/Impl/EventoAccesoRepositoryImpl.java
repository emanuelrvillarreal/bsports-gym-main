package com.blue.open.Code.Repository.Impl;

import com.blue.open.Code.Entity.Db.EventoAcceso;
import com.blue.open.Code.Repository.Custom.EventoAccesoRepositoryCustom;
import com.blue.open.Core.Entity.DTO.PaginateList;
import com.blue.open.Core.Repository.Impl.RepositoryImpl;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;

public class EventoAccesoRepositoryImpl extends RepositoryImpl<EventoAcceso> implements EventoAccesoRepositoryCustom {

    @Override
    @Transactional
    public Page<EventoAcceso> paginate(PaginateList paginateList) {
        return super.createSql(paginateList);
    }
}
