package com.blue.open.Code.Repository.Custom;

import com.blue.open.Code.Entity.Db.AsistenciaActividad;
import com.blue.open.Core.Entity.DTO.PaginateList;
import org.springframework.data.domain.Page;

public interface AsistenciaActividadRepositoryCustom {

    Page<AsistenciaActividad> paginate(PaginateList paginateList);
}
