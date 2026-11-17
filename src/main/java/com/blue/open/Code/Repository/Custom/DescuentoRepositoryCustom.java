package com.blue.open.Code.Repository.Custom;

import com.blue.open.Code.Entity.Db.Descuento;
import com.blue.open.Core.Entity.DTO.PaginateList;
import org.springframework.data.domain.Page;

public interface DescuentoRepositoryCustom {

    Page<Descuento> paginate(PaginateList paginateList);
}
