package com.blue.open.Code.Repository.Impl;

import com.blue.open.Code.Entity.Db.AccesoClub;
import com.blue.open.Code.Repository.Custom.AccesoClubRepositoryCustom;
import com.blue.open.Core.Entity.DTO.PaginateList;
import com.blue.open.Core.Repository.Impl.RepositoryImpl;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;

public class AccesoClubRepositoryImpl extends RepositoryImpl<AccesoClub> implements AccesoClubRepositoryCustom {

    @Override
    @Transactional
    public Page<AccesoClub> paginate(PaginateList paginateList) {
        return super.createSql(paginateList);
    }
}
