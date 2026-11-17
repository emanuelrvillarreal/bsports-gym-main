package com.blue.open.Code.Repository.Custom;

import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Core.Entity.DTO.PaginateList;
import org.springframework.data.domain.Page;

public interface CompanyRepositoryCustom {
    Page<Company> paginate(PaginateList paginateList);
}
