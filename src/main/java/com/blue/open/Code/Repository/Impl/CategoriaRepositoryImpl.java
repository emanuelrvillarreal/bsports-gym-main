package com.blue.open.Code.Repository.Impl;

import com.blue.open.Code.Entity.Db.Categoria;
import com.blue.open.Code.Repository.Custom.CategoriaRepositoryCustom;
import com.blue.open.Core.Entity.DTO.PaginateList;
import com.blue.open.Core.Repository.Impl.RepositoryImpl;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;

public class CategoriaRepositoryImpl extends RepositoryImpl<Categoria> implements CategoriaRepositoryCustom {

    @Override
    @Transactional
    public Page<Categoria> paginate(PaginateList paginateList) {
        return super.createSql(paginateList);
    }
}
