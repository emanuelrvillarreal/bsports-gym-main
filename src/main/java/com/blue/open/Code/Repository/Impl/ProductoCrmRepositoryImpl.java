package com.blue.open.Code.Repository.Impl;

import com.blue.open.Code.Entity.Db.ProductoCrm;
import com.blue.open.Code.Repository.Custom.ProductoCrmRepositoryCustom;
import com.blue.open.Core.Entity.DTO.PaginateList;
import com.blue.open.Core.Repository.Impl.RepositoryImpl;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;

public class ProductoCrmRepositoryImpl extends RepositoryImpl<ProductoCrm> implements ProductoCrmRepositoryCustom {

    @Override
    @Transactional
    public Page<ProductoCrm> paginate(PaginateList paginateList) {
        return super.createSql(paginateList);
    }
}
