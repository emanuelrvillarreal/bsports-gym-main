package com.blue.open.Code.Repository;

import com.blue.open.Code.Entity.Db.EventoActividad;
import com.blue.open.Code.Repository.Custom.EventoActividadRepositoryCustom;
import com.blue.open.Core.Repository.EntityRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface EventoActividadRepository extends EntityRepository<EventoActividad>, EventoActividadRepositoryCustom {

    @Query("""
            SELECT e FROM EventoActividad e
            JOIN FETCH e.actividad a
            WHERE e.uuid = :uuid
              AND e.company.id = :companyId
              AND e.status = 0
            """)
    Optional<EventoActividad> findByUuidAndCompanyIdWithDetails(
            @Param("uuid") String uuid,
            @Param("companyId") Long companyId);

    @Query("""
            SELECT e FROM EventoActividad e
            JOIN FETCH e.actividad a
            WHERE e.company.id = :companyId
              AND e.status = 0
              AND e.inicio >= :desde
              AND e.inicio <= :hasta
            ORDER BY e.inicio
            """)
    List<EventoActividad> findByCompanyAndInicioBetweenWithDetails(
            @Param("companyId") Long companyId,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);

    @Query("""
            SELECT e FROM EventoActividad e
            JOIN FETCH e.actividad a
            WHERE e.company.id = :companyId
              AND e.status = 0
              AND e.actividad.id IN :actividadIds
              AND e.inicio >= :desde
              AND e.inicio <= :hasta
            ORDER BY e.inicio
            """)
    List<EventoActividad> findByActividadIdsAndInicioBetweenWithDetails(
            @Param("companyId") Long companyId,
            @Param("actividadIds") Collection<Long> actividadIds,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);
}
