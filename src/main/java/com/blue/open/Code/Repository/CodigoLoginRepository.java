package com.blue.open.Code.Repository;

import com.blue.open.Code.Entity.Db.CodigoLogin;
import com.blue.open.Code.Entity.Db.User;
import com.blue.open.Code.Repository.Custom.CodigoLoginRepositoryCustom;
import com.blue.open.Core.Repository.EntityRepository;

public interface CodigoLoginRepository extends EntityRepository<CodigoLogin>, CodigoLoginRepositoryCustom {

    CodigoLogin findFirstByUserAndStatusOrderByDateCreatedDesc(User user, Integer status);

    CodigoLogin findFirstByCodigoHashAndStatusAndUserStatus(String codigoHash, Integer status, Integer userStatus);
}
