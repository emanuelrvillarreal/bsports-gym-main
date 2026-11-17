package com.blue.open.Code.Repository.Custom;

import com.blue.open.Code.Entity.Db.Actividad;
import com.blue.open.Core.Entity.DTO.PaginateList;
import org.springframework.data.domain.Page;

public interface ActividadRepositoryCustom {

    Page<Actividad> paginate(PaginateList paginateList);
}
