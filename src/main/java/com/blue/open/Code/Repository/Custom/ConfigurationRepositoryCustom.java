package com.blue.open.Code.Repository.Custom;

import com.blue.open.Code.Entity.Db.Configuration;
import com.blue.open.Core.Entity.DTO.PaginateList;
import org.springframework.data.domain.Page;

public interface ConfigurationRepositoryCustom {
    Page<Configuration> paginate(PaginateList paginateList);
}
