package com.blue.open.Core.Repository;

import com.blue.open.Code.Entity.Db.User;
import com.blue.open.Core.Repository.Custom.UserRepositoryCustom;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends EntityRepository<User>, UserRepositoryCustom<User> {
    User findFirstByUsernameIgnoringCase(String username);

    User findFirstByUsernameAndStatus(String username, Integer status);

    User findFirstByEmailAndStatus(String email, Integer status);

    User findFirstByMalvinasLegajoAndStatus(String legajo, Integer status);

    User findFirstByEmailAndStatusNot(String email, Integer status);

    User findFirstBySsoUuidAndStatus(String ssoUuid, Integer status);

    User findFirstByDni(String dni);

    User findFirstByDniAndUsernameIgnoreCase(String dni, String username);

    User findFirstByDniAndStatusNot(String dni, Integer status);
}
