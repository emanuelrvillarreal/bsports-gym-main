package com.blue.open.Code.Repository;

import com.blue.open.Code.Entity.Db.InscripcionActividadDescuento;
import com.blue.open.Code.Repository.Custom.InscripcionActividadDescuentoRepositoryCustom;
import com.blue.open.Core.Repository.EntityRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface InscripcionActividadDescuentoRepository
        extends EntityRepository<InscripcionActividadDescuento>, InscripcionActividadDescuentoRepositoryCustom {

    /**
     * Asignaciones vigentes de una inscripción en {@code fecha}. Puede devolver más de una: no
     * hay exclusividad entre asignaciones de una misma inscripción.
     */
    @Query("""
            SELECT iad FROM InscripcionActividadDescuento iad
            WHERE iad.inscripcionActividad.id = :inscripcionActividadId
              AND iad.status = 0
              AND iad.fechaDesde <= :fecha
              AND (iad.fechaHasta IS NULL OR iad.fechaHasta >= :fecha)
            """)
    List<InscripcionActividadDescuento> findVigentesByInscripcionActividadId(
            @Param("inscripcionActividadId") Long inscripcionActividadId,
            @Param("fecha") LocalDate fecha);

    /** Asignaciones activas (sin filtrar por vigencia) de un lote de socios, para listados. */
    @Query("""
            SELECT iad FROM InscripcionActividadDescuento iad
            JOIN FETCH iad.descuento d
            JOIN FETCH iad.inscripcionActividad ia
            JOIN FETCH ia.actividad a
            WHERE ia.member.id IN :memberIds
              AND iad.status = 0
              AND ia.status = 0
            """)
    List<InscripcionActividadDescuento> findActivasByMemberIdIn(@Param("memberIds") Collection<Long> memberIds);

    /** Resuelve una asignación por su propio uuid, scopeado a la company de la inscripción dueña. */
    @Query("""
            SELECT iad FROM InscripcionActividadDescuento iad
            JOIN FETCH iad.descuento
            JOIN FETCH iad.inscripcionActividad ia
            WHERE iad.uuid = :uuid
              AND ia.company.id = :companyId
              AND iad.status = 0
            """)
    Optional<InscripcionActividadDescuento> findByUuidAndCompanyIdAndStatusActive(
            @Param("uuid") String uuid,
            @Param("companyId") Long companyId);

    /** Elimina físicamente todas las asignaciones de un {@code Descuento} dado de baja del catálogo. */
    @Modifying
    @Query("DELETE FROM InscripcionActividadDescuento iad WHERE iad.descuento.id = :descuentoId")
    void deleteAllByDescuentoId(@Param("descuentoId") Long descuentoId);
}
