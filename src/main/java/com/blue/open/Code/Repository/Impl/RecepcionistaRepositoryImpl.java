package com.blue.open.Code.Repository.Impl;

import com.blue.open.Code.Entity.Db.Recepcionista;
import com.blue.open.Code.Repository.Custom.RecepcionistaRepositoryCustom;
import com.blue.open.Core.Entity.DTO.PaginateList;
import com.blue.open.Core.Repository.Impl.RepositoryImpl;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;

public class RecepcionistaRepositoryImpl extends RepositoryImpl<Recepcionista> implements RecepcionistaRepositoryCustom {

    @Override
    @Transactional
    public Page<Recepcionista> paginate(PaginateList paginateList) {
        return super.createSql(paginateList);
    }
}
