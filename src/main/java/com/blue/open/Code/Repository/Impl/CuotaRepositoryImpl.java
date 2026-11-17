package com.blue.open.Code.Repository.Impl;

import com.blue.open.Code.Entity.Db.Cuota;
import com.blue.open.Code.Repository.Custom.CuotaRepositoryCustom;
import com.blue.open.Core.Entity.DTO.PaginateList;
import com.blue.open.Core.Repository.Impl.RepositoryImpl;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;

public class CuotaRepositoryImpl extends RepositoryImpl<Cuota> implements CuotaRepositoryCustom {

    @Override
    @Transactional
    public Page<Cuota> paginate(PaginateList paginateList) {
        return super.createSql(paginateList);
    }
}
