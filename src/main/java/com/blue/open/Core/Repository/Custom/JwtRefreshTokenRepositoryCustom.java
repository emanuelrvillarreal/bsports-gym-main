package com.blue.open.Core.Repository.Custom;

import com.blue.open.Core.Entity.DTO.PaginateList;
import com.blue.open.Core.Entity.Db.JwtRefreshToken;
import org.springframework.data.domain.Page;

public interface JwtRefreshTokenRepositoryCustom {
    Page<JwtRefreshToken> paginate(PaginateList paginateList);
}
