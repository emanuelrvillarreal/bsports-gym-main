package com.blue.open.Code.Repository;

import com.blue.open.Code.Entity.Db.InscripcionActividad;
import com.blue.open.Code.Entity.Enums.MemberEstadoEnum;
import com.blue.open.Code.Repository.Custom.InscripcionActividadRepositoryCustom;
import com.blue.open.Core.Repository.EntityRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface InscripcionActividadRepository extends EntityRepository<InscripcionActividad>,
        InscripcionActividadRepositoryCustom {

    boolean existsByActividad_IdAndMember_IdAndStatus(Long actividadId, Long memberId, Integer status);

    long countByActividad_IdAndStatus(Long actividadId, Integer status);

    @Query("""
            SELECT i.actividad.id, COUNT(i) FROM InscripcionActividad i
            WHERE i.actividad.id IN :actividadIds
              AND i.status = 0
            GROUP BY i.actividad.id
            """)
    List<Object[]> countActivasByActividadIdIn(@Param("actividadIds") Collection<Long> actividadIds);

    /**
     * Alumnos por actividad para el porcentaje de asistencia: [actividadUuid, memberId, status,
     * dateUpdated, horarioUuid]. Hay una fila por cada horario elegido (horarioUuid null si la
     * inscripcion no tiene horarios).
     */
    @Query("""
            SELECT i.actividad.uuid, i.member.id, i.status, i.dateUpdated, h.uuid FROM InscripcionActividad i
            LEFT JOIN i.horarios h
            WHERE i.company.id = :companyId
              AND i.actividad.uuid IN :actividadUuids
              AND i.status IN :statuses
            """)
    List<Object[]> findAlumnosByActividadUuidIn(
            @Param("companyId") Long companyId,
            @Param("actividadUuids") Collection<String> actividadUuids,
            @Param("statuses") Collection<Integer> statuses);

    /** Horarios a los que asiste el socio en sus inscripciones activas, para su calendario. */
    @Query("""
            SELECT h.uuid FROM InscripcionActividad i JOIN i.horarios h
            WHERE i.member.id = :memberId AND i.company.id = :companyId AND i.status = 0
            """)
    List<String> findHorarioUuidsActivosByMemberIdAndCompanyId(
            @Param("memberId") Long memberId,
            @Param("companyId") Long companyId);

    Optional<InscripcionActividad> findByActividad_IdAndMember_IdAndStatus(
            Long actividadId, Long memberId, Integer status);

    /**
     * Inscripciones activas del club a las que ya les toca la próxima cuota
     * ({@code proximaGeneracion <= hasta}). Al generarla, el job avanza el puntero en la misma
     * transacción, así que una segunda corrida no vuelve a encontrar la misma inscripción.
     */
    @Query("""
            SELECT i FROM InscripcionActividad i
            JOIN FETCH i.member m
            LEFT JOIN FETCH m.titular
            JOIN FETCH i.actividad a
            WHERE i.company.id = :companyId
              AND i.status = 0
              AND a.status = 0
              AND m.status = 0
              AND m.estado IN :estados
              AND i.proximaGeneracion <= :hasta
            """)
    List<InscripcionActividad> findConGeneracionPendiente(
            @Param("companyId") Long companyId,
            @Param("estados") Collection<MemberEstadoEnum> estados,
            @Param("hasta") LocalDate hasta);

    /** Pares (memberId, nombre de la actividad) de las inscripciones vigentes en actividades que exigen certificado. */
    @Query("""
            SELECT i.member.id, a.nombre FROM InscripcionActividad i
            JOIN i.actividad a
            WHERE i.company.id = :companyId
              AND i.status = 0
              AND a.status = 0
              AND a.requiereCertificado = true
            """)
    List<Object[]> findActividadesQueExigenCertificado(@Param("companyId") Long companyId);

    @Query("""
            SELECT i FROM InscripcionActividad i
            JOIN FETCH i.actividad a
            WHERE i.member.id = :memberId
              AND i.company.id = :companyId
              AND i.status = :status
              AND a.status = 0
            """)
    List<InscripcionActividad> findActivasByMemberIdAndCompanyIdWithActividad(
            @Param("memberId") Long memberId,
            @Param("companyId") Long companyId,
            @Param("status") Integer status);

    @Query("""
            SELECT DISTINCT i.actividad.id FROM InscripcionActividad i
            WHERE i.member.id = :memberId
              AND i.company.id = :companyId
              AND i.status = 0
            """)
    List<Long> findActividadIdsByMemberIdAndCompanyId(
            @Param("memberId") Long memberId,
            @Param("companyId") Long companyId);

    @Query("""
            SELECT i FROM InscripcionActividad i
            JOIN FETCH i.member m
            LEFT JOIN FETCH m.user
            LEFT JOIN FETCH m.titular
            WHERE i.company.id = :companyId
              AND i.actividad.id = :actividadId
              AND i.status = 0
            """)
    List<InscripcionActividad> findActivasByActividadIdAndCompanyIdWithMember(
            @Param("actividadId") Long actividadId,
            @Param("companyId") Long companyId);

    @Query("""
            SELECT i FROM InscripcionActividad i
            JOIN FETCH i.member m
            LEFT JOIN FETCH m.user
            WHERE i.company.id = :companyId
              AND i.actividad.id = :actividadId
              AND i.status IN :statuses
            """)
    List<InscripcionActividad> findByActividadIdAndCompanyIdAndStatusInWithMember(
            @Param("actividadId") Long actividadId,
            @Param("companyId") Long companyId,
            @Param("statuses") Collection<Integer> statuses);

    @Query("""
            SELECT i FROM InscripcionActividad i
            WHERE i.uuid = :uuid
              AND i.company.id = :companyId
              AND i.status = 0
            """)
    Optional<InscripcionActividad> findByUuidAndCompanyIdAndStatusActive(
            @Param("uuid") String uuid,
            @Param("companyId") Long companyId);

    /**
     * Periodos de inscripcion (activos y cerrados) de las actividades que se superponen con el rango.
     * Los cerrados sin fecha de baja no son historial (registros anteriores al v77 sin fecha) y se omiten.
     */
    @Query("""
            SELECT DISTINCT i FROM InscripcionActividad i
            JOIN FETCH i.actividad act
            JOIN FETCH i.member m
            LEFT JOIN FETCH m.user
            LEFT JOIN FETCH m.categoria
            WHERE i.company.id = :companyId
              AND act.id IN :actividadIds
              AND (i.status = 0 OR (i.status = -1 AND i.fechaBaja IS NOT NULL AND :incluirBajas = true))
              AND i.fechaInscripcion <= :hasta
              AND (i.fechaBaja IS NULL OR i.fechaBaja >= :desde)
            """)
    List<InscripcionActividad> findPeriodosByActividadIdIn(
            @Param("companyId") Long companyId,
            @Param("actividadIds") Collection<Long> actividadIds,
            @Param("desde") java.time.LocalDate desde,
            @Param("hasta") java.time.LocalDate hasta,
            @Param("incluirBajas") boolean incluirBajas);

    /** Todos los periodos de inscripcion del socio (historial completo), del mas viejo al mas nuevo. */
    @Query("""
            SELECT DISTINCT i FROM InscripcionActividad i
            JOIN FETCH i.actividad act
            JOIN FETCH i.member m
            LEFT JOIN FETCH m.user
            LEFT JOIN FETCH m.categoria
            WHERE i.company.id = :companyId
              AND m.id = :memberId
              AND (i.status = 0 OR (i.status = -1 AND i.fechaBaja IS NOT NULL))
            ORDER BY i.fechaInscripcion
            """)
    List<InscripcionActividad> findHistorialByMemberId(
            @Param("companyId") Long companyId,
            @Param("memberId") Long memberId);

    @Query("""
            SELECT i FROM InscripcionActividad i
            JOIN FETCH i.actividad act
            JOIN FETCH i.member m
            LEFT JOIN FETCH m.user
            LEFT JOIN FETCH m.categoria
            WHERE i.uuid = :uuid
              AND i.company.id = :companyId
              AND (i.status = 0 OR (i.status = -1 AND i.fechaBaja IS NOT NULL))
            """)
    Optional<InscripcionActividad> findPeriodoByUuid(
            @Param("uuid") String uuid,
            @Param("companyId") Long companyId);
}
