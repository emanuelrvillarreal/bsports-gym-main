package com.blue.open.Code.Repository.Impl;

import com.blue.open.Code.Entity.Db.MemberDescuento;
import com.blue.open.Code.Repository.Custom.MemberDescuentoRepositoryCustom;
import com.blue.open.Core.Entity.DTO.PaginateList;
import com.blue.open.Core.Repository.Impl.RepositoryImpl;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;

public class MemberDescuentoRepositoryImpl extends RepositoryImpl<MemberDescuento> implements MemberDescuentoRepositoryCustom {

    @Override
    @Transactional
    public Page<MemberDescuento> paginate(PaginateList paginateList) {
        return super.createSql(paginateList);
    }
}
