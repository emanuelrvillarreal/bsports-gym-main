package com.blue.open.Code.Repository;

import com.blue.open.Code.Entity.Db.AsistenciaActividad;
import com.blue.open.Code.Entity.Enums.AsistenciaActividadEstadoEnum;
import com.blue.open.Code.Repository.Custom.AsistenciaActividadRepositoryCustom;
import com.blue.open.Core.Repository.EntityRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AsistenciaActividadRepository extends EntityRepository<AsistenciaActividad>,
        AsistenciaActividadRepositoryCustom {

    @Query("""
            SELECT a FROM AsistenciaActividad a
            JOIN FETCH a.member m
            LEFT JOIN FETCH m.user
            WHERE a.slot.id = :slotId
              AND a.status = 0
            """)
    List<AsistenciaActividad> findBySlotIdWithMember(@Param("slotId") Long slotId);

    @Query("""
            SELECT a FROM AsistenciaActividad a
            JOIN FETCH a.member m
            WHERE a.slot.id = :slotId
              AND a.member.id IN :memberIds
              AND a.status = 0
            """)
    List<AsistenciaActividad> findBySlotIdAndMemberIdIn(
            @Param("slotId") Long slotId,
            @Param("memberIds") Collection<Long> memberIds);

    @Query("""
            SELECT a.slot.uuid, COUNT(a) FROM AsistenciaActividad a
            WHERE a.slot.uuid IN :slotUuids
              AND a.estado = :estado
              AND a.status = 0
            GROUP BY a.slot.uuid
            """)
    List<Object[]> countBySlotUuidInAndEstado(
            @Param("slotUuids") Collection<String> slotUuids,
            @Param("estado") AsistenciaActividadEstadoEnum estado);

    Optional<AsistenciaActividad> findFirstBySlot_IdAndMember_IdAndStatus(
            Long slotId, Long memberId, Integer status);

    /** Registros de asistencia de las actividades en el rango: [slotId, memberId, estado]. */
    @Query("""
            SELECT a.slot.id, a.member.id, a.estado FROM AsistenciaActividad a
            WHERE a.company.id = :companyId
              AND a.actividad.id IN :actividadIds
              AND a.status = 0
              AND a.fecha >= :desde
              AND a.fecha <= :hasta
            """)
    List<Object[]> findMarcasByActividadIdIn(
            @Param("companyId") Long companyId,
            @Param("actividadIds") Collection<Long> actividadIds,
            @Param("desde") java.time.LocalDate desde,
            @Param("hasta") java.time.LocalDate hasta);

    /** Igual que findMarcasByActividadIdIn, acotado a ciertos socios (portal y trayectoria). */
    @Query("""
            SELECT a.slot.id, a.member.id, a.estado FROM AsistenciaActividad a
            WHERE a.company.id = :companyId
              AND a.actividad.id IN :actividadIds
              AND a.member.id IN :memberIds
              AND a.status = 0
              AND a.fecha >= :desde
              AND a.fecha <= :hasta
            """)
    List<Object[]> findMarcasByActividadIdInAndMemberIdIn(
            @Param("companyId") Long companyId,
            @Param("actividadIds") Collection<Long> actividadIds,
            @Param("memberIds") Collection<Long> memberIds,
            @Param("desde") java.time.LocalDate desde,
            @Param("hasta") java.time.LocalDate hasta);
}
