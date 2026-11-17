package com.blue.open.Code.Repository.Custom;

import com.blue.open.Code.Entity.Db.AccesoClub;
import com.blue.open.Core.Entity.DTO.PaginateList;
import org.springframework.data.domain.Page;

public interface AccesoClubRepositoryCustom {

    Page<AccesoClub> paginate(PaginateList paginateList);
}
