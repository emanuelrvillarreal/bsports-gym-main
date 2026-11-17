package com.blue.open.Code.Repository.Custom;

import com.blue.open.Code.Entity.Db.Amenity;
import com.blue.open.Core.Entity.DTO.PaginateList;
import org.springframework.data.domain.Page;

public interface AmenityRepositoryCustom {

    Page<Amenity> paginate(PaginateList paginateList);
}
