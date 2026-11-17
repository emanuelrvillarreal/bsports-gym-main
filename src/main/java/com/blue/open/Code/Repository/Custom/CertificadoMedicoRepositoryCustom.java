package com.blue.open.Code.Repository.Custom;

import com.blue.open.Code.Entity.Db.CertificadoMedico;
import com.blue.open.Core.Entity.DTO.PaginateList;
import org.springframework.data.domain.Page;

public interface CertificadoMedicoRepositoryCustom {

    Page<CertificadoMedico> paginate(PaginateList paginateList);
}
