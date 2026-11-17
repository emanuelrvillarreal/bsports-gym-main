package com.blue.open.Core.Repository.Custom;

import com.blue.open.Core.Entity.Db.UserCompanyRole;
import com.blue.open.Core.Entity.DTO.PaginateList;
import org.springframework.data.domain.Page;

public interface UserCompanyRoleRepositoryCustom {
    Page<UserCompanyRole> paginate(PaginateList paginateList);
}
