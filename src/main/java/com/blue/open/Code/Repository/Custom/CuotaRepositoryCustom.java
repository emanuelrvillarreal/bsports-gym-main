package com.blue.open.Code.Repository.Custom;

import com.blue.open.Code.Entity.Db.Cuota;
import com.blue.open.Core.Entity.DTO.PaginateList;
import org.springframework.data.domain.Page;

public interface CuotaRepositoryCustom {

    Page<Cuota> paginate(PaginateList paginateList);
}
