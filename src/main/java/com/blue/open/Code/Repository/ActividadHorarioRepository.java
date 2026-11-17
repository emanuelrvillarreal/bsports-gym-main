package com.blue.open.Code.Repository;

import com.blue.open.Code.Entity.Db.ActividadHorario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

/**
 * Acceso a los horarios de las actividades. Los horarios dados de baja quedan con status != 0
 * para no romper inscripciones y asistencias historicas.
 */
public interface ActividadHorarioRepository extends JpaRepository<ActividadHorario, Long> {

    @Query("""
            SELECT h FROM ActividadHorario h
            WHERE h.actividad.id = :actividadId AND h.status = 0
            ORDER BY h.diaSemana, h.horaInicio
            """)
    List<ActividadHorario> findActivosByActividadId(@Param("actividadId") Long actividadId);

    @Query("""
            SELECT h FROM ActividadHorario h
            WHERE h.actividad.id IN :actividadIds AND h.status = 0
            ORDER BY h.diaSemana, h.horaInicio
            """)
    List<ActividadHorario> findActivosByActividadIdIn(@Param("actividadIds") Collection<Long> actividadIds);

    /**
     * Inscripciones activas por horario: [horarioId, cantidad]. Es lo que consume el cupo.
     */
    @Query("""
            SELECT h.id, COUNT(i.id) FROM InscripcionActividad i JOIN i.horarios h
            WHERE h.id IN :horarioIds AND i.status = 0
            GROUP BY h.id
            """)
    List<Object[]> countInscripcionesActivasByHorarioIdIn(@Param("horarioIds") Collection<Long> horarioIds);
}
