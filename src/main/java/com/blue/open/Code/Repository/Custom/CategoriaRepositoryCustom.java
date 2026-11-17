package com.blue.open.Code.Repository.Custom;

import com.blue.open.Code.Entity.Db.Categoria;
import com.blue.open.Core.Entity.DTO.PaginateList;
import org.springframework.data.domain.Page;

public interface CategoriaRepositoryCustom {

    Page<Categoria> paginate(PaginateList paginateList);
}
