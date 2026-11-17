package com.blue.open.Code.Repository.Custom;

import com.blue.open.Code.Entity.Db.MemberDescuento;
import com.blue.open.Core.Entity.DTO.PaginateList;
import org.springframework.data.domain.Page;

public interface MemberDescuentoRepositoryCustom {

    Page<MemberDescuento> paginate(PaginateList paginateList);
}
