package com.blue.open.Code.Repository.Impl;

import com.blue.open.Code.Entity.Db.CuotaSocialRango;
import com.blue.open.Code.Repository.Custom.CuotaSocialRangoRepositoryCustom;
import com.blue.open.Core.Entity.DTO.PaginateList;
import com.blue.open.Core.Repository.Impl.RepositoryImpl;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;

public class CuotaSocialRangoRepositoryImpl extends RepositoryImpl<CuotaSocialRango> implements CuotaSocialRangoRepositoryCustom {

    @Override
    @Transactional
    public Page<CuotaSocialRango> paginate(PaginateList paginateList) {
        return super.createSql(paginateList);
    }
}
