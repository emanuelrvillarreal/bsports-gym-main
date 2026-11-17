package com.blue.open.Core.Repository.Custom;

import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Core.Entity.DTO.PaginateList;
import org.springframework.data.domain.Page;

public interface BaseCompanyRepositoryCustom {
    Page<Company> paginate(PaginateList paginateList);
}
