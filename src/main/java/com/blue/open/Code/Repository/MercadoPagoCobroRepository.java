package com.blue.open.Code.Repository;

import com.blue.open.Code.Entity.Db.MercadoPagoCobro;
import com.blue.open.Code.Repository.Custom.MercadoPagoCobroRepositoryCustom;
import com.blue.open.Core.Repository.EntityRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface MercadoPagoCobroRepository extends EntityRepository<MercadoPagoCobro>,
        MercadoPagoCobroRepositoryCustom {

    @Query("""
            SELECT c FROM MercadoPagoCobro c
            JOIN FETCH c.mercadoPagoCobroIntent i
            LEFT JOIN FETCH c.items item
            LEFT JOIN FETCH item.cuota
            WHERE i.externalReference = :externalReference
              AND c.company.id = :companyId
              AND c.status = 0
              AND i.status = 0
            """)
    Optional<MercadoPagoCobro> findActiveByIntentExternalReferenceAndCompanyId(
            @Param("externalReference") String externalReference,
            @Param("companyId") Long companyId);
}
