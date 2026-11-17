package com.blue.open.Code.Service;

import com.blue.open.Code.Entity.DTO.MemberBulkImportProgressDTO;
import com.blue.open.Code.Entity.DTO.MemberBulkImportResultDTO;
import com.blue.open.Code.Entity.DTO.MemberDTO;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.Member;
import com.blue.open.Code.Entity.Db.User;
import com.blue.open.Code.Entity.Enums.ParentescoEnum;
import com.blue.open.Code.Service.support.RecordingMemberCupoService;
import com.blue.open.Code.Service.support.StubAuthService;
import com.blue.open.Core.Exceptions.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MemberBulkImportServiceTest {

    private MemberBulkImportService service;
    private StubAuthService stubAuthService;
    private Company company;
    private StubFileParser stubFileParser;
    private StubRowProcessor stubRowProcessor;
    private MemberBulkImportProgressService progressService;
    private RecordingMemberCupoService memberCupoService;

    @BeforeEach
    void setUp() {
        service = new MemberBulkImportService();
        stubAuthService = new StubAuthService();
        company = new Company();
        company.setId(10L);
        company.setUuid("club-uuid");
        stubAuthService.setCompany(company);

        stubFileParser = new StubFileParser();
        stubRowProcessor = new StubRowProcessor();
        progressService = new MemberBulkImportProgressService();
        memberCupoService = new RecordingMemberCupoService();

        ReflectionTestUtils.setField(service, "authService", stubAuthService);
        ReflectionTestUtils.setField(service, "fileParser", stubFileParser);
        ReflectionTestUtils.setField(service, "rowProcessor", stubRowProcessor);
        ReflectionTestUtils.setField(service, "validationHelper", new MemberUserValidationHelper());
        ReflectionTestUtils.setField(service, "progressService", progressService);
        ReflectionTestUtils.setField(service, "memberCupoService", memberCupoService);
        // Executor que ejecuta en el hilo actual para que el test sea determinista.
        ReflectionTestUtils.setField(service, "executor", (Executor) Runnable::run);
    }

    @Test
    void importar_parcial_unaFilaOk_yUnaConFechaInvalida() {
        MemberBulkImportRow ok = row(2, "juan@test.com");
        MemberBulkImportRow bad = row(3, "ana@test.com");
        bad.setFechaNacimiento(null);
        bad.getWarnings().add("Fecha de nacimiento: formato esperado dd/MM/yyyy");
        stubFileParser.rows = List.of(ok, bad);

        MemberDTO dto = new MemberDTO();
        dto.setMemberUuid("member-1");
        dto.setNumeroSocio("1");
        Member titularMember = new Member();
        titularMember.setUuid("member-1");
        User user = new User();
        user.setEmail("juan@test.com");
        user.setDni("30123456");
        titularMember.setUser(user);
        stubRowProcessor.titularResult = new MemberBulkImportRowProcessor.ProcessedRow(dto, titularMember);

        MemberBulkImportResultDTO result = service.procesar(service.preparar(dummyFile()));

        assertEquals(2, result.getTotalRows());
        assertEquals(1, result.getSuccessCount());
        assertEquals(1, result.getErrorCount());
        assertEquals("SUCCESS", result.getRows().get(0).getStatus());
        assertEquals("ERROR", result.getRows().get(1).getStatus());
        assertTrue(result.getRows().get(1).getErrorMessage().contains("Fecha de nacimiento"));
    }

    @Test
    void importar_familiar_resuelveTitularDesdeCache() {
        MemberBulkImportRow titularRow = row(2, "titular@test.com");
        titularRow.setDni("20111222");

        MemberBulkImportRow familiarRow = row(3, "fam@test.com");
        familiarRow.setTitularDni("20111222");
        familiarRow.setParentesco(ParentescoEnum.HIJO);

        stubFileParser.rows = List.of(titularRow, familiarRow);

        Member titularMember = new Member();
        titularMember.setId(1L);
        User titularUser = new User();
        titularUser.setDni("20111222");
        titularUser.setEmail("titular@test.com");
        titularMember.setUser(titularUser);

        MemberDTO titularDto = new MemberDTO();
        titularDto.setMemberUuid("titular-uuid");
        titularDto.setNumeroSocio("10");

        MemberDTO famDto = new MemberDTO();
        famDto.setMemberUuid("fam-uuid");

        stubRowProcessor.titularResult = new MemberBulkImportRowProcessor.ProcessedRow(titularDto, titularMember);
        stubRowProcessor.titularForFamiliar = titularMember;
        stubRowProcessor.familiarResult = new MemberBulkImportRowProcessor.ProcessedRow(famDto, null);

        MemberBulkImportResultDTO result = service.procesar(service.preparar(dummyFile()));

        assertEquals(2, result.getSuccessCount());
        assertEquals("FAMILIAR", result.getRows().get(1).getTipo());
        assertEquals("fam-uuid", result.getRows().get(1).getMemberUuid());
    }

    @Test
    void importar_publicaAvanceFilaAFilaYFinaliza() {
        stubFileParser.rows = List.of(row(2, "juan@test.com"), row(3, "ana@test.com"));

        MemberDTO dto = new MemberDTO();
        dto.setMemberUuid("member-1");
        Member titularMember = new Member();
        User user = new User();
        user.setEmail("juan@test.com");
        titularMember.setUser(user);
        stubRowProcessor.titularResult = new MemberBulkImportRowProcessor.ProcessedRow(dto, titularMember);

        MemberBulkImportContext context = service.preparar(dummyFile());
        MemberBulkImportProgressDTO inicial = progressService.consultar(context.importId());
        assertEquals(0, inicial.getFilasProcesadas());
        assertEquals(2, inicial.getFilasTotales());
        assertEquals(0, inicial.getPorcentajeCompletado());

        service.importar(context).join();

        MemberBulkImportProgressDTO progreso = progressService.consultar(context.importId());
        assertEquals(2, progreso.getFilasProcesadas());
        assertEquals(2, progreso.getFilasTotales());
        assertEquals(100, progreso.getPorcentajeCompletado());
    }

    // preparar() debe validar el cupo contra el TOTAL de filas del archivo, no fila por fila.
    @Test
    void preparar_validaCupoConTotalDeFilas() {
        stubFileParser.rows = List.of(row(2, "juan@test.com"), row(3, "ana@test.com"), row(4, "leo@test.com"));

        service.preparar(dummyFile());

        assertEquals(company, memberCupoService.getLastCompany());
        assertEquals(3, memberCupoService.getLastNuevosSocios());
    }

    // Si el cupo esta agotado, preparar() debe cortar antes de arrancar el procesamiento (no
    // debe iniciarse ningun progreso ni crearse un MemberBulkImportContext).
    @Test
    void preparar_sinCupoDisponible_lanzaYNoIniciaNada() {
        stubFileParser.rows = List.of(row(2, "juan@test.com"), row(3, "ana@test.com"));
        memberCupoService.setExceptionToThrow(new CustomException(HttpStatus.BAD_REQUEST,
                "El club alcanzó el límite de socios de su plan", "test"));

        CustomException ex = assertThrows(CustomException.class, () -> service.preparar(dummyFile()));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertTrue(memberCupoService.wasCalled());
    }

    private MemberBulkImportRow row(int rowNumber, String email) {
        MemberBulkImportRow row = new MemberBulkImportRow();
        row.setRowNumber(rowNumber);
        row.setNombre("Nombre");
        row.setApellido("Apellido");
        row.setEmail(email);
        row.setPassword("Pass1234");
        row.setFechaNacimiento(LocalDate.of(1990, 5, 15));
        return row;
    }

    private MockMultipartFile dummyFile() {
        return new MockMultipartFile("file", "import.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                new byte[] {1, 2, 3});
    }

    private static class StubFileParser extends MemberBulkImportFileParser {
        List<MemberBulkImportRow> rows = List.of();

        @Override
        public List<MemberBulkImportRow> parse(MultipartFile file) {
            return rows;
        }
    }

    private static class StubRowProcessor extends MemberBulkImportRowProcessor {
        MemberBulkImportRowProcessor.ProcessedRow titularResult;
        MemberBulkImportRowProcessor.ProcessedRow familiarResult;
        Member titularForFamiliar;

        @Override
        public MemberBulkImportRowProcessor.ProcessedRow processTitular(Company company,
                com.blue.open.Code.Entity.DTO.MemberTitularAltaCommand command) {
            return titularResult;
        }

        @Override
        public MemberBulkImportRowProcessor.ProcessedRow processFamiliar(Company company, Member titular,
                com.blue.open.Code.Entity.DTO.MemberFamiliarAltaCommand command) {
            return familiarResult;
        }

        @Override
        public Member resolveTitular(Company company, String identifier, Map<String, Member> titularCache) {
            return titularForFamiliar != null ? titularForFamiliar : titularCache.get(cacheKey(identifier));
        }
    }
}
