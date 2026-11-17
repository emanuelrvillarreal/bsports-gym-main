package com.blue.open.Code.Repository;

import com.blue.open.Code.Entity.Db.EventoAcceso;
import com.blue.open.Code.Repository.Custom.EventoAccesoRepositoryCustom;
import com.blue.open.Core.Repository.EntityRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface EventoAccesoRepository extends EntityRepository<EventoAcceso>, EventoAccesoRepositoryCustom {

    @Query("""
            SELECT e FROM EventoAcceso e
            WHERE e.uuid = :uuid
              AND e.company.id = :companyId
              AND e.status = 0
            """)
    Optional<EventoAcceso> findByUuidAndCompanyId(
            @Param("uuid") String uuid,
            @Param("companyId") Long companyId);
}
