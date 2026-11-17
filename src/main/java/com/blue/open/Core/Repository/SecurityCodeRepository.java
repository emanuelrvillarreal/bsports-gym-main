package com.blue.open.Core.Repository;

import com.blue.open.Code.Entity.Db.User;
import com.blue.open.Core.Entity.Db.SecurityCode;
public interface SecurityCodeRepository extends EntityRepository<SecurityCode> {
    SecurityCode findFirstByUserAndStatusOrderByCreatedAtDesc(User user, Integer status);

    SecurityCode findFirstByCodeAndUser(String code, User user);
}
