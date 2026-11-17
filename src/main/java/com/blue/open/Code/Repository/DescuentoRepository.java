package com.blue.open.Code.Repository;

import com.blue.open.Code.Entity.Db.Descuento;
import com.blue.open.Code.Repository.Custom.DescuentoRepositoryCustom;
import com.blue.open.Core.Repository.EntityRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DescuentoRepository extends EntityRepository<Descuento>, DescuentoRepositoryCustom {

    @Query("""
            SELECT d FROM Descuento d
            WHERE d.company.id = :companyId
              AND d.status = 0
            ORDER BY d.titulo
            """)
    List<Descuento> findActivosByCompanyId(@Param("companyId") Long companyId);

    @Query("""
            SELECT d FROM Descuento d
            WHERE d.uuid = :uuid
              AND d.company.id = :companyId
              AND d.status = 0
            """)
    Optional<Descuento> findByUuidAndCompanyIdAndStatusActive(
            @Param("uuid") String uuid,
            @Param("companyId") Long companyId);
}
