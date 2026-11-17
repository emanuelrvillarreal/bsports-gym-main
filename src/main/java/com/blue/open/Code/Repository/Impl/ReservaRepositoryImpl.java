package com.blue.open.Code.Repository.Impl;

import com.blue.open.Code.Entity.Db.Reserva;
import com.blue.open.Code.Repository.Custom.ReservaRepositoryCustom;
import com.blue.open.Core.Entity.DTO.PaginateList;
import com.blue.open.Core.Repository.Impl.RepositoryImpl;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;

public class ReservaRepositoryImpl extends RepositoryImpl<Reserva> implements ReservaRepositoryCustom {

    @Override
    @Transactional
    public Page<Reserva> paginate(PaginateList paginateList) {
        return super.createSql(paginateList);
    }
}
