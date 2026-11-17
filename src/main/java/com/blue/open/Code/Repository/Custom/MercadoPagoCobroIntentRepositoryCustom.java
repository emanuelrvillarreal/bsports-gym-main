package com.blue.open.Code.Repository.Custom;

import com.blue.open.Code.Entity.Db.MercadoPagoCobroIntent;
import com.blue.open.Core.Entity.DTO.PaginateList;
import org.springframework.data.domain.Page;

public interface MercadoPagoCobroIntentRepositoryCustom {
    Page<MercadoPagoCobroIntent> paginate(PaginateList paginateList);
}
