package com.blue.open.Code.Repository;

import com.blue.open.Code.Entity.Db.MemberDescuento;
import com.blue.open.Code.Repository.Custom.MemberDescuentoRepositoryCustom;
import com.blue.open.Core.Repository.EntityRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface MemberDescuentoRepository extends EntityRepository<MemberDescuento>, MemberDescuentoRepositoryCustom {

    /**
     * Asignaciones vigentes de un socio en {@code fecha}. Puede devolver más de una: no hay
     * exclusividad entre asignaciones de un mismo socio.
     */
    @Query("""
            SELECT md FROM MemberDescuento md
            WHERE md.member.id = :memberId
              AND md.status = 0
              AND md.fechaDesde <= :fecha
              AND (md.fechaHasta IS NULL OR md.fechaHasta >= :fecha)
            """)
    List<MemberDescuento> findVigentesByMemberId(
            @Param("memberId") Long memberId,
            @Param("fecha") LocalDate fecha);

    /** Asignaciones activas (sin filtrar por vigencia) de un lote de socios, para listados. */
    @Query("""
            SELECT md FROM MemberDescuento md
            JOIN FETCH md.descuento d
            WHERE md.member.id IN :memberIds
              AND md.status = 0
            """)
    List<MemberDescuento> findActivasByMemberIdIn(@Param("memberIds") Collection<Long> memberIds);

    /** Resuelve una asignación por su propio uuid, scopeado a la company del socio dueño. */
    @Query("""
            SELECT md FROM MemberDescuento md
            JOIN FETCH md.descuento
            WHERE md.uuid = :uuid
              AND md.member.company.id = :companyId
              AND md.status = 0
            """)
    Optional<MemberDescuento> findByUuidAndCompanyIdAndStatusActive(
            @Param("uuid") String uuid,
            @Param("companyId") Long companyId);

    /** Elimina físicamente todas las asignaciones de un {@code Descuento} dado de baja del catálogo. */
    @Modifying
    @Query("DELETE FROM MemberDescuento md WHERE md.descuento.id = :descuentoId")
    void deleteAllByDescuentoId(@Param("descuentoId") Long descuentoId);
}
