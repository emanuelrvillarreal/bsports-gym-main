package com.blue.open.Code.Service.Tesoreria;

import com.blue.open.Code.Auth.PermissionEnum;
import com.blue.open.Code.Entity.DTO.ReporteEconomicoDTO;
import com.blue.open.Code.Entity.DTO.ReporteEconomicoRequestDTO;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.Member;
import com.blue.open.Code.Entity.Db.User;
import com.blue.open.Code.Entity.Enums.MovimientoEstadoEnum;
import com.blue.open.Code.Entity.Enums.MovimientoTipoEnum;
import com.blue.open.Code.Repository.ActividadRepository;
import com.blue.open.Code.Service.MemberAuthService;
import com.blue.open.Core.Exceptions.CustomException;
import com.blue.open.Core.Service.Auth.AuthService;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Exportacion del reporte economico a XLSX (Apache POI, que ya usaba el backend para importar socios).
 *
 * Usa exactamente los mismos filtros que la pantalla. Escribe con SXSSF (streaming) y recorre los
 * movimientos por lotes, asi un historial grande no queda entero en memoria.
 *
 * Hojas: Movimientos, Resumen economico, Por categoria, Por actividad, Por medio de pago y Mensual.
 * Cada hoja lleva nombre del club, periodo, filtros y fecha de generacion; encabezados congelados,
 * filtros automaticos, fechas dd/mm/aaaa, importes con formato moneda y totales por formula.
 */
@Service
public class ReporteEconomicoExcelService {

    private static final ZoneId ZONA_CLUB = ZoneId.of("America/Argentina/Buenos_Aires");
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final String SIN_REGISTROS = "No hay registros para esta agrupación con los filtros aplicados.";

    private static final String[] COLUMNAS_MOVIMIENTOS = {
            "Fecha", "Operación", "Tipo", "Categoría", "Concepto", "Actividad", "Socio", "DNI",
            "Categoría del socio", "Medio de pago", "Importe", "Estado", "Registrado por", "Observaciones"};
    private static final int[] ANCHOS_MOVIMIENTOS = {12, 13, 10, 22, 40, 18, 26, 12, 18, 16, 15, 10, 22, 40};
    private static final int COLUMNA_TIPO = 2;
    private static final int COLUMNA_IMPORTE = 10;

    @Autowired
    private AuthService authService;

    @Autowired
    private ReporteEconomicoQueryService queryService;

    @Autowired
    private ActividadRepository actividadRepository;

    @Autowired
    private MemberAuthService memberAuthService;

    /** Archivo generado: contenido y nombre sugerido para la descarga. */
    public record Archivo(byte[] contenido, String nombre) { }

    @Transactional(readOnly = true)
    public Archivo exportar(ReporteEconomicoRequestDTO request) {
        authService.valid(List.of(PermissionEnum.TREASURY_REPORTS));
        Company company = authService.getCurrentCompanyOrThrow();

        long total = queryService.contar(request, company.getId());
        if (total > ReporteEconomicoQueryService.MAX_FILAS_EXPORTACION) {
            throw new CustomException(HttpStatus.BAD_REQUEST,
                    "El reporte tiene " + total + " movimientos y el máximo por archivo es "
                            + ReporteEconomicoQueryService.MAX_FILAS_EXPORTACION + ". Acotá el período o los filtros.",
                    "ReporteEconomicoExcelService | demasiadas filas: " + total);
        }

        ReporteEconomicoDTO resumen = queryService.resumen(request, company.getId());
        Encabezado encabezado = new Encabezado(
                nombreClub(company),
                textoPeriodo(request),
                ZonedDateTime.now(ZONA_CLUB).format(FECHA_HORA),
                textoFiltros(request, company.getId()));

        SXSSFWorkbook libro = new SXSSFWorkbook(200);
        try {
            Estilos estilos = new Estilos(libro);
            hojaMovimientos(libro, estilos, encabezado, request, company.getId(), total);
            hojaResumen(libro, estilos, encabezado, resumen.getResumen());
            hojaGrupos(libro, estilos, encabezado, "Por categoría", "Categoría", resumen.getPorCategoria());
            hojaGrupos(libro, estilos, encabezado, "Por actividad", "Actividad", resumen.getPorActividad());
            hojaGrupos(libro, estilos, encabezado, "Por medio de pago", "Medio de pago", resumen.getPorMedioPago());
            hojaGrupos(libro, estilos, encabezado, "Mensual", "Mes", resumen.getMensual());

            ByteArrayOutputStream salida = new ByteArrayOutputStream();
            libro.write(salida);
            return new Archivo(salida.toByteArray(), nombreArchivo(encabezado.club(), request));
        } catch (IOException e) {
            throw new CustomException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo generar el Excel",
                    "ReporteEconomicoExcelService | " + e.getMessage());
        } finally {
            libro.dispose();
            try {
                libro.close();
            } catch (IOException ignored) {
                // * Nada que liberar: los temporales ya se borraron con dispose()
            }
        }
    }

    // ---------------------------------------------------------------- Hojas

    private void hojaMovimientos(SXSSFWorkbook libro, Estilos estilos, Encabezado encabezado,
                                 ReporteEconomicoRequestDTO request, Long companyId, long total) {
        SXSSFSheet hoja = libro.createSheet("Movimientos");
        int filaTitulos = escribirEncabezado(hoja, estilos, encabezado, "Movimientos económicos");
        titulos(hoja, estilos, filaTitulos, COLUMNAS_MOVIMIENTOS, ANCHOS_MOVIMIENTOS);
        hoja.createFreezePane(0, filaTitulos + 1);

        int[] fila = {filaTitulos + 1};
        queryService.recorrer(request, companyId, movimiento -> {
            Row row = hoja.createRow(fila[0]++);
            fecha(row, 0, movimiento.getFecha(), estilos);
            texto(row, 1, movimiento.getOperacion());
            texto(row, 2, movimiento.getTipo());
            texto(row, 3, movimiento.getCategoriaTexto());
            texto(row, 4, movimiento.getConcepto());
            texto(row, 5, movimiento.getActividad());
            texto(row, 6, movimiento.getSocio());
            texto(row, 7, movimiento.getDni());
            texto(row, 8, movimiento.getCategoriaSocio());
            texto(row, 9, movimiento.getMedioPago());
            moneda(row, 10, movimiento.getImporte(), estilos.moneda);
            texto(row, 11, movimiento.getEstado());
            texto(row, 12, movimiento.getRegistradoPor());
            texto(row, 13, movimiento.getObservaciones());
        });

        int ultimaDato = fila[0] - 1;
        if (total == 0) {
            Row vacio = hoja.createRow(fila[0]++);
            texto(vacio, 0, SIN_REGISTROS);
            return;
        }
        hoja.setAutoFilter(new CellRangeAddress(filaTitulos, ultimaDato, 0, COLUMNAS_MOVIMIENTOS.length - 1));

        // * Totales con formula: si alguien filtra o corrige el Excel, siguen siendo coherentes
        String tipos = rango(COLUMNA_TIPO, filaTitulos + 1, ultimaDato);
        String importes = rango(COLUMNA_IMPORTE, filaTitulos + 1, ultimaDato);
        int filaTotal = fila[0] + 1;
        String celdaIngresos = totalFormula(hoja, estilos, filaTotal, "Total ingresos", "SUMIF(" + tipos + ",\"Ingreso\"," + importes + ")");
        String celdaEgresos = totalFormula(hoja, estilos, filaTotal + 1, "Total egresos", "SUMIF(" + tipos + ",\"Egreso\"," + importes + ")");
        totalFormula(hoja, estilos, filaTotal + 2, "Balance", celdaIngresos + "-" + celdaEgresos);
    }

    private void hojaResumen(SXSSFWorkbook libro, Estilos estilos, Encabezado encabezado, ReporteEconomicoDTO.Resumen r) {
        SXSSFSheet hoja = libro.createSheet("Resumen económico");
        int fila = escribirEncabezado(hoja, estilos, encabezado, "Resumen económico");
        hoja.setColumnWidth(0, 32 * 256);
        hoja.setColumnWidth(1, 18 * 256);
        titulos(hoja, estilos, fila, new String[]{"Indicador", "Valor"}, null);
        if (r.getOperaciones() == 0) {
            texto(hoja.createRow(fila + 1), 0, SIN_REGISTROS);
            return;
        }
        Object[][] datos = {
                {"Total de ingresos", r.getTotalIngresos()},
                {"Total de egresos", r.getTotalEgresos()},
                {"Balance (ingresos - egresos)", r.getBalance()},
                {"Cantidad de operaciones", r.getOperaciones()},
                {"Cantidad de ingresos", r.getCantidadIngresos()},
                {"Cantidad de egresos", r.getCantidadEgresos()},
                {"Promedio por ingreso", r.getPromedioIngreso()},
                {"Promedio por egreso", r.getPromedioEgreso()},
                {"Cuotas sociales cobradas", r.getCuotasSociales()},
                {"Cuotas de actividades cobradas", r.getCuotasActividades()},
                {"Pases y membresías cobrados", r.getPases()},
                {"Amenities y reservas", r.getAmenities()},
                {"Entradas a eventos", r.getEventos()},
                {"Otros ingresos", r.getOtrosIngresos()},
        };
        for (Object[] dato : datos) {
            Row row = hoja.createRow(++fila);
            texto(row, 0, (String) dato[0]);
            if (dato[1] instanceof BigDecimal importe) {
                moneda(row, 1, importe, "Balance (ingresos - egresos)".equals(dato[0]) ? estilos.monedaTotal : estilos.moneda);
            } else {
                row.createCell(1).setCellValue(((Number) dato[1]).doubleValue());
            }
        }
        Row nota = hoja.createRow(fila + 2);
        texto(nota, 0, "Solo incluye dinero efectivamente cobrado o pagado. Las cuotas pendientes no suman como ingreso.");
    }

    private void hojaGrupos(SXSSFWorkbook libro, Estilos estilos, Encabezado encabezado,
                            String nombreHoja, String columna, List<ReporteEconomicoDTO.Grupo> grupos) {
        SXSSFSheet hoja = libro.createSheet(nombreHoja);
        int filaTitulos = escribirEncabezado(hoja, estilos, encabezado, nombreHoja);
        titulos(hoja, estilos, filaTitulos, new String[]{columna, "Ingresos", "Egresos", "Balance", "Operaciones"},
                new int[]{30, 16, 16, 16, 13});
        hoja.createFreezePane(0, filaTitulos + 1);
        if (grupos.isEmpty()) {
            texto(hoja.createRow(filaTitulos + 1), 0, SIN_REGISTROS);
            return;
        }
        int fila = filaTitulos;
        for (ReporteEconomicoDTO.Grupo grupo : grupos) {
            Row row = hoja.createRow(++fila);
            texto(row, 0, grupo.getNombre());
            moneda(row, 1, grupo.getIngresos(), estilos.moneda);
            moneda(row, 2, grupo.getEgresos(), estilos.moneda);
            moneda(row, 3, grupo.getBalance(), estilos.moneda);
            row.createCell(4).setCellValue(grupo.getOperaciones());
        }
        hoja.setAutoFilter(new CellRangeAddress(filaTitulos, fila, 0, 4));
        Row total = hoja.createRow(fila + 1);
        Cell etiqueta = total.createCell(0);
        etiqueta.setCellValue("Total");
        etiqueta.setCellStyle(estilos.etiquetaTotal);
        for (int columnaTotal = 1; columnaTotal <= 4; columnaTotal++) {
            Cell celda = total.createCell(columnaTotal);
            celda.setCellFormula("SUM(" + rango(columnaTotal, filaTitulos + 1, fila) + ")");
            celda.setCellStyle(columnaTotal == 4 ? estilos.etiquetaTotal : estilos.monedaTotal);
        }
    }

    // ---------------------------------------------------------------- Bloques comunes

    private int escribirEncabezado(Sheet hoja, Estilos estilos, Encabezado e, String titulo) {
        Row r0 = hoja.createRow(0);
        Cell c0 = r0.createCell(0);
        c0.setCellValue(e.club() + " · " + titulo);
        c0.setCellStyle(estilos.titulo);
        texto(hoja.createRow(1), 0, "Período: " + e.periodo());
        texto(hoja.createRow(2), 0, "Filtros: " + e.filtros());
        texto(hoja.createRow(3), 0, "Generado el " + e.generado());
        return 5;
    }

    private void titulos(SXSSFSheet hoja, Estilos estilos, int fila, String[] titulos, int[] anchos) {
        Row row = hoja.createRow(fila);
        for (int i = 0; i < titulos.length; i++) {
            Cell celda = row.createCell(i);
            celda.setCellValue(titulos[i]);
            celda.setCellStyle(estilos.cabecera);
            if (anchos != null) {
                hoja.setColumnWidth(i, anchos[i] * 256);
            }
        }
    }

    private String totalFormula(Sheet hoja, Estilos estilos, int fila, String etiqueta, String formula) {
        Row row = hoja.createRow(fila);
        Cell texto = row.createCell(COLUMNA_IMPORTE - 1);
        texto.setCellValue(etiqueta);
        texto.setCellStyle(estilos.etiquetaTotal);
        Cell valor = row.createCell(COLUMNA_IMPORTE);
        valor.setCellFormula(formula);
        valor.setCellStyle(estilos.monedaTotal);
        return new CellReference(fila, COLUMNA_IMPORTE).formatAsString();
    }

    private static String rango(int columna, int desde, int hasta) {
        String letra = CellReference.convertNumToColString(columna);
        return letra + (desde + 1) + ":" + letra + (hasta + 1);
    }

    private static void texto(Row row, int columna, String valor) {
        if (valor != null) {
            row.createCell(columna).setCellValue(valor);
        }
    }

    private static void moneda(Row row, int columna, BigDecimal valor, CellStyle estilo) {
        Cell celda = row.createCell(columna);
        celda.setCellValue(valor == null ? 0d : valor.doubleValue());
        celda.setCellStyle(estilo);
    }

    private static void fecha(Row row, int columna, LocalDate valor, Estilos estilos) {
        if (valor != null) {
            Cell celda = row.createCell(columna);
            celda.setCellValue(valor);
            celda.setCellStyle(estilos.fecha);
        }
    }

    // ---------------------------------------------------------------- Textos

    private static String nombreClub(Company company) {
        if (company.getNombreComercial() != null && !company.getNombreComercial().isBlank()) {
            return company.getNombreComercial();
        }
        return company.getRazonSocial() != null ? company.getRazonSocial() : "Club";
    }

    static String textoPeriodo(ReporteEconomicoRequestDTO r) {
        if (r.getDesde() == null && r.getHasta() == null) return "Todo el historial";
        if (r.getDesde() == null) return "Hasta el " + r.getHasta().format(FECHA);
        if (r.getHasta() == null) return "Desde el " + r.getDesde().format(FECHA);
        return r.getDesde().format(FECHA) + " al " + r.getHasta().format(FECHA);
    }

    private String textoFiltros(ReporteEconomicoRequestDTO r, Long companyId) {
        List<String> partes = new ArrayList<>();
        if (r.getTipo() != null) partes.add(r.getTipo() == MovimientoTipoEnum.INGRESO ? "Solo ingresos" : "Solo egresos");
        if (r.getCategorias() != null && !r.getCategorias().isEmpty()) {
            partes.add("Concepto: " + r.getCategorias().stream()
                    .map(c -> MovimientoTipoEnum.EGRESO == r.getTipo() && ReporteEconomicoQueryService.MANUAL.equals(c)
                            ? ReporteEconomicoQueryService.textoCategoria("GASTOS")
                            : ReporteEconomicoQueryService.textoCategoria(c))
                    .collect(Collectors.joining(", ")));
        }
        if (r.getActividadUuid() != null && !r.getActividadUuid().isBlank()) {
            actividadRepository.findByUuidAndCompany_Id(r.getActividadUuid(), companyId)
                    .ifPresent(a -> partes.add("Actividad: " + a.getNombre()));
        }
        if (r.getMemberUuid() != null && !r.getMemberUuid().isBlank()) {
            Member member = memberAuthService.resolveMemberByUuid(r.getMemberUuid(), companyId);
            User user = member.getUser();
            partes.add("Socio: " + (user != null ? (user.getNombre() + " " + user.getApellido()).trim() : member.getUuid()));
        }
        if (r.getSocio() != null && !r.getSocio().isBlank()) partes.add("Socio: \"" + r.getSocio().trim() + "\"");
        if (r.getMediosPago() != null && !r.getMediosPago().isEmpty()) {
            partes.add("Medio de pago: " + r.getMediosPago().stream()
                    .map(ReporteEconomicoQueryService::textoMedioPago).collect(Collectors.joining(", ")));
        }
        if (r.getEstados() != null && r.getEstados().contains(MovimientoEstadoEnum.ANULADO)) {
            partes.add(r.getEstados().size() == 1 ? "Solo anulados" : "Incluye anulados");
        }
        if (r.getConcepto() != null && !r.getConcepto().isBlank()) partes.add("Texto: \"" + r.getConcepto().trim() + "\"");
        if (r.getImporteMinimo() != null) partes.add("Desde $ " + r.getImporteMinimo().toPlainString());
        if (r.getImporteMaximo() != null) partes.add("Hasta $ " + r.getImporteMaximo().toPlainString());
        if (r.getCategoriaSocioUuid() != null && !r.getCategoriaSocioUuid().isBlank()) partes.add("Con categoría de socio");
        return partes.isEmpty() ? "Sin filtros (movimientos activos)" : String.join(" · ", partes);
    }

    private static String nombreArchivo(String club, ReporteEconomicoRequestDTO r) {
        String base = club.replaceAll("[^\\p{L}\\p{N}]+", "-").replaceAll("(^-|-$)", "").toLowerCase();
        String periodo = r.getDesde() == null && r.getHasta() == null ? "historial"
                : (r.getDesde() != null ? r.getDesde().toString() : "inicio") + "_" + (r.getHasta() != null ? r.getHasta().toString() : "hoy");
        return "reporte-economico-" + (base.isEmpty() ? "club" : base) + "-" + periodo + ".xlsx";
    }

    private record Encabezado(String club, String periodo, String generado, String filtros) { }

    private static final class Estilos {
        private final CellStyle titulo;
        private final CellStyle cabecera;
        private final CellStyle moneda;
        private final CellStyle monedaTotal;
        private final CellStyle etiquetaTotal;
        private final CellStyle fecha;

        private Estilos(SXSSFWorkbook libro) {
            DataFormat formato = libro.createDataFormat();
            short formatoMoneda = formato.getFormat("\"$\" #,##0.00;[Red]-\"$\" #,##0.00");

            Font negrita = libro.createFont();
            negrita.setBold(true);
            Font grande = libro.createFont();
            grande.setBold(true);
            grande.setFontHeightInPoints((short) 14);
            Font blanca = libro.createFont();
            blanca.setBold(true);
            blanca.setColor(IndexedColors.WHITE.getIndex());

            titulo = libro.createCellStyle();
            titulo.setFont(grande);

            cabecera = libro.createCellStyle();
            cabecera.setFont(blanca);
            cabecera.setFillForegroundColor(IndexedColors.DARK_TEAL.getIndex());
            cabecera.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            cabecera.setBorderBottom(BorderStyle.THIN);

            moneda = libro.createCellStyle();
            moneda.setDataFormat(formatoMoneda);

            monedaTotal = libro.createCellStyle();
            monedaTotal.setDataFormat(formatoMoneda);
            monedaTotal.setFont(negrita);
            monedaTotal.setBorderTop(BorderStyle.THIN);

            etiquetaTotal = libro.createCellStyle();
            etiquetaTotal.setFont(negrita);
            etiquetaTotal.setBorderTop(BorderStyle.THIN);

            fecha = libro.createCellStyle();
            fecha.setDataFormat(formato.getFormat("dd/mm/yyyy"));
        }
    }
}
