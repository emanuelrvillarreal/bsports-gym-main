package com.blue.open.Core.Repository.Impl;

import com.blue.open.Core.Entity.Db.Role;
import com.blue.open.Core.Entity.DTO.PaginateList;
import com.blue.open.Core.Repository.Custom.RoleRepositoryCustom;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Repository;

@Repository
public class RoleRepositoryImpl extends RepositoryImpl<Role> implements RoleRepositoryCustom {
    @Override
    @Transactional
    public Page<Role> paginate(PaginateList paginateList) {
        return super.createSql(paginateList);
    }
}

