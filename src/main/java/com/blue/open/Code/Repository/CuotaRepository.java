package com.blue.open.Code.Repository;

import com.blue.open.Code.Entity.Db.Cuota;
import com.blue.open.Code.Entity.Db.CuotaDescuento;
import com.blue.open.Code.Entity.Enums.CuotaEstadoEnum;
import com.blue.open.Code.Repository.Custom.CuotaRepositoryCustom;
import com.blue.open.Core.Repository.EntityRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface CuotaRepository extends EntityRepository<Cuota>, CuotaRepositoryCustom {

    @Query("""
            SELECT DISTINCT c.titular.id FROM Cuota c
            WHERE c.company.id = :companyId
              AND c.status = 0
              AND c.estado = com.blue.open.Code.Entity.Enums.CuotaEstadoEnum.VENCIDA
              AND c.tipo <> com.blue.open.Code.Entity.Enums.CuotaTipoEnum.PASE
            """)
    Set<Long> findTitularIdsConCuotaVencida(@Param("companyId") Long companyId);

    @Query("""
            SELECT MIN(c.vencimiento) FROM Cuota c
            WHERE c.company.id = :companyId
              AND c.titular.id = :titularId
              AND c.status = 0
              AND c.estado = com.blue.open.Code.Entity.Enums.CuotaEstadoEnum.VENCIDA
              AND c.tipo <> com.blue.open.Code.Entity.Enums.CuotaTipoEnum.PASE
            """)
    Optional<LocalDate> findVencimientoCuotaVencidaMasAntiguaGrupo(
            @Param("companyId") Long companyId,
            @Param("titularId") Long titularId);

    /** LEFT JOIN FETCH descuentos: {@link Cuota#montoFinal()} lo necesita con la sesión cerrada. */
    @Query("""
            SELECT DISTINCT c FROM Cuota c
            JOIN FETCH c.persona
            JOIN FETCH c.titular
            LEFT JOIN FETCH c.descuentos
            WHERE c.uuid IN :uuids
              AND c.company.id = :companyId
              AND c.status = 0
            """)
    List<Cuota> findByUuidInAndCompanyIdWithPersonas(
            @Param("uuids") Collection<String> uuids,
            @Param("companyId") Long companyId);

    /** Cuotas sociales impagas del club, sin recargo aplicado, que vencieron hasta {@code limite}. */
    @Query("""
            SELECT DISTINCT c FROM Cuota c
            LEFT JOIN FETCH c.descuentos
            WHERE c.company.id = :companyId
              AND c.status = 0
              AND c.tipo = com.blue.open.Code.Entity.Enums.CuotaTipoEnum.SOCIAL
              AND c.estado IN :estados
              AND c.recargoAplicadoAt IS NULL
              AND c.vencimiento <= :limite
            """)
    List<Cuota> findSocialesImpagasParaRecargo(
            @Param("companyId") Long companyId,
            @Param("estados") Collection<CuotaEstadoEnum> estados,
            @Param("limite") LocalDate limite);

    /** Cuotas de actividades con recargo activo, impagas y sin recargo aplicado, que vencieron hasta {@code limite}. */
    @Query("""
            SELECT DISTINCT c FROM Cuota c
            JOIN FETCH c.actividad a
            LEFT JOIN FETCH c.descuentos
            WHERE c.company.id = :companyId
              AND c.status = 0
              AND c.tipo = com.blue.open.Code.Entity.Enums.CuotaTipoEnum.ACTIVIDAD
              AND a.recargo.habilitado = true
              AND c.estado IN :estados
              AND c.recargoAplicadoAt IS NULL
              AND c.vencimiento <= :limite
            """)
    List<Cuota> findActividadImpagasParaRecargo(
            @Param("companyId") Long companyId,
            @Param("estados") Collection<CuotaEstadoEnum> estados,
            @Param("limite") LocalDate limite);

    /** Cuotas impagas de una actividad; con descuentos cargados para decidir si se pueden actualizar. */
    @Query("""
            SELECT DISTINCT c FROM Cuota c
            LEFT JOIN FETCH c.descuentos
            WHERE c.company.id = :companyId
              AND c.actividad.id = :actividadId
              AND c.status = 0
              AND c.estado IN :estados
            """)
    List<Cuota> findImpagasDeActividad(
            @Param("companyId") Long companyId,
            @Param("actividadId") Long actividadId,
            @Param("estados") Collection<CuotaEstadoEnum> estados);

    Cuota findFirstByUuidAndCompanyIdAndStatus(String uuid, Long companyId, Integer status);

    @Query("""
            SELECT c FROM Cuota c
            JOIN FETCH c.persona p
            JOIN FETCH p.user
            JOIN FETCH c.titular t
            JOIN FETCH t.user
            LEFT JOIN FETCH c.anuladaPor
            WHERE c.uuid = :uuid
              AND c.company.id = :companyId
              AND c.status = 0
            """)
    Cuota findByUuidAndCompanyIdWithDetails(
            @Param("uuid") String uuid,
            @Param("companyId") Long companyId);

    @Modifying(clearAutomatically = true)
    @Query("""
            UPDATE Cuota c SET c.estado = :nuevoEstado
            WHERE c.company.id = :companyId
              AND c.status = 0
              AND c.estado = :estadoOrigen
              AND c.vencimiento <= :fechaCorte
            """)
    int marcarPendientesVencidas(
            @Param("companyId") Long companyId,
            @Param("estadoOrigen") CuotaEstadoEnum estadoOrigen,
            @Param("nuevoEstado") CuotaEstadoEnum nuevoEstado,
            @Param("fechaCorte") LocalDate fechaCorte);

    /**
     * El neto se calcula acá en vez de en una columna generada: MariaDB rechaza tanto
     * ROUND() como la división dentro de GENERATED ALWAYS AS (el resultado de {@code /}
     * depende de la variable de sesión div_precision_increment, así que no lo considera
     * determinístico para persistir). En un SELECT normal esa restricción no existe.
     * <p>
     * El descuento de cada cuota es la suma de sus {@link CuotaDescuento}, nunca negativo
     * (mismo tope que {@link Cuota#montoFinal()}).
     */
    @Query("""
            SELECT COALESCE(SUM(
                    function('greatest', c.monto - COALESCE(
                        (SELECT SUM(cd.valor) FROM CuotaDescuento cd WHERE cd.cuota = c), 0
                    ), 0) + c.recargo
                ), 0) FROM Cuota c
            WHERE c.company.id = :companyId
              AND c.status = 0
              AND c.estado IN :estados
              AND c.fechaGeneracion BETWEEN :desde AND :hasta
            """)
    BigDecimal sumMontoPorCobrarByPeriodo(
            @Param("companyId") Long companyId,
            @Param("estados") Collection<CuotaEstadoEnum> estados,
            @Param("desde") LocalDate desde,
            @Param("hasta") LocalDate hasta);

    /**
     * Fechas de generación distintas del club. Se devuelve la fecha completa y no year()/month():
     * así se resuelve recorriendo {@code idx_cuotas_company_fecha_generacion}, y el período se arma en Java.
     */
    @Query("""
            SELECT DISTINCT c.fechaGeneracion FROM Cuota c
            WHERE c.company.id = :companyId
              AND c.status = 0
            """)
    List<LocalDate> findDistinctFechasGeneracion(@Param("companyId") Long companyId);

    /** Cuota de actividad vigente (no anulada) de esa persona para esa fecha de generación, si existe. */
    Optional<Cuota> findFirstByPersona_IdAndActividad_IdAndFechaGeneracionAndEstadoNot(
            Long personaId, Long actividadId, LocalDate fechaGeneracion, CuotaEstadoEnum estado);
}
