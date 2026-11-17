package com.blue.open.Code.Repository;

import com.blue.open.Code.Entity.Db.CompanyMercadoPago;
import com.blue.open.Code.Repository.Custom.CompanyMercadoPagoRepositoryCustom;
import com.blue.open.Core.Repository.EntityRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.List;

public interface CompanyMercadoPagoRepository extends EntityRepository<CompanyMercadoPago>,
        CompanyMercadoPagoRepositoryCustom {

    @Query("""
            SELECT c FROM CompanyMercadoPago c
            JOIN FETCH c.company
            WHERE c.company.id = :companyId
              AND c.status = 0
            """)
    Optional<CompanyMercadoPago> findActiveByCompanyId(@Param("companyId") Long companyId);

    @Query("""
            SELECT c FROM CompanyMercadoPago c
            JOIN FETCH c.company co
            WHERE co.uuid = :companyUuid
              AND c.status = 0
              AND co.status = 0
            """)
    Optional<CompanyMercadoPago> findActiveByCompanyUuid(@Param("companyUuid") String companyUuid);

    @Query("""
            SELECT c FROM CompanyMercadoPago c
            JOIN FETCH c.company
            """)
    List<CompanyMercadoPago> findAllIncludingInactiveWithCompany();
}
