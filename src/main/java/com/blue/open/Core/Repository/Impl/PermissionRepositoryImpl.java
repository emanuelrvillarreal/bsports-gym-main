package com.blue.open.Core.Repository.Impl;

import com.blue.open.Core.Entity.Db.Permission;
import com.blue.open.Core.Entity.DTO.PaginateList;
import com.blue.open.Core.Repository.Custom.PermissionRepositoryCustom;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Repository;

@Repository
public class PermissionRepositoryImpl extends RepositoryImpl<Permission> implements PermissionRepositoryCustom {
    @Override
    @Transactional
    public Page<Permission> paginate(PaginateList paginateList) {
        return super.createSql(paginateList);
    }
}

