package com.blue.open.Code.Service.Membresias;

import com.blue.open.Code.Entity.Db.Membresia;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * Renovación automática: una semana antes del vencimiento se crea la membresía siguiente con su cuota, así el
 * socio tiene tiempo de pagarla. Cada renovación va en su propia transacción: si una falla (pase desactivado,
 * socio dado de baja) no frena a las demás.
 */
@Service
@Slf4j
public class MembresiaRenovacionService {

    static final int DIAS_ANTICIPACION = 7;

    @Autowired
    private MembresiaSoporte soporte;

    @Autowired
    private MembresiaService membresiaService;

    @Autowired
    private TransactionTemplate transactionTemplate;

    public int renovarPendientes() {
        LocalDate hoy = soporte.hoy();
        List<Long> candidatas = transactionTemplate.execute(status -> {
            List<Membresia> lista = soporte.em().createQuery("""
                            SELECT mb FROM Membresia mb JOIN FETCH mb.pase p
                            WHERE mb.status = 0 AND mb.renovacionAutomatica = true AND p.activo = true
                              AND mb.estado = com.blue.open.Code.Entity.Enums.MembresiaEstadoEnum.ACTIVA
                              AND mb.fechaFin BETWEEN :hoy AND :limite""", Membresia.class)
                    .setParameter("hoy", hoy).setParameter("limite", hoy.plusDays(DIAS_ANTICIPACION)).getResultList();
            Set<Long> renovadas = soporte.conSucesora(lista);
            return lista.stream().map(Membresia::getId).filter(id -> !renovadas.contains(id)).toList();
        });
        int total = 0;
        for (Long id : candidatas == null ? List.<Long>of() : candidatas) {
            try {
                transactionTemplate.executeWithoutResult(status -> {
                    Membresia m = soporte.em().find(Membresia.class, id);
                    membresiaService.renovarInterno(m.getCompany(), m, List.of(), null);
                });
                total++;
            } catch (RuntimeException e) {
                log.warn("No se pudo renovar la membresía {}: {}", id, e.getMessage());
            }
        }
        return total;
    }
}
