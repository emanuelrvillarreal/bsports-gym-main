package com.blue.open.Core.Repository.Impl;

import com.blue.open.Core.Entity.DTO.PaginateList;
import com.blue.open.Core.Entity.Db.JwtRefreshToken;
import com.blue.open.Core.Repository.Custom.JwtRefreshTokenRepositoryCustom;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;

public class JwtRefreshTokenRepositoryImpl extends RepositoryImpl<JwtRefreshToken>
        implements JwtRefreshTokenRepositoryCustom {
    @Override
    @Transactional
    public Page<JwtRefreshToken> paginate(PaginateList paginateList) {
        return super.createSql(paginateList);
    }
}
