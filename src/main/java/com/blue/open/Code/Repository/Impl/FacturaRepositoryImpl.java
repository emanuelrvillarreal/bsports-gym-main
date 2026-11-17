package com.blue.open.Code.Repository.Impl;

import com.blue.open.Code.Entity.Db.Factura;
import com.blue.open.Code.Repository.Custom.FacturaRepositoryCustom;
import com.blue.open.Core.Entity.DTO.PaginateList;
import com.blue.open.Core.Repository.Impl.RepositoryImpl;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;

/**
 * Implementacion de las consultas propias de FacturaRepository.
 * Delega la paginacion en RepositoryImpl del Core, igual que el resto de los repositorios.
 */
public class FacturaRepositoryImpl extends RepositoryImpl<Factura> implements FacturaRepositoryCustom {

    @Override
    @Transactional
    public Page<Factura> paginate(PaginateList paginateList) {
        return super.createSql(paginateList);
    }
}
