package com.blue.open.Core.Repository.Custom;

import com.blue.open.Core.Entity.DTO.PaginateList;
import org.springframework.data.domain.Page;

public interface PaginateAutomatico<T> {
    Page<T> paginate(PaginateList paginateList);
}
