package com.blue.open.Code.Repository.Custom;

import com.blue.open.Code.Entity.Db.Reserva;
import com.blue.open.Core.Entity.DTO.PaginateList;
import org.springframework.data.domain.Page;

public interface ReservaRepositoryCustom {

    Page<Reserva> paginate(PaginateList paginateList);
}
