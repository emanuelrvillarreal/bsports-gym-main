package com.blue.open.Core.Repository.Impl;

import com.blue.open.Core.Entity.DTO.PaginateList;
import com.blue.open.Core.Entity.Db.UserAccessToken;
import com.blue.open.Core.Repository.Custom.UserAccessTokenRepositoryCustom;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;

public class UserAccessTokenRepositoryImpl extends RepositoryImpl<UserAccessToken> implements UserAccessTokenRepositoryCustom {
    @Override
    @Transactional
    public Page<UserAccessToken> paginate(PaginateList paginateList) {
        return super.createSql(paginateList);
    }
}
