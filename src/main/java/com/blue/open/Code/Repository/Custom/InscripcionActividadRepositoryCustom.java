package com.blue.open.Code.Repository.Custom;

import com.blue.open.Code.Entity.Db.InscripcionActividad;
import com.blue.open.Core.Entity.DTO.PaginateList;
import org.springframework.data.domain.Page;

public interface InscripcionActividadRepositoryCustom {

    Page<InscripcionActividad> paginate(PaginateList paginateList);
}
