package com.blue.open.Code.Service;

import com.blue.open.Code.Auth.PermissionEnum;
import com.blue.open.Code.Entity.DTO.MemberFichaDTO;
import com.blue.open.Code.Entity.DTO.MembresiaDTO;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.Cuota;
import com.blue.open.Code.Entity.Db.InscripcionActividad;
import com.blue.open.Code.Entity.Db.Member;
import com.blue.open.Code.Entity.Db.Membresia;
import com.blue.open.Code.Entity.Db.Movimiento;
import com.blue.open.Code.Entity.Enums.CuotaEstadoEnum;
import com.blue.open.Code.Entity.Enums.MovimientoEstadoEnum;
import com.blue.open.Code.Repository.InscripcionActividadRepository;
import com.blue.open.Code.Service.Membresias.MembresiaCoberturaActividades;
import com.blue.open.Code.Service.Membresias.MembresiaSoporte;
import com.blue.open.Core.Service.Auth.AuthService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Ficha del socio para el listado de Socios: estado de cuenta, actividades (activas y dadas de baja), membresías
 * y últimas cuotas. Solo lectura; los datos personales ya vienen en la fila del listado.
 */
@Service
public class MemberFichaService {

    private static final int CUOTAS_RECIENTES = 15;

    @Autowired
    private AuthService authService;

    @Autowired
    private MemberAuthService memberAuthService;

    @Autowired
    private InscripcionActividadRepository inscripcionActividadRepository;

    @Autowired
    private MembresiaSoporte membresiaSoporte;

    @Autowired
    private MembresiaCoberturaActividades coberturaPases;

    @PersistenceContext
    private EntityManager em;

    @Transactional(readOnly = true)
    public MemberFichaDTO ficha(String memberUuid) {
        authService.valid(List.of(PermissionEnum.MEMBER_VIEW));
        Company company = authService.getCurrentCompanyOrThrow();
        Member member = memberAuthService.resolveMemberByUuid(memberUuid, company.getId());
        LocalDate hoy = membresiaSoporte.hoy();

        List<Cuota> cuotas = em.createQuery("""
                        SELECT c FROM Cuota c LEFT JOIN FETCH c.actividad
                        WHERE c.company.id = :companyId AND c.persona.id = :memberId AND c.estado <> :anulada
                        ORDER BY c.vencimiento DESC, c.id DESC""", Cuota.class)
                .setParameter("companyId", company.getId())
                .setParameter("memberId", member.getId())
                .setParameter("anulada", CuotaEstadoEnum.ANULADA)
                .getResultList();
        Map<Long, Movimiento> cobros = cobros(cuotas);

        MemberFichaDTO dto = new MemberFichaDTO();
        dto.setCuenta(cuenta(cuotas, cobros, hoy));
        dto.setCuotas(cuotas.stream().limit(CUOTAS_RECIENTES).map(c -> item(c, cobros, hoy)).toList());
        dto.setActividades(actividades(company, member, cuotas, cobros, hoy));
        dto.setMembresias(membresias(member, company, hoy));
        return dto;
    }

    private MemberFichaDTO.Cuenta cuenta(List<Cuota> cuotas, Map<Long, Movimiento> cobros, LocalDate hoy) {
        MemberFichaDTO.Cuenta cuenta = new MemberFichaDTO.Cuenta();
        Cuota proxima = null;
        for (Cuota c : cuotas) {
            if (!c.estaImpaga()) continue;
            if (vencida(c, hoy)) {
                cuenta.setCuotasVencidas(cuenta.getCuotasVencidas() + 1);
                cuenta.setDeudaVencida(cuenta.getDeudaVencida().add(c.montoFinal()));
            } else {
                cuenta.setCuotasPorVencer(cuenta.getCuotasPorVencer() + 1);
                cuenta.setPorVencer(cuenta.getPorVencer().add(c.montoFinal()));
                if (proxima == null || (c.getVencimiento() != null && proxima.getVencimiento() != null
                        && c.getVencimiento().isBefore(proxima.getVencimiento()))) {
                    proxima = c;
                }
            }
        }
        cuenta.setAlDia(cuenta.getCuotasVencidas() == 0);
        if (proxima != null) cuenta.setProximoVencimiento(item(proxima, cobros, hoy));
        return cuenta;
    }

    private List<MemberFichaDTO.Actividad> actividades(Company company, Member member, List<Cuota> cuotas,
                                                       Map<Long, Movimiento> cobros, LocalDate hoy) {
        List<InscripcionActividad> historial = inscripcionActividadRepository
                .findHistorialByMemberId(company.getId(), member.getId());
        return historial.stream()
                .sorted(Comparator.comparing((InscripcionActividad i) -> activa(i) ? 0 : 1)
                        .thenComparing(InscripcionActividad::getFechaInscripcion,
                                Comparator.nullsLast(Comparator.reverseOrder())))
                .map(i -> {
                    MemberFichaDTO.Actividad a = new MemberFichaDTO.Actividad();
                    boolean activa = activa(i);
                    a.setInscripcionUuid(i.getUuid());
                    a.setActividad(i.getActividad().getNombre());
                    a.setActiva(activa);
                    a.setFechaInscripcion(i.getFechaInscripcion());
                    a.setFechaBaja(i.getFechaBaja());
                    a.setMotivoBaja(i.getMotivoBaja());
                    if (activa) {
                        a.setProximaCuota(i.getProximaGeneracion());
                        a.setCubiertaPorPase(coberturaPases.cubre(member.getId(), i.getActividad().getId(), hoy));
                    }
                    Long actividadId = i.getActividad().getId();
                    cuotas.stream()
                            .filter(c -> c.getActividad() != null && Objects.equals(c.getActividad().getId(), actividadId))
                            .findFirst()
                            .ifPresent(c -> a.setUltimaCuota(item(c, cobros, hoy)));
                    return a;
                })
                .toList();
    }

    private static boolean activa(InscripcionActividad i) {
        return Objects.equals(i.getStatus(), 0) && i.getFechaBaja() == null;
    }

    private List<MembresiaDTO.Membresia> membresias(Member member, Company company, LocalDate hoy) {
        List<Membresia> lista = membresiaSoporte.deSocio(member.getId(), company.getId());
        Map<Long, Cuota> cuotas = membresiaSoporte.cuotas(lista);
        Set<Long> renovadas = membresiaSoporte.conSucesora(lista);
        return lista.stream()
                .map(m -> membresiaSoporte.membresia(m, cuotas.get(m.getId()), renovadas.contains(m.getId()),
                        hoy, false, false))
                .toList();
    }

    private Map<Long, Movimiento> cobros(List<Cuota> cuotas) {
        Map<Long, Movimiento> porCuota = new HashMap<>();
        List<Long> pagadas = cuotas.stream()
                .filter(c -> c.getEstado() == CuotaEstadoEnum.PAGADA)
                .map(Cuota::getId)
                .toList();
        if (pagadas.isEmpty()) return porCuota;
        em.createQuery("""
                        SELECT mv FROM Movimiento mv
                        WHERE mv.cuota.id IN :ids AND mv.estado = :activo""", Movimiento.class)
                .setParameter("ids", pagadas)
                .setParameter("activo", MovimientoEstadoEnum.ACTIVO)
                .getResultList()
                .forEach(mv -> porCuota.putIfAbsent(mv.getCuota().getId(), mv));
        return porCuota;
    }

    private static boolean vencida(Cuota c, LocalDate hoy) {
        return c.estaImpaga() && (c.getEstado() == CuotaEstadoEnum.VENCIDA
                || (c.getVencimiento() != null && c.getVencimiento().isBefore(hoy)));
    }

    private static MemberFichaDTO.CuotaItem item(Cuota c, Map<Long, Movimiento> cobros, LocalDate hoy) {
        MemberFichaDTO.CuotaItem item = new MemberFichaDTO.CuotaItem();
        item.setUuid(c.getUuid());
        item.setConcepto(c.getConcepto());
        item.setTipo(c.getTipo());
        item.setEstado(c.getEstado());
        item.setMonto(c.montoFinal());
        item.setVencimiento(c.getVencimiento());
        item.setVencida(vencida(c, hoy));
        Movimiento cobro = cobros.get(c.getId());
        if (cobro != null) {
            item.setFechaPago(cobro.getFecha());
            item.setMedioPago(cobro.getMedioPago());
        }
        return item;
    }
}
