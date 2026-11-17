package com.blue.open.Code.Repository.Custom;

import com.blue.open.Code.Entity.Db.EventoActividad;
import com.blue.open.Core.Entity.DTO.PaginateList;
import org.springframework.data.domain.Page;

public interface EventoActividadRepositoryCustom {

    Page<EventoActividad> paginate(PaginateList paginateList);
}
