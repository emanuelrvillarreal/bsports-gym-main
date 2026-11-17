package com.blue.open.Code.Service;

import com.blue.open.Code.Entity.Enums.ParentescoEnum;
import com.blue.open.Code.Entity.Enums.SexoEnum;
import com.blue.open.Core.Exceptions.CustomException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Component
public class MemberBulkImportFileParser {

    private static final int MAX_DATA_ROWS = 2000;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DataFormatter DATA_FORMATTER = new DataFormatter(Locale.ROOT);

    private static final Map<String, String> HEADER_ALIASES = Map.ofEntries(
            Map.entry("nombre", "nombre"),
            Map.entry("apellido", "apellido"),
            Map.entry("dni", "dni"),
            Map.entry("email", "email"),
            Map.entry("contrasena", "password"),
            Map.entry("password", "password"),
            Map.entry("telefono", "telefono"),
            Map.entry("fecha de nacimiento", "fechaNacimiento"),
            Map.entry("sexo", "sexo"),
            Map.entry("genero", "sexo"),
            Map.entry("fecha de alta", "fechaAlta"),
            Map.entry("numero de socio", "numeroSocio"),
            Map.entry("numero socio", "numeroSocio"),
            Map.entry("nro de socio", "numeroSocio"),
            Map.entry("nro socio", "numeroSocio"),
            Map.entry("n de socio", "numeroSocio"),
            Map.entry("socio n", "numeroSocio"),
            Map.entry("parentesco", "parentesco"),
            Map.entry("titular dni", "titularDni")
    );

    private static final Set<String> REQUIRED_HEADERS = Set.of(
            "nombre", "apellido", "email", "password", "fechaNacimiento"
    );

    public List<MemberBulkImportRow> parse(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Archivo requerido",
                    "MemberBulkImportFileParser | file vacío");
        }
        String filename = file.getOriginalFilename();
        if (filename == null || (!filename.toLowerCase(Locale.ROOT).endsWith(".xlsx")
                && !filename.toLowerCase(Locale.ROOT).endsWith(".xls"))) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Formato no soportado",
                    "Solo se aceptan archivos Excel (.xls o .xlsx)");
        }

        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = workbook.getNumberOfSheets() > 0 ? workbook.getSheetAt(0) : null;
            if (sheet == null) {
                throw new CustomException(HttpStatus.BAD_REQUEST, "Archivo vacío",
                        "MemberBulkImportFileParser | sin hojas");
            }

            Row headerRow = sheet.getRow(sheet.getFirstRowNum());
            if (headerRow == null) {
                throw new CustomException(HttpStatus.BAD_REQUEST, "Encabezados requeridos",
                        "MemberBulkImportFileParser | sin fila de encabezados");
            }

            Map<String, Integer> columnIndex = resolveColumnIndex(headerRow);
            validateRequiredHeaders(columnIndex);

            List<MemberBulkImportRow> rows = new ArrayList<>();
            int firstDataRow = headerRow.getRowNum() + 1;
            int lastRow = sheet.getLastRowNum();
            for (int i = firstDataRow; i <= lastRow; i++) {
                Row row = sheet.getRow(i);
                if (row == null || isRowEmpty(row, columnIndex)) {
                    continue;
                }
                if (rows.size() >= MAX_DATA_ROWS) {
                    throw new CustomException(HttpStatus.BAD_REQUEST, "Demasiadas filas",
                            "Máximo " + MAX_DATA_ROWS + " filas de datos");
                }
                rows.add(mapRow(row, columnIndex, i + 1));
            }

            if (rows.isEmpty()) {
                throw new CustomException(HttpStatus.BAD_REQUEST, "Sin datos",
                        "MemberBulkImportFileParser | no hay filas de datos");
            }
            return rows;
        } catch (CustomException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "No se pudo leer el archivo Excel",
                    ex.getMessage());
        }
    }

    private Map<String, Integer> resolveColumnIndex(Row headerRow) {
        Map<String, Integer> columnIndex = new HashMap<>();
        for (Cell cell : headerRow) {
            String raw = cellString(cell);
            if (raw == null || raw.isBlank()) {
                continue;
            }
            String canonical = HEADER_ALIASES.get(normalizeHeader(raw));
            if (canonical != null) {
                columnIndex.putIfAbsent(canonical, cell.getColumnIndex());
            }
        }
        return columnIndex;
    }

    private void validateRequiredHeaders(Map<String, Integer> columnIndex) {
        List<String> missing = new ArrayList<>();
        for (String required : REQUIRED_HEADERS) {
            if (!columnIndex.containsKey(required)) {
                missing.add(required);
            }
        }
        if (!missing.isEmpty()) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Columnas obligatorias faltantes",
                    "Faltan columnas: " + String.join(", ", missing));
        }
    }

    private MemberBulkImportRow mapRow(Row row, Map<String, Integer> columnIndex, int rowNumber) {
        MemberBulkImportRow parsed = new MemberBulkImportRow();
        parsed.setRowNumber(rowNumber);
        parsed.setNombre(cellValue(row, columnIndex.get("nombre")));
        parsed.setApellido(cellValue(row, columnIndex.get("apellido")));
        parsed.setDni(blankToNull(cellValue(row, columnIndex.get("dni"))));
        parsed.setEmail(cellValue(row, columnIndex.get("email")));
        parsed.setPassword(cellValue(row, columnIndex.get("password")));
        parsed.setTelefono(blankToNull(cellValue(row, columnIndex.get("telefono"))));

        String fechaNacimientoRaw = columnIndex.containsKey("fechaNacimiento")
                ? cellValue(row, columnIndex.get("fechaNacimiento")) : null;
        parsed.setFechaNacimiento(parseDate(fechaNacimientoRaw, "Fecha de nacimiento", parsed.getWarnings()));

        String sexoRaw = columnIndex.containsKey("sexo") ? cellValue(row, columnIndex.get("sexo")) : null;
        parsed.setSexo(parseSexo(sexoRaw));
        if (sexoRaw != null && !sexoRaw.isBlank() && parsed.getSexo() == null) {
            parsed.getWarnings().add("Sexo inválido: " + sexoRaw.trim() + " (valores: Masculino, Femenino, Otro)");
        }

        String numeroSocioRaw = columnIndex.containsKey("numeroSocio")
                ? cellValue(row, columnIndex.get("numeroSocio")) : null;
        parsed.setNumeroSocio(parseNumeroSocio(numeroSocioRaw, parsed.getWarnings()));

        String fechaAltaRaw = columnIndex.containsKey("fechaAlta")
                ? cellValue(row, columnIndex.get("fechaAlta")) : null;
        parsed.setFechaAlta(parseDate(fechaAltaRaw, "Fecha de alta", parsed.getWarnings()));

        String parentescoRaw = columnIndex.containsKey("parentesco")
                ? cellValue(row, columnIndex.get("parentesco")) : null;
        parsed.setParentesco(parseParentesco(parentescoRaw));
        if (parentescoRaw != null && !parentescoRaw.isBlank() && parsed.getParentesco() == null) {
            parsed.getWarnings().add("Parentesco inválido: " + parentescoRaw.trim()
                    + " (valores: CONYUGE, HIJO, HIJA, PADRE, MADRE, OTRO)");
        }

        parsed.setTitularDni(blankToNull(columnIndex.containsKey("titularDni")
                ? cellValue(row, columnIndex.get("titularDni")) : null));
        return parsed;
    }

    /** Acepta Masculino/Femenino/Otro, M/F/O y también Hombre/Mujer (sin importar mayúsculas ni tildes). */
    static SexoEnum parseSexo(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return switch (normalizeHeader(raw)) {
            case "m", "masculino", "hombre", "varon" -> SexoEnum.MASCULINO;
            case "f", "femenino", "mujer" -> SexoEnum.FEMENINO;
            case "o", "otro", "x" -> SexoEnum.OTRO;
            default -> null;
        };
    }

    /** Número de socio opcional; si viene vacío o inválido se asigna el siguiente libre. */
    static Integer parseNumeroSocio(String raw, List<String> warnings) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String limpio = raw.trim().replaceAll("[.\\s]", "");
        if (limpio.endsWith(",0")) {
            limpio = limpio.substring(0, limpio.length() - 2);
        }
        try {
            int numero = Integer.parseInt(limpio);
            if (numero > 0) {
                return numero;
            }
        } catch (NumberFormatException ignored) {
            // se avisa abajo
        }
        warnings.add("Número de socio inválido: " + raw.trim() + " (se asignó el siguiente libre)");
        return null;
    }

    private ParentescoEnum parseParentesco(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return ParentescoEnum.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private LocalDate parseDate(String raw, String fieldLabel, List<String> warnings) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(raw.trim(), DATE_FORMAT);
        } catch (DateTimeParseException ex) {
            warnings.add(fieldLabel + ": formato esperado dd/MM/yyyy (valor: " + raw.trim() + ")");
            return null;
        }
    }

    private boolean isRowEmpty(Row row, Map<String, Integer> columnIndex) {
        for (Integer index : columnIndex.values()) {
            String value = cellValue(row, index);
            if (value != null && !value.isBlank()) {
                return false;
            }
        }
        return true;
    }

    private String cellValue(Row row, Integer columnIndex) {
        if (columnIndex == null) {
            return null;
        }
        Cell cell = row.getCell(columnIndex);
        return cellString(cell);
    }

    private String cellString(Cell cell) {
        if (cell == null) {
            return null;
        }
        String value = DATA_FORMATTER.formatCellValue(cell);
        return value != null ? value.trim() : null;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    static String normalizeHeader(String header) {
        String normalized = Normalizer.normalize(header.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return normalized;
    }
}
