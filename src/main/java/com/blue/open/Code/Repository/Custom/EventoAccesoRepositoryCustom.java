package com.blue.open.Code.Repository.Custom;

import com.blue.open.Code.Entity.Db.EventoAcceso;
import com.blue.open.Core.Entity.DTO.PaginateList;
import org.springframework.data.domain.Page;

public interface EventoAccesoRepositoryCustom {

    Page<EventoAcceso> paginate(PaginateList paginateList);
}
