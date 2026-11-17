package com.blue.open.Code.Service.Tesoreria;

import com.blue.open.Code.Entity.DTO.ReporteComparacionDTO;
import com.blue.open.Code.Entity.DTO.ReporteCuotasDTO;
import com.blue.open.Code.Entity.DTO.ReporteEconomicoDTO;
import com.blue.open.Code.Entity.DTO.ReporteEconomicoRequestDTO;
import com.blue.open.Code.Entity.DTO.ReporteMovimientoRowDTO;
import com.blue.open.Code.Entity.Enums.CuotaEstadoEnum;
import com.blue.open.Code.Entity.Enums.CuotaTipoEnum;
import com.blue.open.Code.Entity.Enums.MedioPagoEnum;
import com.blue.open.Code.Entity.Enums.MovimientoEstadoEnum;
import com.blue.open.Code.Entity.Enums.MovimientoTipoEnum;
import com.blue.open.Core.Exceptions.CustomException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.Consumer;

/**
 * Consultas del reporte economico sobre {@code Movimiento}, el libro de caja unico del club.
 *
 * Todo sale de movimientos reales: lo que se cobro o pago. Una cuota pendiente no es un movimiento,
 * asi que nunca suma como ingreso; una reserva con sena solo suma lo percibido; los descuentos ya
 * estan restados en el importe cobrado. Por defecto se excluyen los movimientos anulados.
 *
 * Un unico armador de filtros ({@link Filtro}) alimenta la pagina, los resumenes, la comparacion y
 * la exportacion, asi el Excel coincide siempre con lo que se ve en pantalla. Los resumenes se
 * calculan con una consulta agrupada (GROUP BY) y la exportacion recorre los movimientos por lotes,
 * sin cargar todo el historial en memoria. Siempre filtra por el club recibido.
 */
@Service
public class ReporteEconomicoQueryService {

    /** Tope de filas por exportacion: mas que esto requiere acotar los filtros. */
    public static final int MAX_FILAS_EXPORTACION = 100_000;
    private static final int LOTE_EXPORTACION = 2_000;

    private static final String[] MESES = {"Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio", "Julio",
            "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"};

    static final String CUOTA_SOCIAL = "CUOTA_SOCIAL";
    static final String CUOTA_ACTIVIDAD = "CUOTA_ACTIVIDAD";
    static final String PASE = "PASE";
    static final String AMENITY = "AMENITY";
    static final String EVENTO = "EVENTO";
    static final String MANUAL = "MANUAL";

    private static final String JOINS = """
             FROM Movimiento m
             LEFT JOIN m.cuota c
             LEFT JOIN c.actividad act
             LEFT JOIN m.persona p
             LEFT JOIN p.user pu
             LEFT JOIN p.categoria pc
             LEFT JOIN m.titular t
             LEFT JOIN t.user tu
             LEFT JOIN m.registradoPor rp
             LEFT JOIN m.reserva r
             LEFT JOIN r.amenity am
             LEFT JOIN m.eventoAcceso ev
            """;

    /** Procedencia del movimiento, derivada de con que esta vinculado. */
    private static final String CATEGORIA = """
            CASE
              WHEN c.id IS NOT NULL AND c.tipo = com.blue.open.Code.Entity.Enums.CuotaTipoEnum.SOCIAL THEN 'CUOTA_SOCIAL'
              WHEN c.id IS NOT NULL AND c.tipo = com.blue.open.Code.Entity.Enums.CuotaTipoEnum.PASE THEN 'PASE'
              WHEN c.id IS NOT NULL THEN 'CUOTA_ACTIVIDAD'
              WHEN r.id IS NOT NULL THEN 'AMENITY'
              WHEN ev.id IS NOT NULL THEN 'EVENTO'
              ELSE 'MANUAL'
            END""";

    @PersistenceContext
    private EntityManager entityManager;

    // ---------------------------------------------------------------- Reporte en pantalla

    @Transactional(readOnly = true)
    public ReporteEconomicoDTO reporte(ReporteEconomicoRequestDTO request, Long companyId) {
        Filtro filtro = new Filtro(request, companyId);
        ReporteEconomicoDTO dto = agrupar(filtro);

        PageRequest pageable = PageRequest.of(request.getPage() - 1, request.getSize());
        long total = contar(filtro);
        List<ReporteMovimientoRowDTO> filas = filas(filtro, (int) pageable.getOffset(), pageable.getPageSize());
        dto.setMovimientos(new PageImpl<>(filas, pageable, total));
        return dto;
    }

    /** Resumenes y agrupaciones sin la pagina (para el Excel y la comparacion). */
    @Transactional(readOnly = true)
    public ReporteEconomicoDTO resumen(ReporteEconomicoRequestDTO request, Long companyId) {
        return agrupar(new Filtro(request, companyId));
    }

    @Transactional(readOnly = true)
    public long contar(ReporteEconomicoRequestDTO request, Long companyId) {
        return contar(new Filtro(request, companyId));
    }

    /** Recorre todos los movimientos filtrados por lotes, en el mismo orden que la pantalla. */
    @Transactional(readOnly = true)
    public void recorrer(ReporteEconomicoRequestDTO request, Long companyId, Consumer<ReporteMovimientoRowDTO> consumidor) {
        Filtro filtro = new Filtro(request, companyId);
        int desde = 0;
        while (true) {
            List<ReporteMovimientoRowDTO> lote = filas(filtro, desde, LOTE_EXPORTACION);
            lote.forEach(consumidor);
            if (lote.size() < LOTE_EXPORTACION) {
                return;
            }
            desde += LOTE_EXPORTACION;
            entityManager.clear();
        }
    }

    // ---------------------------------------------------------------- Comparacion

    @Transactional(readOnly = true)
    public ReporteComparacionDTO.Respuesta comparar(ReporteComparacionDTO.Request request, Long companyId) {
        ReporteEconomicoRequestDTO base = request.getFiltros() != null ? request.getFiltros() : new ReporteEconomicoRequestDTO();
        ReporteComparacionDTO.Totales a = totales(base, request.getPeriodoA(), companyId);
        ReporteComparacionDTO.Totales b = totales(base, request.getPeriodoB(), companyId);

        ReporteComparacionDTO.Respuesta respuesta = new ReporteComparacionDTO.Respuesta();
        respuesta.setPeriodoA(a);
        respuesta.setPeriodoB(b);
        respuesta.setDiferenciaIngresos(b.getIngresos().subtract(a.getIngresos()));
        respuesta.setDiferenciaEgresos(b.getEgresos().subtract(a.getEgresos()));
        respuesta.setDiferenciaBalance(b.getBalance().subtract(a.getBalance()));
        respuesta.setVariacionIngresos(variacion(a.getIngresos(), b.getIngresos()));
        respuesta.setVariacionEgresos(variacion(a.getEgresos(), b.getEgresos()));
        respuesta.setVariacionBalance(variacion(a.getBalance(), b.getBalance()));
        return respuesta;
    }

    private ReporteComparacionDTO.Totales totales(
            ReporteEconomicoRequestDTO base, ReporteComparacionDTO.Periodo periodo, Long companyId) {
        if (periodo.getDesde().isAfter(periodo.getHasta())) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Cada período tiene que empezar antes de terminar",
                    "ReporteEconomicoQueryService | periodo invalido");
        }
        ReporteEconomicoRequestDTO filtros = copiar(base);
        filtros.setDesde(periodo.getDesde());
        filtros.setHasta(periodo.getHasta());
        ReporteEconomicoDTO.Resumen resumen = agrupar(new Filtro(filtros, companyId)).getResumen();

        ReporteComparacionDTO.Totales totales = new ReporteComparacionDTO.Totales();
        totales.setDesde(periodo.getDesde());
        totales.setHasta(periodo.getHasta());
        totales.setIngresos(resumen.getTotalIngresos());
        totales.setEgresos(resumen.getTotalEgresos());
        totales.setBalance(resumen.getBalance());
        totales.setOperaciones(resumen.getOperaciones());
        return totales;
    }

    // ---------------------------------------------------------------- Reporte de cuotas

    @Transactional(readOnly = true)
    public ReporteCuotasDTO.Respuesta reporteCuotas(ReporteCuotasDTO.Request request, Long companyId) {
        if (request.getDesde() != null && request.getHasta() != null && request.getDesde().isAfter(request.getHasta())) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "La fecha desde no puede ser posterior a la fecha hasta",
                    "ReporteEconomicoQueryService | reporteCuotas | rango invalido");
        }
        CuotaTipoEnum tipo = parsearTipoCuota(request.getTipo());
        ReporteCuotasDTO.Respuesta respuesta = new ReporteCuotasDTO.Respuesta();

        // * Generadas: cuotas emitidas en el periodo, agrupadas por estado. Importe = lista - descuentos.
        StringBuilder where = new StringBuilder(" WHERE c.company.id = :companyId AND c.status = 0");
        Map<String, Object> params = new HashMap<>();
        params.put("companyId", companyId);
        if (request.getDesde() != null) {
            where.append(" AND c.fechaGeneracion >= :desde");
            params.put("desde", request.getDesde());
        }
        if (request.getHasta() != null) {
            where.append(" AND c.fechaGeneracion <= :hasta");
            params.put("hasta", request.getHasta());
        }
        if (tipo != null) {
            where.append(" AND c.tipo = :tipo");
            params.put("tipo", tipo);
        }
        if (tieneTexto(request.getActividadUuid())) {
            where.append(" AND act.uuid = :actividadUuid");
            params.put("actividadUuid", request.getActividadUuid());
        }

        Map<CuotaEstadoEnum, BigDecimal> descuentosPorEstado = new HashMap<>();
        TypedQuery<Object[]> descuentos = entityManager.createQuery(
                "SELECT c.estado, SUM(d.valor) FROM CuotaDescuento d JOIN d.cuota c LEFT JOIN c.actividad act"
                        + where + " AND d.status = 0 GROUP BY c.estado", Object[].class);
        params.forEach(descuentos::setParameter);
        for (Object[] fila : descuentos.getResultList()) {
            descuentosPorEstado.put((CuotaEstadoEnum) fila[0], monto(fila[1]));
        }

        TypedQuery<Object[]> generadas = entityManager.createQuery(
                "SELECT c.estado, COUNT(c.id), SUM(c.monto) FROM Cuota c LEFT JOIN c.actividad act"
                        + where + " GROUP BY c.estado", Object[].class);
        params.forEach(generadas::setParameter);
        for (Object[] fila : generadas.getResultList()) {
            CuotaEstadoEnum estado = (CuotaEstadoEnum) fila[0];
            BigDecimal descuento = descuentosPorEstado.getOrDefault(estado, BigDecimal.ZERO);
            BigDecimal importe = monto(fila[2]).subtract(descuento).max(BigDecimal.ZERO);
            ReporteCuotasDTO.Estado destino = switch (estado) {
                case PAGADA -> respuesta.getPagadas();
                case PENDIENTE -> respuesta.getPendientes();
                case VENCIDA -> respuesta.getVencidas();
                case ANULADA -> respuesta.getAnuladas();
            };
            destino.setCantidad(((Number) fila[1]).longValue());
            destino.setImporte(importe);
            if (estado != CuotaEstadoEnum.ANULADA) {
                respuesta.getGeneradas().setCantidad(respuesta.getGeneradas().getCantidad() + destino.getCantidad());
                respuesta.getGeneradas().setImporte(respuesta.getGeneradas().getImporte().add(importe));
                respuesta.setDescuentosAplicados(respuesta.getDescuentosAplicados().add(descuento));
            }
        }

        // * Recaudado: lo que efectivamente entro a caja por cuotas en el periodo (fecha de cobro)
        ReporteEconomicoRequestDTO cobros = new ReporteEconomicoRequestDTO();
        cobros.setDesde(request.getDesde());
        cobros.setHasta(request.getHasta());
        cobros.setTipo(MovimientoTipoEnum.INGRESO);
        cobros.setActividadUuid(request.getActividadUuid());
        cobros.setCategorias(tipo == null ? List.of(CUOTA_SOCIAL, CUOTA_ACTIVIDAD, PASE)
                : List.of(switch (tipo) {
                    case SOCIAL -> CUOTA_SOCIAL;
                    case ACTIVIDAD -> CUOTA_ACTIVIDAD;
                    case PASE -> PASE;
                }));
        ReporteEconomicoDTO recaudacion = agrupar(new Filtro(cobros, companyId));
        respuesta.setRecaudado(recaudacion.getResumen().getTotalIngresos());
        respuesta.setCobros(recaudacion.getResumen().getCantidadIngresos());
        respuesta.setRecaudacionMensual(recaudacion.getMensual());
        respuesta.setRecaudacionPorActividad(recaudacion.getPorActividad());
        respuesta.setRecaudacionPorMedioPago(recaudacion.getPorMedioPago());
        respuesta.setRecaudacionPorCategoriaSocio(categoriasSocio(new Filtro(cobros, companyId)));
        return respuesta;
    }

    // ---------------------------------------------------------------- Consultas

    private ReporteEconomicoDTO agrupar(Filtro filtro) {
        String jpql = "SELECT m.tipo, " + CATEGORIA + ", act.uuid, act.nombre, m.medioPago, "
                + "YEAR(m.fecha), MONTH(m.fecha), SUM(m.monto), COUNT(m.id)"
                + JOINS + filtro.where
                + " GROUP BY m.tipo, " + CATEGORIA + ", act.uuid, act.nombre, m.medioPago, YEAR(m.fecha), MONTH(m.fecha)";
        TypedQuery<Object[]> query = entityManager.createQuery(jpql, Object[].class);
        filtro.params.forEach(query::setParameter);

        ReporteEconomicoDTO dto = new ReporteEconomicoDTO();
        ReporteEconomicoDTO.Resumen resumen = dto.getResumen();
        Map<String, ReporteEconomicoDTO.Grupo> porCategoria = new LinkedHashMap<>();
        Map<String, ReporteEconomicoDTO.Grupo> porActividad = new LinkedHashMap<>();
        Map<String, ReporteEconomicoDTO.Grupo> porMedio = new LinkedHashMap<>();
        Map<String, ReporteEconomicoDTO.Grupo> mensual = new TreeMap<>();

        for (Object[] fila : query.getResultList()) {
            boolean ingreso = fila[0] == MovimientoTipoEnum.INGRESO;
            String categoria = (String) fila[1];
            BigDecimal importe = monto(fila[7]);
            long cantidad = ((Number) fila[8]).longValue();

            if (ingreso) {
                resumen.setTotalIngresos(resumen.getTotalIngresos().add(importe));
                resumen.setCantidadIngresos(resumen.getCantidadIngresos() + cantidad);
                switch (categoria) {
                    case CUOTA_SOCIAL -> resumen.setCuotasSociales(resumen.getCuotasSociales().add(importe));
                    case CUOTA_ACTIVIDAD -> resumen.setCuotasActividades(resumen.getCuotasActividades().add(importe));
                    case PASE -> resumen.setPases(resumen.getPases().add(importe));
                    case AMENITY -> resumen.setAmenities(resumen.getAmenities().add(importe));
                    case EVENTO -> resumen.setEventos(resumen.getEventos().add(importe));
                    default -> resumen.setOtrosIngresos(resumen.getOtrosIngresos().add(importe));
                }
            } else {
                resumen.setTotalEgresos(resumen.getTotalEgresos().add(importe));
                resumen.setCantidadEgresos(resumen.getCantidadEgresos() + cantidad);
            }

            String claveCategoria = ingreso ? categoria : (MANUAL.equals(categoria) ? "GASTOS" : categoria);
            sumar(porCategoria, claveCategoria, textoCategoria(claveCategoria), ingreso, importe, cantidad);
            String actividadUuid = (String) fila[2];
            sumar(porActividad, actividadUuid == null ? "SIN_ACTIVIDAD" : actividadUuid,
                    actividadUuid == null ? "Sin actividad" : (String) fila[3], ingreso, importe, cantidad);
            MedioPagoEnum medio = (MedioPagoEnum) fila[4];
            sumar(porMedio, medio == null ? "SIN_MEDIO" : medio.name(), textoMedioPago(medio), ingreso, importe, cantidad);
            int anio = ((Number) fila[5]).intValue();
            int numeroMes = ((Number) fila[6]).intValue();
            String periodo = String.format(Locale.ROOT, "%04d-%02d", anio, numeroMes);
            sumar(mensual, periodo, MESES[numeroMes - 1] + " " + anio, ingreso, importe, cantidad);
        }

        resumen.setBalance(resumen.getTotalIngresos().subtract(resumen.getTotalEgresos()));
        resumen.setOperaciones(resumen.getCantidadIngresos() + resumen.getCantidadEgresos());
        resumen.setPromedioIngreso(promedio(resumen.getTotalIngresos(), resumen.getCantidadIngresos()));
        resumen.setPromedioEgreso(promedio(resumen.getTotalEgresos(), resumen.getCantidadEgresos()));

        Comparator<ReporteEconomicoDTO.Grupo> porMovimiento = Comparator
                .comparing((ReporteEconomicoDTO.Grupo g) -> g.getIngresos().add(g.getEgresos())).reversed();
        dto.setPorCategoria(ordenar(porCategoria, porMovimiento));
        dto.setPorActividad(ordenar(porActividad, porMovimiento));
        dto.setPorMedioPago(ordenar(porMedio, porMovimiento));
        dto.setMensual(new ArrayList<>(mensual.values()));
        return dto;
    }

    private List<ReporteEconomicoDTO.Grupo> categoriasSocio(Filtro filtro) {
        String jpql = "SELECT pc.uuid, pc.nombre, SUM(m.monto), COUNT(m.id)" + JOINS + filtro.where
                + " GROUP BY pc.uuid, pc.nombre";
        TypedQuery<Object[]> query = entityManager.createQuery(jpql, Object[].class);
        filtro.params.forEach(query::setParameter);
        List<ReporteEconomicoDTO.Grupo> grupos = new ArrayList<>();
        for (Object[] fila : query.getResultList()) {
            BigDecimal importe = monto(fila[2]);
            grupos.add(new ReporteEconomicoDTO.Grupo(
                    fila[0] == null ? "SIN_CATEGORIA" : (String) fila[0],
                    fila[1] == null ? "Sin categoría" : (String) fila[1],
                    importe, BigDecimal.ZERO, importe, ((Number) fila[3]).longValue()));
        }
        grupos.sort(Comparator.comparing(ReporteEconomicoDTO.Grupo::getIngresos).reversed());
        return grupos;
    }

    private long contar(Filtro filtro) {
        TypedQuery<Long> query = entityManager.createQuery("SELECT COUNT(m.id)" + JOINS + filtro.where, Long.class);
        filtro.params.forEach(query::setParameter);
        return query.getSingleResult();
    }

    private List<ReporteMovimientoRowDTO> filas(Filtro filtro, int desde, int cantidad) {
        String jpql = "SELECT m.id, m.uuid, m.fecha, m.tipo, " + CATEGORIA + ", m.concepto, act.nombre, "
                + "pu.nombre, pu.apellido, pu.dni, tu.nombre, tu.apellido, tu.dni, pc.nombre, "
                + "m.medioPago, m.monto, m.estado, rp.nombre, rp.apellido, m.observaciones, am.nombre, ev.titulo"
                + JOINS + filtro.where + " ORDER BY m.fecha DESC, m.id DESC";
        TypedQuery<Object[]> query = entityManager.createQuery(jpql, Object[].class)
                .setFirstResult(desde)
                .setMaxResults(cantidad);
        filtro.params.forEach(query::setParameter);

        List<ReporteMovimientoRowDTO> filas = new ArrayList<>();
        for (Object[] f : query.getResultList()) {
            ReporteMovimientoRowDTO fila = new ReporteMovimientoRowDTO();
            fila.setOperacion(String.format(Locale.ROOT, "MOV-%06d", (Long) f[0]));
            fila.setUuid((String) f[1]);
            fila.setFecha((LocalDate) f[2]);
            fila.setTipo(f[3] == MovimientoTipoEnum.INGRESO ? "Ingreso" : "Egreso");
            String categoria = (String) f[4];
            fila.setCategoria(categoria);
            fila.setCategoriaTexto(textoCategoria(f[3] == MovimientoTipoEnum.EGRESO && MANUAL.equals(categoria) ? "GASTOS" : categoria));
            String detalle = f[20] != null ? (String) f[20] : (String) f[21];
            fila.setConcepto(detalle != null && f[5] != null && !((String) f[5]).contains(detalle)
                    ? f[5] + " · " + detalle : (String) f[5]);
            fila.setActividad((String) f[6]);
            boolean conPersona = f[7] != null || f[8] != null;
            fila.setSocio(nombreCompleto(conPersona ? f[7] : f[10], conPersona ? f[8] : f[11]));
            fila.setDni((String) (conPersona ? f[9] : f[12]));
            fila.setCategoriaSocio((String) f[13]);
            fila.setMedioPago(textoMedioPago((MedioPagoEnum) f[14]));
            fila.setImporte(monto(f[15]));
            fila.setEstado(f[16] == MovimientoEstadoEnum.ANULADO ? "Anulado" : "Activo");
            fila.setRegistradoPor(nombreCompleto(f[17], f[18]));
            fila.setObservaciones((String) f[19]);
            filas.add(fila);
        }
        return filas;
    }

    // ---------------------------------------------------------------- Filtros

    /** WHERE y parametros compartidos por todas las consultas del reporte. */
    private static final class Filtro {
        private final StringBuilder where = new StringBuilder();
        private final Map<String, Object> params = new HashMap<>();

        private Filtro(ReporteEconomicoRequestDTO r, Long companyId) {
            if (r.getDesde() != null && r.getHasta() != null && r.getDesde().isAfter(r.getHasta())) {
                throw new CustomException(HttpStatus.BAD_REQUEST, "La fecha desde no puede ser posterior a la fecha hasta",
                        "ReporteEconomicoQueryService | rango invalido");
            }
            if (r.getImporteMinimo() != null && r.getImporteMaximo() != null
                    && r.getImporteMinimo().compareTo(r.getImporteMaximo()) > 0) {
                throw new CustomException(HttpStatus.BAD_REQUEST, "El importe mínimo no puede superar al máximo",
                        "ReporteEconomicoQueryService | importes invalidos");
            }
            // ! Aislamiento: siempre el club del header, nunca uno que venga en el pedido
            where.append(" WHERE m.company.id = :companyId AND m.status = 0");
            params.put("companyId", companyId);

            List<MovimientoEstadoEnum> estados = r.getEstados() == null || r.getEstados().isEmpty()
                    ? List.of(MovimientoEstadoEnum.ACTIVO) : r.getEstados();
            agregar("m.estado IN :estados", "estados", estados);
            if (r.getDesde() != null) agregar("m.fecha >= :desde", "desde", r.getDesde());
            if (r.getHasta() != null) agregar("m.fecha <= :hasta", "hasta", r.getHasta());
            if (r.getTipo() != null) agregar("m.tipo = :tipo", "tipo", r.getTipo());
            if (tieneTexto(r.getActividadUuid())) agregar("act.uuid = :actividadUuid", "actividadUuid", r.getActividadUuid());
            if (tieneTexto(r.getMemberUuid())) agregar("(p.uuid = :memberUuid OR t.uuid = :memberUuid)", "memberUuid", r.getMemberUuid());
            if (tieneTexto(r.getSocio())) {
                agregar("(LOWER(CONCAT(COALESCE(pu.nombre, ''), ' ', COALESCE(pu.apellido, ''))) LIKE :socio"
                        + " OR LOWER(CONCAT(COALESCE(tu.nombre, ''), ' ', COALESCE(tu.apellido, ''))) LIKE :socio"
                        + " OR pu.dni LIKE :socio OR tu.dni LIKE :socio)",
                        "socio", "%" + r.getSocio().trim().toLowerCase(Locale.ROOT) + "%");
            }
            if (tieneTexto(r.getCategoriaSocioUuid())) agregar("pc.uuid = :categoriaSocioUuid", "categoriaSocioUuid", r.getCategoriaSocioUuid());
            if (r.getMediosPago() != null && !r.getMediosPago().isEmpty()) agregar("m.medioPago IN :medios", "medios", r.getMediosPago());
            if (tieneTexto(r.getRegistradoPorUuid())) agregar("rp.uuid = :registradoPorUuid", "registradoPorUuid", r.getRegistradoPorUuid());
            if (tieneTexto(r.getConcepto())) {
                agregar("LOWER(m.concepto) LIKE :concepto", "concepto", "%" + r.getConcepto().trim().toLowerCase(Locale.ROOT) + "%");
            }
            if (r.getImporteMinimo() != null) agregar("m.monto >= :importeMinimo", "importeMinimo", r.getImporteMinimo());
            if (r.getImporteMaximo() != null) agregar("m.monto <= :importeMaximo", "importeMaximo", r.getImporteMaximo());
            if (r.getCategorias() != null && !r.getCategorias().isEmpty()) {
                List<String> condiciones = new ArrayList<>();
                for (String categoria : r.getCategorias()) {
                    condiciones.add(switch (categoria) {
                        case CUOTA_SOCIAL -> "(c.id IS NOT NULL AND c.tipo = com.blue.open.Code.Entity.Enums.CuotaTipoEnum.SOCIAL)";
                        case CUOTA_ACTIVIDAD -> "(c.id IS NOT NULL AND c.tipo = com.blue.open.Code.Entity.Enums.CuotaTipoEnum.ACTIVIDAD)";
                        case PASE -> "(c.id IS NOT NULL AND c.tipo = com.blue.open.Code.Entity.Enums.CuotaTipoEnum.PASE)";
                        case AMENITY -> "(c.id IS NULL AND r.id IS NOT NULL)";
                        case EVENTO -> "(c.id IS NULL AND r.id IS NULL AND ev.id IS NOT NULL)";
                        case MANUAL -> "(c.id IS NULL AND r.id IS NULL AND ev.id IS NULL)";
                        default -> throw new CustomException(HttpStatus.BAD_REQUEST, "Concepto inválido: " + categoria,
                                "ReporteEconomicoQueryService | categoria: " + categoria);
                    });
                }
                where.append(" AND (").append(String.join(" OR ", condiciones)).append(")");
            }
        }

        private void agregar(String condicion, String nombre, Object valor) {
            where.append(" AND ").append(condicion);
            params.put(nombre, valor);
        }
    }

    // ---------------------------------------------------------------- Utilidades

    static String textoCategoria(String categoria) {
        return switch (categoria) {
            case CUOTA_SOCIAL -> "Cuotas sociales";
            case CUOTA_ACTIVIDAD -> "Cuotas de actividades";
            case PASE -> "Pases y membresías";
            case AMENITY -> "Amenities y reservas";
            case EVENTO -> "Entradas a eventos";
            case "GASTOS" -> "Gastos y otros egresos";
            default -> "Otros ingresos";
        };
    }

    static String textoMedioPago(MedioPagoEnum medio) {
        if (medio == null) return "Sin medio de pago";
        return switch (medio) {
            case EFECTIVO -> "Efectivo";
            case TRANSFERENCIA -> "Transferencia";
            case MERCADO_PAGO -> "Mercado Pago";
            case OTRO -> "Otro";
            case DEBITO -> "Tarjeta de débito";
            case DEBITO_AUTOMATICO -> "Débito automático";
            case CREDITO -> "Tarjeta de crédito";
            case CHEQUE -> "Cheque";
            case NFC -> "Pago con celular (NFC)";
        };
    }

    static Double variacion(BigDecimal anterior, BigDecimal actual) {
        if (anterior == null || anterior.signum() == 0) {
            return null;
        }
        return actual.subtract(anterior)
                .multiply(BigDecimal.valueOf(100))
                .divide(anterior.abs(), 1, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private static void sumar(Map<String, ReporteEconomicoDTO.Grupo> mapa, String clave, String nombre,
                              boolean ingreso, BigDecimal importe, long cantidad) {
        ReporteEconomicoDTO.Grupo grupo = mapa.computeIfAbsent(clave, k -> {
            ReporteEconomicoDTO.Grupo nuevo = new ReporteEconomicoDTO.Grupo();
            nuevo.setClave(clave);
            nuevo.setNombre(nombre);
            return nuevo;
        });
        if (ingreso) {
            grupo.setIngresos(grupo.getIngresos().add(importe));
        } else {
            grupo.setEgresos(grupo.getEgresos().add(importe));
        }
        grupo.setBalance(grupo.getIngresos().subtract(grupo.getEgresos()));
        grupo.setOperaciones(grupo.getOperaciones() + cantidad);
    }

    private static List<ReporteEconomicoDTO.Grupo> ordenar(
            Map<String, ReporteEconomicoDTO.Grupo> mapa, Comparator<ReporteEconomicoDTO.Grupo> orden) {
        List<ReporteEconomicoDTO.Grupo> lista = new ArrayList<>(mapa.values());
        lista.sort(orden);
        return lista;
    }

    private static BigDecimal promedio(BigDecimal total, long cantidad) {
        return cantidad == 0 ? BigDecimal.ZERO : total.divide(BigDecimal.valueOf(cantidad), 2, RoundingMode.HALF_UP);
    }

    private static BigDecimal monto(Object valor) {
        if (valor == null) return BigDecimal.ZERO;
        if (valor instanceof BigDecimal bd) return bd.setScale(2, RoundingMode.HALF_UP);
        return new BigDecimal(valor.toString()).setScale(2, RoundingMode.HALF_UP);
    }

    private static String nombreCompleto(Object nombre, Object apellido) {
        String texto = (Objects.toString(nombre, "") + " " + Objects.toString(apellido, "")).trim();
        return texto.isEmpty() ? null : texto;
    }

    private static boolean tieneTexto(String valor) {
        return valor != null && !valor.isBlank();
    }

    private static CuotaTipoEnum parsearTipoCuota(String tipo) {
        if (!tieneTexto(tipo)) return null;
        try {
            return CuotaTipoEnum.valueOf(tipo);
        } catch (IllegalArgumentException e) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Tipo de cuota inválido",
                    "ReporteEconomicoQueryService | tipo: " + tipo);
        }
    }

    static ReporteEconomicoRequestDTO copiar(ReporteEconomicoRequestDTO o) {
        ReporteEconomicoRequestDTO c = new ReporteEconomicoRequestDTO();
        c.setDesde(o.getDesde());
        c.setHasta(o.getHasta());
        c.setTipo(o.getTipo());
        c.setCategorias(o.getCategorias());
        c.setActividadUuid(o.getActividadUuid());
        c.setMemberUuid(o.getMemberUuid());
        c.setSocio(o.getSocio());
        c.setCategoriaSocioUuid(o.getCategoriaSocioUuid());
        c.setMediosPago(o.getMediosPago());
        c.setEstados(o.getEstados());
        c.setRegistradoPorUuid(o.getRegistradoPorUuid());
        c.setConcepto(o.getConcepto());
        c.setImporteMinimo(o.getImporteMinimo());
        c.setImporteMaximo(o.getImporteMaximo());
        return c;
    }
}
