package com.blue.open.Core.Repository.Custom;

import com.blue.open.Core.Entity.Db.SecurityCode;
import com.blue.open.Core.Entity.DTO.PaginateList;
import org.springframework.data.domain.Page;

public interface SecurityCodeRepositoryCustom {
    Page<SecurityCode> paginate(PaginateList paginateList);
}
