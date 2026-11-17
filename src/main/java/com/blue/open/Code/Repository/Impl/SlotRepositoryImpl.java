package com.blue.open.Code.Repository.Impl;

import com.blue.open.Code.Entity.Db.Slot;
import com.blue.open.Code.Repository.Custom.SlotRepositoryCustom;
import com.blue.open.Core.Entity.DTO.PaginateList;
import com.blue.open.Core.Repository.Impl.RepositoryImpl;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;

public class SlotRepositoryImpl extends RepositoryImpl<Slot> implements SlotRepositoryCustom {

    @Override
    @Transactional
    public Page<Slot> paginate(PaginateList paginateList) {
        return super.createSql(paginateList);
    }
}
