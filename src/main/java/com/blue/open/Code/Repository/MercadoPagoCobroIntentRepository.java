package com.blue.open.Code.Repository;

import com.blue.open.Code.Entity.Db.MercadoPagoCobroIntent;
import com.blue.open.Code.Repository.Custom.MercadoPagoCobroIntentRepositoryCustom;
import com.blue.open.Core.Repository.EntityRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface MercadoPagoCobroIntentRepository extends EntityRepository<MercadoPagoCobroIntent>,
        MercadoPagoCobroIntentRepositoryCustom {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT i FROM MercadoPagoCobroIntent i
            JOIN FETCH i.company
            JOIN FETCH i.mercadoPagoCobro mpCob
            JOIN FETCH mpCob.registradoPor
            LEFT JOIN FETCH mpCob.eventoAcceso
            LEFT JOIN FETCH mpCob.items item
            LEFT JOIN FETCH item.cuota c
            LEFT JOIN FETCH c.titular
            LEFT JOIN FETCH c.persona
            WHERE i.externalReference = :externalReference
              AND i.company.id = :companyId
              AND i.status = 0
            """)
    Optional<MercadoPagoCobroIntent> findActiveForUpdateByExternalReferenceAndCompanyId(
            @Param("externalReference") String externalReference,
            @Param("companyId") Long companyId);
}
