package com.blue.open.Code.Repository.Custom;

import com.blue.open.Code.Entity.Db.Movimiento;
import com.blue.open.Core.Entity.DTO.PaginateList;
import org.springframework.data.domain.Page;

public interface MovimientoRepositoryCustom {

    Page<Movimiento> paginate(PaginateList paginateList);
}
