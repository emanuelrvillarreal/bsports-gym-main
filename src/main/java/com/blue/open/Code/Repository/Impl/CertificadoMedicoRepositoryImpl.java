package com.blue.open.Code.Repository.Impl;

import com.blue.open.Code.Entity.Db.CertificadoMedico;
import com.blue.open.Code.Repository.Custom.CertificadoMedicoRepositoryCustom;
import com.blue.open.Core.Entity.DTO.PaginateList;
import com.blue.open.Core.Repository.Impl.RepositoryImpl;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;

public class CertificadoMedicoRepositoryImpl extends RepositoryImpl<CertificadoMedico> implements CertificadoMedicoRepositoryCustom {

    @Override
    @Transactional
    public Page<CertificadoMedico> paginate(PaginateList paginateList) {
        return super.createSql(paginateList);
    }
}
