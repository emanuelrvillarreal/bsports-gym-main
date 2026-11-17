package com.blue.open.Code.Repository;

import com.blue.open.Code.Entity.Db.Reserva;
import com.blue.open.Code.Entity.Enums.ReservaEstadoEnum;
import com.blue.open.Code.Repository.Custom.ReservaRepositoryCustom;
import com.blue.open.Core.Repository.EntityRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ReservaRepository extends EntityRepository<Reserva>, ReservaRepositoryCustom {

    @Query("""
            SELECT r FROM Reserva r
            JOIN FETCH r.slot s
            JOIN FETCH r.amenity a
            JOIN FETCH r.member m
            WHERE r.uuid = :uuid
              AND r.company.id = :companyId
              AND r.status = 0
            """)
    Optional<Reserva> findByUuidAndCompanyIdWithDetails(
            @Param("uuid") String uuid,
            @Param("companyId") Long companyId);

    @Query("""
            SELECT COUNT(r) FROM Reserva r
            JOIN r.slot s
            WHERE r.company.id = :companyId
              AND r.member.id = :memberId
              AND r.estado IN :estadosActivos
              AND s.inicio >= :semanaInicio
              AND s.inicio < :semanaFin
              AND r.status = 0
            """)
    long countByMemberAndSemanaCalendario(
            @Param("companyId") Long companyId,
            @Param("memberId") Long memberId,
            @Param("estadosActivos") List<ReservaEstadoEnum> estadosActivos,
            @Param("semanaInicio") LocalDateTime semanaInicio,
            @Param("semanaFin") LocalDateTime semanaFin);

    @Query("""
            SELECT r FROM Reserva r
            JOIN FETCH r.slot s
            WHERE r.estado = :estado
              AND r.venceSenaAt < :ahora
              AND r.status = 0
            """)
    List<Reserva> findPendientesSenaVencidas(
            @Param("estado") ReservaEstadoEnum estado,
            @Param("ahora") LocalDateTime ahora);

    @Query("""
            SELECT r FROM Reserva r
            JOIN FETCH r.member m
            LEFT JOIN FETCH m.user
            WHERE r.company.id = :companyId
              AND r.slot.id IN :slotIds
              AND r.status = 0
            """)
    List<Reserva> findByCompanyIdAndSlotIdIn(
            @Param("companyId") Long companyId,
            @Param("slotIds") List<Long> slotIds);

    @Query(value = """
            SELECT r FROM Reserva r
            JOIN FETCH r.slot s
            JOIN FETCH r.amenity a
            JOIN FETCH r.member m
            LEFT JOIN FETCH m.user
            LEFT JOIN FETCH r.canceladaPor
            WHERE r.company.id = :companyId
              AND r.member.id = :memberId
              AND r.status = 0
            ORDER BY s.inicio DESC
            """,
            countQuery = """
            SELECT COUNT(r) FROM Reserva r
            WHERE r.company.id = :companyId
              AND r.member.id = :memberId
              AND r.status = 0
            """)
    Page<Reserva> paginateByCompanyIdAndMemberId(
            @Param("companyId") Long companyId,
            @Param("memberId") Long memberId,
            Pageable pageable);

    @Query("""
            SELECT r FROM Reserva r
            JOIN FETCH r.slot s
            JOIN FETCH r.amenity a
            JOIN FETCH r.member m
            LEFT JOIN FETCH m.user
            WHERE r.company.id = :companyId
              AND r.member.id = :memberId
              AND r.estado IN :estados
              AND s.inicio >= :desde
              AND s.inicio <= :hasta
              AND r.status = 0
            ORDER BY s.inicio
            """)
    List<Reserva> findActivasByCompanyIdAndMemberIdAndSlotInicioBetween(
            @Param("companyId") Long companyId,
            @Param("memberId") Long memberId,
            @Param("estados") List<ReservaEstadoEnum> estados,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);
}
