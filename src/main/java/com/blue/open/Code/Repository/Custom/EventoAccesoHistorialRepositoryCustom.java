package com.blue.open.Code.Repository.Custom;

import com.blue.open.Code.Entity.Db.EventoAccesoHistorial;
import com.blue.open.Core.Entity.DTO.PaginateList;
import org.springframework.data.domain.Page;

public interface EventoAccesoHistorialRepositoryCustom {

    Page<EventoAccesoHistorial> paginate(PaginateList paginateList);
}
