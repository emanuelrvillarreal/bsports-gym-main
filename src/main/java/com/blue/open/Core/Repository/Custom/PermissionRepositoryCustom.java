package com.blue.open.Core.Repository.Custom;

import com.blue.open.Core.Entity.Db.Permission;
import com.blue.open.Core.Entity.DTO.PaginateList;
import org.springframework.data.domain.Page;

public interface PermissionRepositoryCustom {
    Page<Permission> paginate(PaginateList paginateList);
}
