package com.blue.open.Core.Repository.Impl;

import com.blue.open.Core.Entity.Db.UserCompanyRole;
import com.blue.open.Core.Repository.Custom.UserCompanyRoleRepositoryCustom;
import com.blue.open.Core.Entity.DTO.PaginateList;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;

public class UserCompanyRoleRepositoryImpl extends RepositoryImpl<UserCompanyRole> implements UserCompanyRoleRepositoryCustom {
    @Override
    @Transactional
    public Page<UserCompanyRole> paginate(PaginateList paginateList) {
        return super.createSql(paginateList);
    }
}
