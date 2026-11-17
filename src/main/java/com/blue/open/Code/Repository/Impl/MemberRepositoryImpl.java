package com.blue.open.Code.Repository.Impl;

import com.blue.open.Code.Entity.Db.Member;
import com.blue.open.Code.Repository.Custom.MemberRepositoryCustom;
import com.blue.open.Core.Entity.DTO.PaginateList;
import com.blue.open.Core.Repository.Impl.RepositoryImpl;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;

public class MemberRepositoryImpl extends RepositoryImpl<Member> implements MemberRepositoryCustom {

    @Override
    @Transactional
    public Page<Member> paginate(PaginateList paginateList) {
        return super.createSql(paginateList);
    }
}
