package com.blue.open.Core.Repository;

import com.blue.open.Code.Entity.Db.User;
import com.blue.open.Core.Entity.Db.UserAccessToken;
import java.util.List;

public interface UserAccessTokenRepository extends EntityRepository<UserAccessToken> {
    UserAccessToken findFirstByCode(String code);

    UserAccessToken findFirstByNameAndUserAndStatus(String name, User user, Integer status);

    List<UserAccessToken> findAllByUserAndStatus(User user, Integer status);
}
