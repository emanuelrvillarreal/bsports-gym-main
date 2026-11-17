package com.blue.open.Code.Repository.Impl;

import com.blue.open.Code.Entity.Db.Amenity;
import com.blue.open.Code.Repository.Custom.AmenityRepositoryCustom;
import com.blue.open.Core.Entity.DTO.PaginateList;
import com.blue.open.Core.Repository.Impl.RepositoryImpl;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;

public class AmenityRepositoryImpl extends RepositoryImpl<Amenity> implements AmenityRepositoryCustom {

    @Override
    @Transactional
    public Page<Amenity> paginate(PaginateList paginateList) {
        return super.createSql(paginateList);
    }
}
