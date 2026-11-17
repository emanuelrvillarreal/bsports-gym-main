package com.blue.open.Core.Repository.Custom;

import com.blue.open.Core.Entity.Db.Role;
import com.blue.open.Core.Entity.DTO.PaginateList;
import org.springframework.data.domain.Page;

public interface RoleRepositoryCustom {
    Page<Role> paginate(PaginateList paginateList);
}

