package com.blue.open.Code.Repository.Impl;

import com.blue.open.Code.Entity.Db.Profesor;
import com.blue.open.Code.Repository.Custom.ProfesorRepositoryCustom;
import com.blue.open.Core.Entity.DTO.PaginateList;
import com.blue.open.Core.Repository.Impl.RepositoryImpl;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;

public class ProfesorRepositoryImpl extends RepositoryImpl<Profesor> implements ProfesorRepositoryCustom {

    @Override
    @Transactional
    public Page<Profesor> paginate(PaginateList paginateList) {
        return super.createSql(paginateList);
    }
}
