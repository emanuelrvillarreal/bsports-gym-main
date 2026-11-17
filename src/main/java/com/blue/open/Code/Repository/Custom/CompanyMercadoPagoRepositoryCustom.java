package com.blue.open.Code.Repository.Custom;

import com.blue.open.Code.Entity.Db.CompanyMercadoPago;
import com.blue.open.Core.Entity.DTO.PaginateList;
import org.springframework.data.domain.Page;

public interface CompanyMercadoPagoRepositoryCustom {
    Page<CompanyMercadoPago> paginate(PaginateList paginateList);
}
