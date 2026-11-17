package com.blue.open.Code.Repository;

import com.blue.open.Code.Entity.Db.Movimiento;
import com.blue.open.Code.Entity.Enums.MovimientoEstadoEnum;
import com.blue.open.Code.Entity.Enums.MovimientoTipoEnum;
import com.blue.open.Code.Repository.Custom.MovimientoRepositoryCustom;
import com.blue.open.Core.Repository.EntityRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MovimientoRepository extends EntityRepository<Movimiento>, MovimientoRepositoryCustom {

    boolean existsByCuotaIdAndEstadoAndStatus(Long cuotaId, MovimientoEstadoEnum estado, Integer status);

    Optional<Movimiento> findFirstByCuotaIdAndEstadoAndStatus(Long cuotaId, MovimientoEstadoEnum estado, Integer status);

    @Query("""
            SELECT COALESCE(SUM(m.monto), 0) FROM Movimiento m
            WHERE m.company.id = :companyId
              AND m.status = 0
              AND m.estado = :estado
              AND m.tipo = :tipo
              AND m.fecha >= :desde
              AND m.fecha <= :hasta
            """)
    BigDecimal sumMontoByCompanyTipoEstadoAndFechaBetween(
            @Param("companyId") Long companyId,
            @Param("tipo") MovimientoTipoEnum tipo,
            @Param("estado") MovimientoEstadoEnum estado,
            @Param("desde") LocalDate desde,
            @Param("hasta") LocalDate hasta);

    @Query("""
            SELECT DISTINCT YEAR(m.fecha), MONTH(m.fecha) FROM Movimiento m
            WHERE m.company.id = :companyId
              AND m.status = 0
              AND m.estado = com.blue.open.Code.Entity.Enums.MovimientoEstadoEnum.ACTIVO
            """)
    List<Object[]> findDistinctPeriodosFromMovimientos(@Param("companyId") Long companyId);

    @Query("""
            SELECT m FROM Movimiento m
            LEFT JOIN FETCH m.cuota
            LEFT JOIN FETCH m.persona p
            LEFT JOIN FETCH p.user
            LEFT JOIN FETCH m.titular t
            LEFT JOIN FETCH t.user
            LEFT JOIN FETCH m.registradoPor
            LEFT JOIN FETCH m.anuladaPor
            WHERE m.uuid = :uuid
              AND m.company.id = :companyId
              AND m.status = 0
            """)
    Movimiento findByUuidAndCompanyIdWithDetails(
            @Param("uuid") String uuid,
            @Param("companyId") Long companyId);
}
