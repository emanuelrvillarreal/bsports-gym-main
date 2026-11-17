package com.blue.open.Code.Repository.Impl;

import com.blue.open.Code.Entity.Db.CodigoLogin;
import com.blue.open.Code.Repository.Custom.CodigoLoginRepositoryCustom;
import com.blue.open.Core.Entity.DTO.PaginateList;
import com.blue.open.Core.Repository.Impl.RepositoryImpl;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;

public class CodigoLoginRepositoryImpl extends RepositoryImpl<CodigoLogin>
        implements CodigoLoginRepositoryCustom {

    @Override
    @Transactional
    public Page<CodigoLogin> paginate(PaginateList paginateList) {
        return super.createSql(paginateList);
    }
}
