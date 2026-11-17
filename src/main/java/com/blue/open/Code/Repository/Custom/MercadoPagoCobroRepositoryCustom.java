package com.blue.open.Code.Repository.Custom;

import com.blue.open.Code.Entity.Db.MercadoPagoCobro;
import com.blue.open.Core.Entity.DTO.PaginateList;
import org.springframework.data.domain.Page;

public interface MercadoPagoCobroRepositoryCustom {
    Page<MercadoPagoCobro> paginate(PaginateList paginateList);
}
