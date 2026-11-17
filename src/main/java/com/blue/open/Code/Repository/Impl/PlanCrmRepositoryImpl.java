package com.blue.open.Code.Repository.Impl;

import com.blue.open.Code.Entity.Db.PlanCrm;
import com.blue.open.Code.Repository.Custom.PlanCrmRepositoryCustom;
import com.blue.open.Core.Entity.DTO.PaginateList;
import com.blue.open.Core.Repository.Impl.RepositoryImpl;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;

public class PlanCrmRepositoryImpl extends RepositoryImpl<PlanCrm> implements PlanCrmRepositoryCustom {

    @Override
    @Transactional
    public Page<PlanCrm> paginate(PaginateList paginateList) {
        return super.createSql(paginateList);
    }
}
