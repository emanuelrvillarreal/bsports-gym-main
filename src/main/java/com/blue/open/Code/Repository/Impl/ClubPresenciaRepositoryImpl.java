package com.blue.open.Code.Repository.Impl;

import com.blue.open.Code.Entity.Db.ClubPresencia;
import com.blue.open.Code.Repository.Custom.ClubPresenciaRepositoryCustom;
import com.blue.open.Core.Entity.DTO.PaginateList;
import com.blue.open.Core.Repository.Impl.RepositoryImpl;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;

public class ClubPresenciaRepositoryImpl extends RepositoryImpl<ClubPresencia>
        implements ClubPresenciaRepositoryCustom {

    @Override
    @Transactional
    public Page<ClubPresencia> paginate(PaginateList paginateList) {
        return super.createSql(paginateList);
    }
}
