package com.blue.open.Code.Service.Membresias;

import com.blue.open.Code.Entity.DTO.MembresiaDTO;
import com.blue.open.Code.Entity.Db.Actividad;
import com.blue.open.Code.Entity.Db.Amenity;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.Cuota;
import com.blue.open.Code.Entity.Db.Member;
import com.blue.open.Code.Entity.Db.Membresia;
import com.blue.open.Code.Entity.Db.MembresiaAcceso;
import com.blue.open.Code.Entity.Enums.MembresiaEstadoEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Control de pases en el ingreso por QR. Se engancha en el registro de accesos existente
 * ({@code AccesoClubRegistroService}): no hay un segundo sistema de QR.
 * <ul>
 *   <li>Si el socio tiene un pase que habilita el ingreso, se registra (y se descuenta un ingreso si el pase
 *   los limita).</li>
 *   <li>Si no lo tiene: con "exigir pase" activado se rechaza e informa el motivo; si no, entra igual (el pase es
 *   opcional en ese club) y recepción ve una advertencia.</li>
 * </ul>
 * Los rechazos se guardan en una transacción aparte para que queden aunque el ingreso se aborte.
 */
@Service
public class MembresiaAccesoService {

    @Autowired
    private MembresiaSoporte soporte;

    @Autowired
    private MembresiaAccesoRegistro registro;

    @Transactional(propagation = Propagation.MANDATORY)
    public MembresiaDTO.EvaluacionAcceso evaluar(Company company, Member member, Actividad actividad, Amenity amenity) {
        LocalDateTime ahora = soporte.ahora();
        boolean exigir = soporte.config(company).isExigirPaseParaIngresar();
        List<Membresia> activas = soporte.deSocio(member.getId(), company.getId()).stream()
                .filter(m -> m.getEstado() == MembresiaEstadoEnum.ACTIVA).toList();

        if (activas.isEmpty()) {
            if (!exigir) return new MembresiaDTO.EvaluacionAcceso(true, null, null);
            String motivo = "No tiene un pase vigente";
            registro.registrarRechazo(company.getId(), null, member.getId(), ahora, motivo, actividad, amenity);
            return new MembresiaDTO.EvaluacionAcceso(false, motivo, null);
        }

        Map<Long, Cuota> cuotas = soporte.cuotas(activas);
        // Primero los ilimitados (no gastan ingresos), después el que vence antes.
        List<Membresia> habilitan = activas.stream()
                .filter(m -> MembresiaReglas.motivoRechazo(m, MembresiaSoporte.estadoCobro(cuotas.get(m.getId())),
                        ahora, actividad, amenity) == null)
                .sorted(Comparator.comparing((Membresia m) -> m.getIngresosPermitidos() != null)
                        .thenComparing(Membresia::getFechaFin))
                .toList();

        for (Membresia elegida : habilitan) {
            if (elegida.getIngresosPermitidos() != null && !consumirIngreso(elegida)) {
                continue; // Otro ingreso simultáneo agotó el pase: probar con el siguiente.
            }
            guardar(company, elegida, member, ahora, true, null, actividad, amenity);
            return new MembresiaDTO.EvaluacionAcceso(true, null, advertencia(elegida, ahora));
        }

        // Ninguno habilita: se informa el motivo del pase más reciente.
        Membresia referencia = activas.stream().max(Comparator.comparing(Membresia::getFechaInicio)).orElseThrow();
        String motivo = MembresiaReglas.motivoRechazo(referencia, MembresiaSoporte.estadoCobro(cuotas.get(referencia.getId())),
                ahora, actividad, amenity);
        if (motivo == null) motivo = "El pase " + referencia.getPase().getNombre() + " no tiene ingresos disponibles";
        if (!exigir) {
            return new MembresiaDTO.EvaluacionAcceso(true, null, motivo + ". Ingresa sin pase.");
        }
        registro.registrarRechazo(company.getId(), referencia.getId(), member.getId(), ahora, motivo, actividad, amenity);
        return new MembresiaDTO.EvaluacionAcceso(false, motivo, null);
    }

    /** UPDATE condicional: dos lecturas simultáneas no pueden pasarse del límite. */
    private boolean consumirIngreso(Membresia m) {
        int filas = soporte.em().createQuery("""
                        UPDATE Membresia mb SET mb.ingresosUsados = mb.ingresosUsados + 1
                        WHERE mb.id = :id AND mb.ingresosPermitidos IS NOT NULL
                          AND mb.ingresosUsados < mb.ingresosPermitidos""")
                .setParameter("id", m.getId()).executeUpdate();
        if (filas == 1) {
            soporte.em().refresh(m);
        }
        return filas == 1;
    }

    private void guardar(Company company, Membresia membresia, Member member, LocalDateTime fecha, boolean permitido,
                         String motivo, Actividad actividad, Amenity amenity) {
        MembresiaAcceso acceso = new MembresiaAcceso();
        acceso.setCompany(company);
        acceso.setMembresia(membresia);
        acceso.setMember(member);
        acceso.setFecha(fecha);
        acceso.setPermitido(permitido);
        acceso.setMotivo(motivo);
        acceso.setActividad(actividad);
        acceso.setAmenity(amenity);
        soporte.em().persist(acceso);
    }

    private static String advertencia(Membresia m, LocalDateTime ahora) {
        Integer quedan = m.ingresosDisponibles();
        long dias = ChronoUnit.DAYS.between(ahora.toLocalDate(), m.getFechaFin());
        String pase = "Pase " + m.getPase().getNombre();
        if (quedan != null && quedan <= 3) {
            return pase + ": " + (quedan == 0 ? "fue el último ingreso" : "le quedan " + quedan + " ingreso" + (quedan == 1 ? "" : "s"));
        }
        if (dias <= 3) {
            return pase + (dias == 0 ? ": vence hoy" : ": vence en " + dias + " día" + (dias == 1 ? "" : "s"));
        }
        return null;
    }
}
