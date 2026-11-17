package com.blue.open.Code.Repository.Custom;

import com.blue.open.Code.Entity.Db.ClubPresencia;
import com.blue.open.Core.Entity.DTO.PaginateList;
import org.springframework.data.domain.Page;

public interface ClubPresenciaRepositoryCustom {

    Page<ClubPresencia> paginate(PaginateList paginateList);
}
