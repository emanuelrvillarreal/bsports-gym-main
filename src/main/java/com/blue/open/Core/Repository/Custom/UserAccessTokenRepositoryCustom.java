package com.blue.open.Core.Repository.Custom;

import com.blue.open.Core.Entity.DTO.PaginateList;
import com.blue.open.Core.Entity.Db.UserAccessToken;
import org.springframework.data.domain.Page;

public interface UserAccessTokenRepositoryCustom {
    Page<UserAccessToken> paginate(PaginateList paginateList);
}
