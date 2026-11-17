package com.blue.open.Code.Repository.Custom;

import com.blue.open.Code.Entity.Db.Slot;
import com.blue.open.Core.Entity.DTO.PaginateList;
import org.springframework.data.domain.Page;

public interface SlotRepositoryCustom {

    Page<Slot> paginate(PaginateList paginateList);
}
