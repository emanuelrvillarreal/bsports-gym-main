package com.blue.open.Code.Repository;

import com.blue.open.Code.Entity.Db.ActividadProfesor;
import com.blue.open.Code.Entity.Db.ActividadProfesorId;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ActividadProfesorRepository extends CrudRepository<ActividadProfesor, ActividadProfesorId> {

    Optional<ActividadProfesor> findByActividadIdAndProfesorId(Long actividadId, Long profesorId);

    boolean existsByActividadIdAndProfesorIdAndStatus(Long actividadId, Long profesorId, Integer status);

    @Query("""
            SELECT ap FROM ActividadProfesor ap
            WHERE ap.actividadId = :actividadId
              AND ap.status = 0
            """)
    List<ActividadProfesor> findActivasByActividadId(@Param("actividadId") Long actividadId);

    @Query("""
            SELECT ap FROM ActividadProfesor ap
            WHERE ap.actividadId IN :actividadIds
              AND ap.status = 0
            """)
    List<ActividadProfesor> findActivasByActividadIdIn(@Param("actividadIds") List<Long> actividadIds);

    @Modifying
    @Query("""
            UPDATE ActividadProfesor ap
            SET ap.status = -1
            WHERE ap.profesorId = :profesorId
              AND ap.status = 0
            """)
    void bajaLogicaByProfesorId(@Param("profesorId") Long profesorId);

    @Modifying
    @Query("""
            UPDATE ActividadProfesor ap
            SET ap.status = -1
            WHERE ap.actividadId = :actividadId
              AND ap.status = 0
            """)
    void bajaLogicaByActividadId(@Param("actividadId") Long actividadId);

    @Modifying
    @Query("""
            UPDATE ActividadProfesor ap
            SET ap.status = -1
            WHERE ap.actividadId = :actividadId
              AND ap.profesorId = :profesorId
              AND ap.status = 0
            """)
    void bajaLogicaByActividadIdAndProfesorId(
            @Param("actividadId") Long actividadId,
            @Param("profesorId") Long profesorId);

    @Query("""
            SELECT ap.actividadId FROM ActividadProfesor ap
            WHERE ap.profesorId = :profesorId
              AND ap.status = 0
            """)
    List<Long> findActividadIdsByProfesorId(@Param("profesorId") Long profesorId);
}
