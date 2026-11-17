package com.blue.open.Code.Service.Membresias;

import com.blue.open.Code.Entity.Enums.CuotaEstadoEnum;
import com.blue.open.Code.Entity.Enums.MembresiaEstadoEnum;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Decide si un pase con "incluye la cuota de las actividades" cubre a un socio en una actividad y una fecha.
 * Cubre si la membresía está activa, la fecha cae dentro de su vigencia, el socio es el titular del pase o un
 * integrante cubierto, y el pase no tiene actividades cargadas (todas) o incluye esa actividad. Una membresía
 * cuya cuota fue anulada no cubre. No exige que el pase esté pago: la cuota del pase sigue pendiente aparte.
 */
@Component
public class MembresiaCoberturaActividades {

    @PersistenceContext
    private EntityManager entityManager;

    public boolean cubre(Long memberId, Long actividadId, LocalDate fecha) {
        if (memberId == null || actividadId == null || fecha == null) {
            return false;
        }
        Long cantidad = entityManager.createQuery("""
                SELECT COUNT(m) FROM Membresia m JOIN m.pase p
                WHERE m.estado = :activa
                  AND p.incluyeCuotaActividades = true
                  AND m.fechaInicio <= :fecha AND m.fechaFin >= :fecha
                  AND (m.member.id = :memberId
                       OR EXISTS (SELECT i FROM Membresia m2 JOIN m2.integrantes i WHERE m2 = m AND i.id = :memberId))
                  AND (p.actividades IS EMPTY
                       OR EXISTS (SELECT a FROM Pase p2 JOIN p2.actividades a WHERE p2 = p AND a.id = :actividadId))
                  AND NOT EXISTS (SELECT c FROM Cuota c WHERE c.membresia = m AND c.estado = :anulada)
                """, Long.class)
                .setParameter("activa", MembresiaEstadoEnum.ACTIVA)
                .setParameter("fecha", fecha)
                .setParameter("memberId", memberId)
                .setParameter("actividadId", actividadId)
                .setParameter("anulada", CuotaEstadoEnum.ANULADA)
                .getSingleResult();
        return cantidad != null && cantidad > 0;
    }
}
