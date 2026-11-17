package com.blue.open.Code.Repository.Custom;

import com.blue.open.Code.Entity.Db.InscripcionActividadDescuento;
import com.blue.open.Core.Entity.DTO.PaginateList;
import org.springframework.data.domain.Page;

public interface InscripcionActividadDescuentoRepositoryCustom {

    Page<InscripcionActividadDescuento> paginate(PaginateList paginateList);
}
