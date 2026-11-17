package com.blue.open.Code.Service.Dashboard;

import com.blue.open.Code.Auth.PermissionEnum;
import com.blue.open.Code.Entity.DTO.AsistenciaEstadisticasDTO;
import com.blue.open.Code.Entity.DTO.AsistenciaEstadisticasRequestDTO;
import com.blue.open.Code.Entity.DTO.AsistenciaMesDTO;
import com.blue.open.Code.Entity.DTO.DashboardDTO;
import com.blue.open.Code.Entity.DTO.ReporteEconomicoDTO;
import com.blue.open.Code.Entity.DTO.ReporteEconomicoRequestDTO;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Enums.MemberEstadoEnum;
import com.blue.open.Code.Service.Actividades.AsistenciaEstadisticasService;
import com.blue.open.Code.Service.Tesoreria.ReporteEconomicoQueryService;
import com.blue.open.Core.Auth.BaseRoleEnum;
import com.blue.open.Core.Service.Auth.AuthService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Indicadores del panel de administracion, siempre del club del header.
 *
 * - resumen(): contrato operativo que ya usaba el panel de recepcion (socios, actividades, accesos,
 *   reservas); el bloque financiero solo va para ADMIN.
 * - general(): dashboard institucional del ADMIN. Reutiliza los calculos existentes: asistencia
 *   (AsistenciaEstadisticasService) y economia (ReporteEconomicoQueryService), asi los numeros
 *   coinciden con las pantallas de detalle.
 */
@Service
@Slf4j
public class DashboardService {

    @PersistenceContext
    private EntityManager em;

    @Autowired
    private AuthService authService;

    @Autowired
    private AsistenciaEstadisticasService asistenciaService;

    @Autowired
    private ReporteEconomicoQueryService reporteService;

    // ================================================================ Resumen operativo

    @Transactional(readOnly = true)
    public DashboardDTO.Resumen resumen() {
        authService.valid(List.of(PermissionEnum.DASHBOARD_VIEW));
        Company company = authService.getCurrentCompanyOrThrow();
        Long c = company.getId();
        DashboardDTO.Resumen r = new DashboardDTO.Resumen();

        Map<MemberEstadoEnum, Long> estados = sociosPorEstado(c);
        r.getSocios().setActivos(estados.getOrDefault(MemberEstadoEnum.ACTIVO, 0L));
        r.getSocios().setMorosos(estados.getOrDefault(MemberEstadoEnum.MOROSO, 0L));
        r.getSocios().setInhabilitados(estados.getOrDefault(MemberEstadoEnum.INHABILITADO, 0L));

        r.getActividades().setActivas(contar("SELECT COUNT(a.id) FROM Actividad a WHERE a.company.id = :c AND a.status = 0", c));
        long inscripciones = contar("SELECT COUNT(i.id) FROM InscripcionActividad i WHERE i.company.id = :c AND i.status = 0 AND i.actividad.status = 0", c);
        r.getActividades().setInscripciones(inscripciones);
        long cupos = contar("SELECT COALESCE(SUM(h.cupoMaximo), 0) FROM ActividadHorario h WHERE h.actividad.company.id = :c AND h.status = 0"
                + " AND h.actividad.status = 0", c);
        long ocupados = contar("SELECT COUNT(h.id) FROM InscripcionActividad i JOIN i.horarios h WHERE i.company.id = :c AND i.status = 0"
                + " AND i.actividad.status = 0 AND h.status = 0", c);
        r.getActividades().setCuposDisponibles(Math.max(0, cupos - ocupados));
        r.getActividades().setProximosEventos(em.createQuery("SELECT COUNT(e.id) FROM EventoActividad e WHERE e.company.id = :c AND e.status = 0"
                        + " AND e.inicio >= :ahora AND e.estado = com.blue.open.Code.Entity.Enums.EventoActividadEstadoEnum.CONFIRMADO", Long.class)
                .setParameter("c", c).setParameter("ahora", LocalDateTime.now()).getSingleResult());

        r.getAccesos().setPresentes(contar("SELECT COUNT(p.id) FROM ClubPresencia p WHERE p.company.id = :c AND p.presente = true", c));
        r.getReservas().setProximas(em.createQuery("SELECT COUNT(r.id) FROM Reserva r WHERE r.company.id = :c AND r.status = 0 AND r.slot.inicio >= :ahora"
                        + " AND r.estado IN (com.blue.open.Code.Entity.Enums.ReservaEstadoEnum.CONFIRMADA, com.blue.open.Code.Entity.Enums.ReservaEstadoEnum.PENDIENTE_SENA)", Long.class)
                .setParameter("c", c).setParameter("ahora", LocalDateTime.now()).getSingleResult());

        if (esAdmin()) {
            LocalDate hoy = LocalDate.now();
            ReporteEconomicoDTO.Resumen mes = economia(company, hoy.withDayOfMonth(1), hoy);
            DashboardDTO.Financiero f = new DashboardDTO.Financiero();
            f.setIngresos(mes.getTotalIngresos());
            f.setEgresos(mes.getTotalEgresos());
            f.setBalance(mes.getBalance());
            f.setDeudaVencida(deudaVencida(c));
            r.setFinanciero(f);
        }
        return r;
    }

    // ================================================================ Dashboard general

    @Transactional(readOnly = true)
    public DashboardDTO.General general() {
        // * Solo ADMIN: TREASURY_REPORTS no lo tiene recepcion (la lista de valid() es "cualquiera de")
        authService.valid(List.of(PermissionEnum.TREASURY_REPORTS));
        Company company = authService.getCurrentCompanyOrThrow();
        Long c = company.getId();
        LocalDate hoy = LocalDate.now();
        LocalDate inicioMes = hoy.withDayOfMonth(1);
        DashboardDTO.General g = new DashboardDTO.General();

        // ---- Socios
        Map<MemberEstadoEnum, Long> estados = sociosPorEstado(c);
        g.setSociosActivos(estados.getOrDefault(MemberEstadoEnum.ACTIVO, 0L));
        g.setSociosMorosos(estados.getOrDefault(MemberEstadoEnum.MOROSO, 0L));
        g.setSociosNuevosMes(em.createQuery("SELECT COUNT(m.id) FROM Member m WHERE m.company.id = :c AND m.status = 0 AND m.fechaAlta >= :d", Long.class)
                .setParameter("c", c).setParameter("d", inicioMes).getSingleResult());
        g.setSociosBajasMes(bajasDesde(c, inicioMes.atStartOfDay()));
        for (Object[] f : em.createQuery("SELECT COALESCE(cat.nombre, 'Sin categoría'), COUNT(m.id) FROM Member m LEFT JOIN m.categoria cat"
                        + " WHERE m.company.id = :c AND m.status = 0 AND m.estado <> com.blue.open.Code.Entity.Enums.MemberEstadoEnum.INACTIVO"
                        + " GROUP BY cat.nombre ORDER BY COUNT(m.id) DESC", Object[].class).setParameter("c", c).getResultList()) {
            g.getSociosPorCategoria().add(new DashboardDTO.Item(null, (String) f[0], ((Number) f[1]).longValue(), 0));
        }

        // ---- Actividades y demanda
        g.setActividades(contar("SELECT COUNT(a.id) FROM Actividad a WHERE a.company.id = :c AND a.status = 0", c));
        g.setInscriptos(contar("SELECT COUNT(DISTINCT i.member.id) FROM InscripcionActividad i WHERE i.company.id = :c AND i.status = 0 AND i.actividad.status = 0", c));
        g.setProfesoresActivos(contar("SELECT COUNT(p.id) FROM Profesor p WHERE p.company.id = :c AND p.status = 0"
                + " AND p.estado = com.blue.open.Code.Entity.Enums.ProfesorEstadoEnum.ACTIVO", c));
        Map<String, Long> cupos = new LinkedHashMap<>();
        for (Object[] f : em.createQuery("SELECT h.actividad.uuid, SUM(h.cupoMaximo) FROM ActividadHorario h WHERE h.actividad.company.id = :c"
                + " AND h.status = 0 AND h.actividad.status = 0 GROUP BY h.actividad.uuid", Object[].class).setParameter("c", c).getResultList()) {
            cupos.put((String) f[0], ((Number) f[1]).longValue());
        }
        for (Object[] f : em.createQuery("SELECT a.uuid, a.nombre, COUNT(i.id) FROM Actividad a LEFT JOIN InscripcionActividad i ON i.actividad = a AND i.status = 0"
                + " WHERE a.company.id = :c AND a.status = 0 GROUP BY a.uuid, a.nombre ORDER BY COUNT(i.id) DESC", Object[].class).setParameter("c", c).getResultList()) {
            g.getDemandaActividades().add(new DashboardDTO.Item((String) f[0], (String) f[1], ((Number) f[2]).longValue(), cupos.getOrDefault((String) f[0], 0L)));
        }

        // ---- Asistencia: ultimos 3 meses, con el mismo calculo que Actividades › Asistencia
        Map<String, Double> asistenciaMensual = new LinkedHashMap<>();
        try {
            AsistenciaEstadisticasRequestDTO pedido = new AsistenciaEstadisticasRequestDTO();
            pedido.setDesde(inicioMes.minusMonths(11));
            pedido.setHasta(hoy);
            pedido.setSize(1);
            AsistenciaEstadisticasDTO asistencia = asistenciaService.estadisticasInstitucionales(pedido);
            for (AsistenciaMesDTO mes : asistencia.getEvolucionMensual()) {
                asistenciaMensual.put(mes.getPeriodo(), mes.getPorcentaje());
            }
            AsistenciaEstadisticasRequestDTO reciente = new AsistenciaEstadisticasRequestDTO();
            reciente.setDesde(inicioMes.minusMonths(2));
            reciente.setHasta(hoy);
            reciente.setSize(1);
            AsistenciaEstadisticasDTO.Resumen resumen = asistenciaService.estadisticasInstitucionales(reciente).getResumen();
            g.setAsistenciaPromedio(resumen.getPromedioAsistencia());
            g.setBajaAsistencia(resumen.getBajaAsistencia());
            g.setDejaronDeAsistir(resumen.getDejaronDeAsistir());
        } catch (RuntimeException e) {
            // ! Un error en asistencia no debe dejar sin dashboard: se informa sin ese bloque
            log.warn("DashboardService | asistencia no disponible | companyId={}", c, e);
        }

        // ---- Deportivo
        g.setEquiposActivos(contar("SELECT COUNT(e.id) FROM EquipoDeportivo e WHERE e.company.id = :c AND e.status = 0 AND e.activo = true", c));
        g.setJugadoresEnPlanteles(contar("SELECT COUNT(DISTINCT j.member.id) FROM EquipoJugador j WHERE j.equipo.company.id = :c AND j.status = 0"
                + " AND j.fechaBaja IS NULL AND j.equipo.status = 0 AND j.equipo.activo = true", c));
        g.setTorneosEnCurso(contar("SELECT COUNT(t.id) FROM Torneo t WHERE t.company.id = :c AND t.status = 0"
                + " AND t.estado = com.blue.open.Code.Entity.Enums.TorneoEstadoEnum.EN_CURSO", c));
        for (Object[] f : em.createQuery("SELECT e.disciplina, COUNT(DISTINCT j.member.id) FROM EquipoJugador j JOIN j.equipo e WHERE e.company.id = :c"
                + " AND e.status = 0 AND e.activo = true AND j.status = 0 AND j.fechaBaja IS NULL GROUP BY e.disciplina ORDER BY COUNT(DISTINCT j.member.id) DESC",
                Object[].class).setParameter("c", c).getResultList()) {
            g.getParticipacionPorDisciplina().add(new DashboardDTO.Item(null, (String) f[0], ((Number) f[1]).longValue(), 0));
        }

        // ---- Economia: mismo calculo que Tesoreria › Reportes
        ReporteEconomicoDTO.Resumen mes = economia(company, inicioMes, hoy);
        g.setIngresosMes(mes.getTotalIngresos());
        g.setEgresosMes(mes.getTotalEgresos());
        g.setBalanceMes(mes.getBalance());
        ReporteEconomicoRequestDTO anio = new ReporteEconomicoRequestDTO();
        anio.setDesde(inicioMes.minusMonths(11));
        anio.setHasta(hoy);
        ReporteEconomicoDTO economiaAnual = reporteService.resumen(anio, c);
        ReporteEconomicoDTO.Resumen anioCalendario = economia(company, hoy.withDayOfYear(1), hoy);
        g.setIngresosAnio(anioCalendario.getTotalIngresos());
        g.setEgresosAnio(anioCalendario.getTotalEgresos());
        g.setDeudaVencida(deudaVencida(c));

        // ---- Alertas operativas
        g.setMantenimientosVencidos(mantenimientosVencidos(c, hoy));
        g.setEvaluacionesVencidas(evaluacionesVencidas(c, hoy));

        // ---- Evolucion de los ultimos 12 meses
        Map<String, DashboardDTO.Mes> evolucion = new LinkedHashMap<>();
        for (int i = 11; i >= 0; i--) {
            String periodo = YearMonth.from(hoy).minusMonths(i).toString();
            evolucion.put(periodo, new DashboardDTO.Mes(periodo, 0, 0, 0, BigDecimal.ZERO, BigDecimal.ZERO, asistenciaMensual.get(periodo)));
        }
        LocalDate desde12 = inicioMes.minusMonths(11);
        for (Object[] f : em.createQuery("SELECT YEAR(m.fechaAlta), MONTH(m.fechaAlta), COUNT(m.id) FROM Member m WHERE m.company.id = :c AND m.status = 0"
                + " AND m.fechaAlta >= :d GROUP BY YEAR(m.fechaAlta), MONTH(m.fechaAlta)", Object[].class)
                .setParameter("c", c).setParameter("d", desde12).getResultList()) {
            DashboardDTO.Mes m = evolucion.get(periodo(f[0], f[1]));
            if (m != null) m.setAltas(((Number) f[2]).longValue());
        }
        for (Object[] f : em.createQuery("SELECT YEAR(i.fechaInscripcion), MONTH(i.fechaInscripcion), COUNT(i.id) FROM InscripcionActividad i"
                + " WHERE i.company.id = :c AND i.fechaInscripcion >= :d AND (i.status = 0 OR i.fechaBaja IS NOT NULL)"
                + " GROUP BY YEAR(i.fechaInscripcion), MONTH(i.fechaInscripcion)", Object[].class)
                .setParameter("c", c).setParameter("d", desde12).getResultList()) {
            DashboardDTO.Mes m = evolucion.get(periodo(f[0], f[1]));
            if (m != null) m.setInscripciones(((Number) f[2]).longValue());
        }
        for (Object[] f : em.createQuery("SELECT YEAR(m.dateUpdated), MONTH(m.dateUpdated), COUNT(m.id) FROM Member m WHERE m.company.id = :c"
                + " AND m.estado = com.blue.open.Code.Entity.Enums.MemberEstadoEnum.INACTIVO AND m.dateUpdated >= :d"
                + " GROUP BY YEAR(m.dateUpdated), MONTH(m.dateUpdated)", Object[].class)
                .setParameter("c", c).setParameter("d", Timestamp.valueOf(desde12.atStartOfDay())).getResultList()) {
            DashboardDTO.Mes m = evolucion.get(periodo(f[0], f[1]));
            if (m != null) m.setBajas(((Number) f[2]).longValue());
        }
        for (ReporteEconomicoDTO.Grupo grupo : economiaAnual.getMensual()) {
            DashboardDTO.Mes m = evolucion.get(grupo.getClave());
            if (m != null) {
                m.setIngresos(grupo.getIngresos());
                m.setEgresos(grupo.getEgresos());
            }
        }
        g.setEvolucion(List.copyOf(evolucion.values()));
        return g;
    }

    // ================================================================ Apoyo

    private Map<MemberEstadoEnum, Long> sociosPorEstado(Long c) {
        Map<MemberEstadoEnum, Long> estados = new LinkedHashMap<>();
        for (Object[] f : em.createQuery("SELECT m.estado, COUNT(m.id) FROM Member m WHERE m.company.id = :c AND m.status = 0 GROUP BY m.estado", Object[].class)
                .setParameter("c", c).getResultList()) {
            estados.put((MemberEstadoEnum) f[0], ((Number) f[1]).longValue());
        }
        return estados;
    }

    /**
     * Socios que pasaron a INACTIVO desde la fecha. No hay fecha de baja propia en members: se usa la
     * ultima modificacion del socio inactivo como aproximacion.
     */
    private long bajasDesde(Long c, LocalDateTime desde) {
        return em.createQuery("SELECT COUNT(m.id) FROM Member m WHERE m.company.id = :c AND m.estado = com.blue.open.Code.Entity.Enums.MemberEstadoEnum.INACTIVO"
                        + " AND m.dateUpdated >= :d", Long.class)
                .setParameter("c", c).setParameter("d", Timestamp.valueOf(desde)).getSingleResult();
    }

    private BigDecimal deudaVencida(Long c) {
        BigDecimal monto = em.createQuery("SELECT COALESCE(SUM(q.monto + q.recargo), 0) FROM Cuota q WHERE q.company.id = :c AND q.status = 0"
                        + " AND q.estado = com.blue.open.Code.Entity.Enums.CuotaEstadoEnum.VENCIDA", BigDecimal.class)
                .setParameter("c", c).getSingleResult();
        BigDecimal descuentos = em.createQuery("SELECT COALESCE(SUM(d.valor), 0) FROM CuotaDescuento d WHERE d.cuota.company.id = :c AND d.status = 0"
                        + " AND d.cuota.status = 0 AND d.cuota.estado = com.blue.open.Code.Entity.Enums.CuotaEstadoEnum.VENCIDA", BigDecimal.class)
                .setParameter("c", c).getSingleResult();
        return monto.subtract(descuentos).max(BigDecimal.ZERO);
    }

    private long mantenimientosVencidos(Long c, LocalDate hoy) {
        // * Por equipo cuenta el proximo previsto de su ultimo mantenimiento
        List<Object[]> filas = em.createQuery("SELECT m.equipamiento.id, m.proximoMantenimiento FROM EquipamientoMantenimiento m"
                        + " WHERE m.equipamiento.company.id = :c AND m.status = 0 AND m.equipamiento.status = 0"
                        + " AND m.equipamiento.estado <> com.blue.open.Code.Entity.Enums.EquipamientoEstadoEnum.BAJA ORDER BY m.fecha, m.id", Object[].class)
                .setParameter("c", c).getResultList();
        Map<Long, LocalDate> ultimo = new LinkedHashMap<>();
        for (Object[] f : filas) ultimo.put((Long) f[0], (LocalDate) f[1]);
        return ultimo.values().stream().filter(d -> d != null && d.isBefore(hoy)).count();
    }

    private long evaluacionesVencidas(Long c, LocalDate hoy) {
        List<Object[]> filas = em.createQuery("SELECT e.member.id, e.proximaEvaluacion FROM EvaluacionFisica e WHERE e.company.id = :c AND e.status = 0"
                + " ORDER BY e.fecha, e.id", Object[].class).setParameter("c", c).getResultList();
        Map<Long, LocalDate> ultimo = new LinkedHashMap<>();
        for (Object[] f : filas) ultimo.put((Long) f[0], (LocalDate) f[1]);
        return ultimo.values().stream().filter(d -> d != null && d.isBefore(hoy)).count();
    }

    private ReporteEconomicoDTO.Resumen economia(Company company, LocalDate desde, LocalDate hasta) {
        ReporteEconomicoRequestDTO pedido = new ReporteEconomicoRequestDTO();
        pedido.setDesde(desde);
        pedido.setHasta(hasta);
        return reporteService.resumen(pedido, company.getId()).getResumen();
    }

    private long contar(String jpql, Long companyId) {
        Object valor = em.createQuery(jpql).setParameter("c", companyId).getSingleResult();
        return valor == null ? 0 : ((Number) valor).longValue();
    }

    private boolean esAdmin() {
        return authService.validBoolean(List.of(BaseRoleEnum.ADMIN, BaseRoleEnum.SYSADMIN));
    }

    private static String periodo(Object anio, Object mes) {
        return String.format(Locale.ROOT, "%04d-%02d", ((Number) anio).intValue(), ((Number) mes).intValue());
    }
}
