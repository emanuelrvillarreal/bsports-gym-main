package com.blue.open.Code.Service;

import com.blue.open.Core.Exceptions.CustomException;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MemberBulkImportFileParserTest {

    private MemberBulkImportFileParser parser;

    @BeforeEach
    void setUp() {
        parser = new MemberBulkImportFileParser();
    }

    @Test
    void parse_aceptaEncabezadosCaseInsensitive_yFechaDdMmYyyy() throws Exception {
        MockMultipartFile file = workbookFile("import.xlsx", new String[] {
                "NOMBRE", "Apellido", "DNI", "Email", "Contraseña", "Teléfono",
                "Fecha de nacimiento", "Número de socio", "Fecha de alta", "Parentesco", "Titular DNI"
        }, new String[] {
                "Juan", "Pérez", "30123456", "juan@test.com", "Pass1234", "111",
                "15/05/1990", "100", "01/06/2026", "", ""
        });

        var rows = parser.parse(file);

        assertEquals(1, rows.size());
        MemberBulkImportRow row = rows.get(0);
        assertEquals("Juan", row.getNombre());
        assertEquals(LocalDate.of(1990, 5, 15), row.getFechaNacimiento());
        assertEquals(LocalDate.of(2026, 6, 1), row.getFechaAlta());
        assertTrue(row.getWarnings().isEmpty());
    }

    @Test
    void parse_fechaInvalida_generaWarning_yNull() throws Exception {
        MockMultipartFile file = workbookFile("import.xlsx", headers(), new String[] {
                "Ana", "López", "", "ana@test.com", "Pass1234", "",
                "1990-05-15", "", "", "", ""
        });

        MemberBulkImportRow row = parser.parse(file).get(0);

        assertNull(row.getFechaNacimiento());
        assertTrue(row.getWarnings().stream().anyMatch(w -> w.contains("dd/MM/yyyy")));
    }

    @Test
    void parse_rechazaCsv() {
        MockMultipartFile file = new MockMultipartFile("file", "datos.csv", "text/csv", "a,b".getBytes());

        CustomException ex = assertThrows(CustomException.class, () -> parser.parse(file));

        assertTrue(ex.getUserMessage().toLowerCase().contains("excel")
                || ex.getUserMessage().toLowerCase().contains("soportado"));
    }

    @Test
    void normalizeHeader_ignoraTildes() {
        assertEquals("numero de socio", MemberBulkImportFileParser.normalizeHeader("Número de socio"));
        assertEquals("contrasena", MemberBulkImportFileParser.normalizeHeader("Contraseña"));
    }

    private static String[] headers() {
        return new String[] {
                "Nombre", "Apellido", "DNI", "Email", "Contraseña", "Teléfono",
                "Fecha de nacimiento", "Número de socio", "Fecha de alta", "Parentesco", "Titular DNI"
        };
    }

    private MockMultipartFile workbookFile(String filename, String[] header, String[] data) throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet();
            var headerRow = sheet.createRow(0);
            for (int i = 0; i < header.length; i++) {
                headerRow.createCell(i).setCellValue(header[i]);
            }
            var dataRow = sheet.createRow(1);
            for (int i = 0; i < data.length; i++) {
                dataRow.createCell(i).setCellValue(data[i]);
            }
            workbook.write(out);
            return new MockMultipartFile("file", filename,
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    out.toByteArray());
        }
    }
}
