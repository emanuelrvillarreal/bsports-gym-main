package com.blue.open.Code.Service.Membresias;

import com.blue.open.Code.Auth.PermissionEnum;
import com.blue.open.Code.Entity.DTO.MembresiaDTO;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.Cuota;
import com.blue.open.Code.Entity.Db.Membresia;
import com.blue.open.Code.Entity.Enums.CuotaEstadoEnum;
import com.blue.open.Code.Entity.Enums.MembresiaEstadoEnum;
import com.blue.open.Code.Entity.Enums.MembresiaOrigenEnum;
import com.blue.open.Code.Entity.Enums.MembresiaSituacionEnum;
import com.blue.open.Code.Entity.Enums.MovimientoEstadoEnum;
import com.blue.open.Code.Entity.Enums.MovimientoTipoEnum;
import com.blue.open.Core.Exceptions.CustomException;
import com.blue.open.Core.Service.Auth.AuthService;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Listado, indicadores y Excel de membresías (solo ADMIN, PASSES_MANAGE). Los ingresos salen de los
 * movimientos de cobro activos de las cuotas de tipo PASE: se cuentan solo cuando el pago está confirmado.
 */
@Service
public class MembresiaReporteService {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Autowired
    private AuthService authService;

    @Autowired
    private MembresiaSoporte soporte;

    @Transactional(readOnly = true)
    public Page<MembresiaDTO.Fila> paginar(MembresiaDTO.PaginateRequest request) {
        authService.valid(List.of(PermissionEnum.PASSES_MANAGE));
        Company company = authService.getCurrentCompanyOrThrow();
        List<MembresiaDTO.Fila> filas = filas(company, request);
        int size = Math.max(1, request.getSize());
        int desde = Math.min(request.getPage() * size, filas.size());
        int hasta = Math.min(desde + size, filas.size());
        return new PageImpl<>(filas.subList(desde, hasta), PageRequest.of(request.getPage(), size), filas.size());
    }

    List<MembresiaDTO.Fila> filas(Company company, MembresiaDTO.PaginateRequest r) {
        if (r.getDesde() != null && r.getHasta() != null && r.getHasta().isBefore(r.getDesde())) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "La fecha hasta no puede ser anterior a la fecha desde",
                    "MembresiaReporteService | rango invertido");
        }
        LocalDate hoy = soporte.hoy();
        List<Membresia> todas = soporte.delClub(company.getId());
        Map<Long, Cuota> cuotas = soporte.cuotas(todas);
        String busqueda = r.getSearch() == null ? "" : r.getSearch().trim().toLowerCase(Locale.ROOT);
        List<MembresiaDTO.Fila> filas = new ArrayList<>();
        for (Membresia m : todas) {
            Cuota cuota = cuotas.get(m.getId());
            MembresiaSituacionEnum situacion = MembresiaReglas.situacion(m, MembresiaSoporte.estadoCobro(cuota), hoy);
            if (r.getSituacion() != null && r.getSituacion() != situacion) continue;
            if (r.getOrigen() != null && r.getOrigen() != m.getOrigen()) continue;
            if (r.getPaseUuid() != null && !r.getPaseUuid().isBlank() && !r.getPaseUuid().equals(m.getPase().getUuid())) continue;
            if (r.getDesde() != null && m.getFechaInicio().isBefore(r.getDesde())) continue;
            if (r.getHasta() != null && m.getFechaInicio().isAfter(r.getHasta())) continue;
            String socio = MembresiaSoporte.nombre(m.getMember());
            if (!busqueda.isEmpty()) {
                String texto = (socio + " " + nulo(m.getMember().getNumeroSocio()) + " " + nulo(m.getMember().getUser().getDni())
                        + " " + m.getPase().getNombre()).toLowerCase(Locale.ROOT);
                if (!texto.contains(busqueda)) continue;
            }
            MembresiaDTO.Fila f = new MembresiaDTO.Fila();
            f.setUuid(m.getUuid());
            f.setSocio(socio);
            f.setSocioUuid(m.getMember().getUuid());
            f.setNumeroSocio(m.getMember().getNumeroSocio());
            f.setPase(m.getPase().getNombre());
            f.setPaseUuid(m.getPase().getUuid());
            f.setCategoria(m.getPase().getCategoria());
            f.setOrigen(m.getOrigen());
            f.setSituacion(situacion);
            f.setFechaInicio(m.getFechaInicio());
            f.setFechaFin(m.getFechaFin());
            f.setPrecio(m.getPrecio());
            f.setEstadoCobro(cuota == null ? null : cuota.getEstado());
            f.setCobrado(cuota != null && cuota.getEstado() == CuotaEstadoEnum.PAGADA ? cuota.montoFinal() : BigDecimal.ZERO);
            f.setIngresosPermitidos(m.getIngresosPermitidos());
            f.setIngresosUsados(m.getIngresosUsados());
            f.setIntegrantes(m.getIntegrantes().size());
            filas.add(f);
        }
        return filas;
    }

    @Transactional(readOnly = true)
    public MembresiaDTO.Dashboard dashboard() {
        authService.valid(List.of(PermissionEnum.PASSES_MANAGE));
        Company company = authService.getCurrentCompanyOrThrow();
        return dashboard(company);
    }

    MembresiaDTO.Dashboard dashboard(Company company) {
        LocalDate hoy = soporte.hoy();
        LocalDate inicioMes = hoy.withDayOfMonth(1);
        List<Membresia> todas = soporte.delClub(company.getId());
        Map<Long, Cuota> cuotas = soporte.cuotas(todas);
        Set<Long> renovadas = soporte.conSucesora(todas);
        MembresiaDTO.Dashboard d = new MembresiaDTO.Dashboard();
        Map<String, MembresiaDTO.Item> contratados = new LinkedHashMap<>();
        for (Membresia m : todas) {
            MembresiaSituacionEnum s = MembresiaReglas.situacion(m, MembresiaSoporte.estadoCobro(cuotas.get(m.getId())), hoy);
            boolean renovada = renovadas.contains(m.getId());
            switch (s) {
                case VIGENTE -> {
                    d.setActivas(d.getActivas() + 1);
                    if (!renovada && !m.getFechaFin().isAfter(hoy.plusDays(7))) d.setProximasAVencer(d.getProximasAVencer() + 1);
                }
                case PENDIENTE_PAGO -> d.setPendientesDePago(d.getPendientesDePago() + 1);
                case CONGELADA -> d.setCongeladas(d.getCongeladas() + 1);
                case VENCIDA -> {
                    if (!renovada && !m.getFechaFin().isBefore(hoy.minusDays(30))) d.setVencidasUltimos30Dias(d.getVencidasUltimos30Dias() + 1);
                }
                default -> { }
            }
            LocalDate alta = m.getDateCreated() == null ? m.getFechaInicio() : m.getDateCreated().toLocalDateTime().toLocalDate();
            if (!alta.isBefore(inicioMes)) {
                if (m.getOrigen() == MembresiaOrigenEnum.RENOVACION) d.setRenovacionesMes(d.getRenovacionesMes() + 1);
                else d.setNuevasMes(d.getNuevasMes() + 1);
            }
            if (m.getEstado() == MembresiaEstadoEnum.BAJA && m.getFechaBaja() != null && !m.getFechaBaja().isBefore(inicioMes)) {
                d.setBajasMes(d.getBajasMes() + 1);
            }
            if (!m.getFechaInicio().isBefore(hoy.minusMonths(12))) {
                MembresiaDTO.Item item = contratados.computeIfAbsent(m.getPase().getUuid(), k -> {
                    MembresiaDTO.Item nuevo = new MembresiaDTO.Item();
                    nuevo.setClave(k);
                    nuevo.setNombre(m.getPase().getNombre());
                    return nuevo;
                });
                item.setCantidad(item.getCantidad() + 1);
            }
        }
        d.setMasContratados(contratados.values().stream()
                .sorted(Comparator.comparingLong(MembresiaDTO.Item::getCantidad).reversed()).limit(8).toList());

        List<Object[]> ingresos = soporte.em().createQuery("""
                        SELECT p.uuid, p.nombre, SUM(mv.monto), COUNT(mv),
                               SUM(CASE WHEN mv.fecha >= :inicioMes THEN mv.monto ELSE 0 END)
                        FROM Movimiento mv JOIN mv.cuota c JOIN c.membresia mb JOIN mb.pase p
                        WHERE mv.company.id = :companyId AND mv.status = 0 AND mv.estado = :activo AND mv.tipo = :ingreso
                          AND mv.fecha BETWEEN :inicioAnio AND :hoy
                        GROUP BY p.uuid, p.nombre ORDER BY SUM(mv.monto) DESC""", Object[].class)
                .setParameter("companyId", company.getId()).setParameter("activo", MovimientoEstadoEnum.ACTIVO)
                .setParameter("ingreso", MovimientoTipoEnum.INGRESO).setParameter("inicioAnio", hoy.withDayOfYear(1))
                .setParameter("inicioMes", inicioMes).setParameter("hoy", hoy).getResultList();
        for (Object[] fila : ingresos) {
            MembresiaDTO.Item item = new MembresiaDTO.Item();
            item.setClave((String) fila[0]);
            item.setNombre((String) fila[1]);
            item.setImporte(monto(fila[2]));
            item.setCantidad(((Number) fila[3]).longValue());
            d.getIngresosPorPase().add(item);
            d.setIngresosAnio(d.getIngresosAnio().add(monto(fila[2])));
            d.setIngresosMes(d.getIngresosMes().add(monto(fila[4])));
        }
        return d;
    }

    // ---------------------------------------------------------------- Excel

    public record Archivo(String nombre, byte[] contenido) {
    }

    @Transactional(readOnly = true)
    public Archivo excel(MembresiaDTO.PaginateRequest request) {
        authService.valid(List.of(PermissionEnum.PASSES_MANAGE));
        Company company = authService.getCurrentCompanyOrThrow();
        List<MembresiaDTO.Fila> filas = filas(company, request);
        try (XSSFWorkbook libro = new XSSFWorkbook(); ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            Estilos e = new Estilos(libro);
            String periodo = (request.getDesde() == null ? "inicio" : FECHA.format(request.getDesde())) + " a "
                    + (request.getHasta() == null ? "hoy" : FECHA.format(request.getHasta()));

            Sheet hoja = libro.createSheet("Membresías");
            String[] columnas = {"Socio", "N.º socio", "Pase", "Categoría", "Origen", "Inicio", "Vencimiento", "Situación",
                    "Precio", "Cobrado", "Estado del cobro", "Ingresos usados", "Ingresos permitidos", "Integrantes"};
            int[] anchos = {28, 10, 26, 14, 14, 12, 12, 16, 14, 14, 16, 10, 10, 10};
            int filaActual = encabezado(hoja, e, company.getNombreComercial(), "Membresías · período por fecha de inicio: " + periodo, columnas, anchos);
            int primera = filaActual;
            for (MembresiaDTO.Fila f : filas) {
                Row row = hoja.createRow(filaActual++);
                texto(row, 0, f.getSocio());
                texto(row, 1, f.getNumeroSocio());
                texto(row, 2, f.getPase());
                texto(row, 3, f.getCategoria() == null ? null : capital(f.getCategoria().name()));
                texto(row, 4, capital(f.getOrigen().name()));
                fecha(row, 5, f.getFechaInicio(), e);
                fecha(row, 6, f.getFechaFin(), e);
                texto(row, 7, capital(f.getSituacion().name()));
                moneda(row, 8, f.getPrecio(), e);
                moneda(row, 9, f.getCobrado(), e);
                texto(row, 10, f.getEstadoCobro() == null ? null : capital(f.getEstadoCobro().name()));
                numero(row, 11, f.getIngresosUsados());
                if (f.getIngresosPermitidos() == null) texto(row, 12, "Ilimitado"); else numero(row, 12, f.getIngresosPermitidos());
                numero(row, 13, f.getIntegrantes() + 1);
            }
            if (filas.isEmpty()) {
                hoja.createRow(filaActual++).createCell(0).setCellValue("No hay membresías con los filtros aplicados.");
            } else {
                Row total = hoja.createRow(filaActual);
                Cell etiqueta = total.createCell(0);
                etiqueta.setCellValue("Total");
                etiqueta.setCellStyle(e.negrita);
                for (int col : new int[]{8, 9}) {
                    Cell c = total.createCell(col);
                    String letra = String.valueOf((char) ('A' + col));
                    c.setCellFormula("SUM(" + letra + (primera + 1) + ":" + letra + filaActual + ")");
                    c.setCellStyle(e.monedaNegrita);
                }
                hoja.setAutoFilter(new CellRangeAddress(primera - 1, filaActual - 1, 0, columnas.length - 1));
            }

            Sheet resumen = libro.createSheet("Por pase");
            String[] colsResumen = {"Pase", "Membresías", "Vigentes", "Renovaciones", "Bajas", "Vendido (precio)", "Cobrado"};
            int fr = encabezado(resumen, e, company.getNombreComercial(), "Resumen por pase · " + periodo, colsResumen,
                    new int[]{28, 12, 12, 13, 10, 16, 16});
            Map<String, Object[]> porPase = new LinkedHashMap<>();
            for (MembresiaDTO.Fila f : filas) {
                Object[] acc = porPase.computeIfAbsent(f.getPase(), k -> new Object[]{0L, 0L, 0L, 0L, BigDecimal.ZERO, BigDecimal.ZERO});
                acc[0] = (Long) acc[0] + 1;
                if (f.getSituacion() == MembresiaSituacionEnum.VIGENTE) acc[1] = (Long) acc[1] + 1;
                if (f.getOrigen() == MembresiaOrigenEnum.RENOVACION) acc[2] = (Long) acc[2] + 1;
                if (f.getSituacion() == MembresiaSituacionEnum.BAJA) acc[3] = (Long) acc[3] + 1;
                acc[4] = ((BigDecimal) acc[4]).add(f.getPrecio());
                acc[5] = ((BigDecimal) acc[5]).add(f.getCobrado());
            }
            for (Map.Entry<String, Object[]> en : porPase.entrySet()) {
                Row row = resumen.createRow(fr++);
                texto(row, 0, en.getKey());
                for (int i = 0; i < 4; i++) numero(row, i + 1, ((Long) en.getValue()[i]).intValue());
                moneda(row, 5, (BigDecimal) en.getValue()[4], e);
                moneda(row, 6, (BigDecimal) en.getValue()[5], e);
            }
            libro.write(salida);
            String nombre = "membresias-" + soporte.hoy() + ".xlsx";
            return new Archivo(nombre, salida.toByteArray());
        } catch (IOException ex) {
            throw new CustomException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo generar el Excel",
                    "MembresiaReporteService | excel | " + ex.getMessage());
        }
    }

    private int encabezado(Sheet hoja, Estilos e, String club, String titulo, String[] columnas, int[] anchos) {
        Row r0 = hoja.createRow(0);
        Cell c0 = r0.createCell(0);
        c0.setCellValue(club == null ? "Club" : club);
        c0.setCellStyle(e.titulo);
        hoja.createRow(1).createCell(0).setCellValue(titulo);
        hoja.createRow(2).createCell(0).setCellValue("Generado el " + FECHA.format(soporte.hoy()));
        Row cab = hoja.createRow(4);
        for (int i = 0; i < columnas.length; i++) {
            Cell c = cab.createCell(i);
            c.setCellValue(columnas[i]);
            c.setCellStyle(e.cabecera);
            hoja.setColumnWidth(i, anchos[i] * 256);
        }
        hoja.createFreezePane(0, 5);
        return 5;
    }

    private static void texto(Row row, int col, String valor) {
        if (valor != null) row.createCell(col).setCellValue(valor);
    }

    private static void numero(Row row, int col, int valor) {
        row.createCell(col).setCellValue(valor);
    }

    private static void moneda(Row row, int col, BigDecimal valor, Estilos e) {
        Cell c = row.createCell(col);
        c.setCellValue(valor == null ? 0 : valor.doubleValue());
        c.setCellStyle(e.moneda);
    }

    private static void fecha(Row row, int col, LocalDate valor, Estilos e) {
        if (valor == null) return;
        Cell c = row.createCell(col);
        c.setCellValue(valor);
        c.setCellStyle(e.fecha);
    }

    private static String capital(String enumName) {
        String t = enumName.replace('_', ' ').toLowerCase(Locale.ROOT);
        return Character.toUpperCase(t.charAt(0)) + t.substring(1);
    }

    private static String nulo(String s) {
        return s == null ? "" : s;
    }

    private static BigDecimal monto(Object valor) {
        if (valor == null) return BigDecimal.ZERO;
        return valor instanceof BigDecimal bd ? bd : new BigDecimal(valor.toString());
    }

    private static final class Estilos {
        final CellStyle titulo;
        final CellStyle cabecera;
        final CellStyle moneda;
        final CellStyle monedaNegrita;
        final CellStyle fecha;
        final CellStyle negrita;

        Estilos(XSSFWorkbook libro) {
            Font grande = libro.createFont();
            grande.setBold(true);
            grande.setFontHeightInPoints((short) 14);
            titulo = libro.createCellStyle();
            titulo.setFont(grande);
            Font bold = libro.createFont();
            bold.setBold(true);
            cabecera = libro.createCellStyle();
            cabecera.setFont(bold);
            cabecera.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            cabecera.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            cabecera.setBorderBottom(BorderStyle.THIN);
            short formatoMoneda = libro.createDataFormat().getFormat("\"$\" #,##0.00");
            moneda = libro.createCellStyle();
            moneda.setDataFormat(formatoMoneda);
            monedaNegrita = libro.createCellStyle();
            monedaNegrita.setDataFormat(formatoMoneda);
            monedaNegrita.setFont(bold);
            fecha = libro.createCellStyle();
            fecha.setDataFormat(libro.createDataFormat().getFormat("dd/mm/yyyy"));
            negrita = libro.createCellStyle();
            negrita.setFont(bold);
        }
    }
}
