package com.blue.open.Code.Repository;

import com.blue.open.Code.Entity.Db.Slot;
import com.blue.open.Code.Repository.Custom.SlotRepositoryCustom;
import com.blue.open.Core.Repository.EntityRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SlotRepository extends EntityRepository<Slot>, SlotRepositoryCustom {

    boolean existsByActividad_IdAndInicio(Long actividadId, LocalDateTime inicio);

    void deleteByActividad_Id(Long actividadId);

    void deleteByActividad_IdAndInicioGreaterThanEqual(Long actividadId, LocalDateTime inicio);

    @Query("""
            SELECT s.inicio FROM Slot s
            WHERE s.actividad.id = :actividadId
            AND s.inicio >= :ventanaInicio
            AND s.inicio <= :ventanaFin
            """)
    List<LocalDateTime> findIniciosByActividad_IdAndInicioBetween(
            @Param("actividadId") Long actividadId,
            @Param("ventanaInicio") LocalDateTime ventanaInicio,
            @Param("ventanaFin") LocalDateTime ventanaFin);

    /** Clases ya generadas de un horario en la ventana, para no duplicarlas: [inicio]. */
    @Query("""
            SELECT s.inicio FROM Slot s
            WHERE s.horario.id = :horarioId
            AND s.inicio >= :ventanaInicio
            AND s.inicio <= :ventanaFin
            """)
    List<LocalDateTime> findIniciosByHorarioIdAndInicioBetween(
            @Param("horarioId") Long horarioId,
            @Param("ventanaInicio") LocalDateTime ventanaInicio,
            @Param("ventanaFin") LocalDateTime ventanaFin);

    /**
     * Borra las clases futuras de la actividad que todavia no tienen asistencia cargada. Se usa al
     * cambiar los horarios: las clases con asistencia se conservan como historial.
     */
    @Modifying
    @Query("""
            DELETE FROM Slot s
            WHERE s.actividad.id = :actividadId
            AND s.inicio > :desde
            AND NOT EXISTS (SELECT 1 FROM AsistenciaActividad a WHERE a.slot = s)
            """)
    int deleteFuturosSinAsistenciaByActividadId(
            @Param("actividadId") Long actividadId,
            @Param("desde") LocalDateTime desde);

    @Query("""
            SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END FROM Slot s
            WHERE s.amenity.id = :amenityId
              AND s.estado IN :estadosBloqueantes
              AND s.inicio < :fin
              AND s.fin > :inicio
              AND s.status = 0
            """)
    boolean existsSolapamientoAmenity(
            @Param("amenityId") Long amenityId,
            @Param("estadosBloqueantes") List<com.blue.open.Code.Entity.Enums.SlotEstadoEnum> estadosBloqueantes,
            @Param("inicio") LocalDateTime inicio,
            @Param("fin") LocalDateTime fin);

    @Query("""
            SELECT s FROM Slot s
            WHERE s.amenity.id = :amenityId
              AND s.inicio >= :desde
              AND s.estado IN :estados
              AND s.status = 0
            """)
    List<Slot> findFuturosByAmenityId(
            @Param("amenityId") Long amenityId,
            @Param("desde") LocalDateTime desde,
            @Param("estados") List<com.blue.open.Code.Entity.Enums.SlotEstadoEnum> estados);

    @Query("""
            SELECT s FROM Slot s
            LEFT JOIN FETCH s.actividad a
            LEFT JOIN FETCH s.amenity am
            WHERE s.company.id = :companyId
              AND s.inicio >= :desde
              AND s.inicio <= :hasta
              AND s.status = 0
            ORDER BY s.inicio
            """)
    List<Slot> findByCompanyAndInicioBetweenWithDetails(
            @Param("companyId") Long companyId,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);

    @Query("""
            SELECT s FROM Slot s
            LEFT JOIN FETCH s.actividad a
            WHERE s.company.id = :companyId
              AND s.actividad.id IN :actividadIds
              AND s.inicio >= :desde
              AND s.inicio <= :hasta
              AND s.status = 0
            ORDER BY s.inicio
            """)
    List<Slot> findByActividadIdsAndInicioBetweenWithDetails(
            @Param("companyId") Long companyId,
            @Param("actividadIds") List<Long> actividadIds,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);

    @Query("""
            SELECT s FROM Slot s
            JOIN FETCH s.actividad a
            WHERE s.uuid = :uuid
              AND s.company.id = :companyId
              AND s.status = 0
            """)
    Optional<Slot> findByUuidAndCompanyIdWithActividad(
            @Param("uuid") String uuid,
            @Param("companyId") Long companyId);

    /**
     * Clases efectivamente dictadas de las actividades: no canceladas, dentro del rango y con la planilla
     * de asistencia cargada (al menos un registro). Devuelve [slotId, actividadId, horarioId, inicio].
     */
    @Query("""
            SELECT s.id, s.actividad.id, h.id, s.inicio FROM Slot s
            LEFT JOIN s.horario h
            WHERE s.company.id = :companyId
            AND s.actividad.id IN :actividadIds
            AND s.status = 0
            AND s.estado <> com.blue.open.Code.Entity.Enums.SlotEstadoEnum.CANCELADO
            AND s.inicio >= :desde
            AND s.inicio < :hasta
            AND EXISTS (SELECT 1 FROM AsistenciaActividad a WHERE a.slot = s AND a.status = 0)
            ORDER BY s.inicio
            """)
    List<Object[]> findClasesRealizadas(
            @Param("companyId") Long companyId,
            @Param("actividadIds") java.util.Collection<Long> actividadIds,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);
}
