package com.blue.open.Code.Repository.Custom;

import com.blue.open.Code.Entity.Db.Profesor;
import com.blue.open.Core.Entity.DTO.PaginateList;
import org.springframework.data.domain.Page;

public interface ProfesorRepositoryCustom {

    Page<Profesor> paginate(PaginateList paginateList);
}
