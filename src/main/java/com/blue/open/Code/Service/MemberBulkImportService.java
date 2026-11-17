package com.blue.open.Code.Service;

import com.blue.open.Code.Auth.PermissionEnum;
import com.blue.open.Code.Entity.DTO.MemberBulkImportResultDTO;
import com.blue.open.Code.Entity.DTO.MemberBulkImportRowResultDTO;
import com.blue.open.Code.Entity.DTO.MemberDTO;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.Member;
import com.blue.open.Code.Entity.Db.MemberBulkImportResult;
import com.blue.open.Code.Repository.MemberBulkImportResultRepository;
import com.blue.open.Core.Exceptions.CustomException;
import com.blue.open.Core.Service.Auth.AuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.regex.Pattern;

@Slf4j
@Service
public class MemberBulkImportService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final Pattern DNI_PATTERN = Pattern.compile("^\\d{7,11}$");

    @Autowired
    private AuthService authService;

    @Autowired
    private MemberBulkImportFileParser fileParser;

    @Autowired
    private MemberBulkImportRowProcessor rowProcessor;

    @Autowired
    private MemberUserValidationHelper validationHelper;

    @Autowired
    private MemberBulkImportProgressService progressService;

    @Autowired
    private MemberBulkImportResultRepository resultRepository;

    @Autowired
    @Qualifier("BulkImportTaskExecutor")
    private Executor executor;

    @Autowired
    private MemberCupoService memberCupoService;

    /**
     * Parte síncrona de la importación: valida permisos, resuelve la company y
     * parsea el Excel (todo depende del contexto de la request, que no existe en
     * el hilo asíncrono). Registra la importación y devuelve el contexto a
     * procesar.
     */
    public MemberBulkImportContext preparar(MultipartFile file) {
        authService.valid(List.of(PermissionEnum.MEMBER_CREATE, PermissionEnum.FAMILY_CREATE));
        Company company = authService.getCurrentCompanyOrThrow();

        List<MemberBulkImportRow> rows = fileParser.parse(file);
        memberCupoService.validarCupoDisponible(company, rows.size());
        String importId = progressService.iniciar(rows.size());
        return new MemberBulkImportContext(importId, company, rows);
    }

    /**
     * Procesa las filas en el pool de tareas publicando el avance por SSE después
     * de cada fila, y guarda el resultado en BD al terminar.
     */
    public CompletableFuture<Void> importar(MemberBulkImportContext context) {
        return CompletableFuture
                .supplyAsync(() -> procesar(context), executor)
                .thenAccept(result -> guardarResultado(context, result, "COMPLETED"))
                .thenRun(() -> progressService.finalizar(context.importId()))
                .exceptionally(error -> {
                    Throwable causa = error instanceof CompletionException && error.getCause() != null
                            ? error.getCause()
                            : error;
                    log.error("Error en importación masiva de socios | importId: {}", context.importId(), causa);
                    guardarResultado(context, null, "FAILED");
                    progressService.fallar(context.importId(), mensajeDeError(causa));
                    return null;
                });
    }

    MemberBulkImportResultDTO procesar(MemberBulkImportContext context) {
        Company company = context.company();
        List<MemberBulkImportRow> rows = context.rows();
        Map<String, Member> titularCache = new HashMap<>();
        Set<String> seenEmails = new HashSet<>();
        Set<String> seenDnis = new HashSet<>();

        MemberBulkImportResultDTO result = new MemberBulkImportResultDTO();
        result.setTotalRows(rows.size());

        for (MemberBulkImportRow row : rows) {
            MemberBulkImportRowResultDTO rowResult = new MemberBulkImportRowResultDTO();
            rowResult.setRowNumber(row.getRowNumber());
            rowResult.getWarnings().addAll(row.getWarnings());

            try {
                validateDuplicatesInFile(row, seenEmails, seenDnis);
                validateRowBasics(row);

                MemberBulkImportRowProcessor.RowKind kind = rowProcessor.resolveRowKind(row);
                if (kind == null) {
                    throw new CustomException(HttpStatus.BAD_REQUEST,
                            "Combinación inválida de Parentesco y Titular DNI",
                            "MemberBulkImportService | fila " + row.getRowNumber());
                }

                if (kind == MemberBulkImportRowProcessor.RowKind.TITULAR) {
                    rowResult.setTipo("TITULAR");
                    MemberBulkImportRowProcessor.ProcessedRow processed = rowProcessor.processTitular(
                            company, rowProcessor.toTitularCommand(row));
                    fillSuccess(rowResult, processed.memberDto());
                    rowProcessor.registerTitularInCache(processed.titularMember(), titularCache);
                } else {
                    rowResult.setTipo("FAMILIAR");
                    if (row.getParentesco() == null) {
                        throw new CustomException(HttpStatus.BAD_REQUEST, "Parentesco inválido",
                                "Valores permitidos: CONYUGE, HIJO, HIJA, PADRE, MADRE, OTRO");
                    }
                    Member titular = rowProcessor.resolveTitular(company, row.getTitularDni(), titularCache);
                    if (titular == null) {
                        throw new CustomException(HttpStatus.BAD_REQUEST,
                                "No se encontró un socio titular con DNI o email "
                                        + row.getTitularDni(),
                                "MemberBulkImportService | fila " + row.getRowNumber());
                    }
                    MemberBulkImportRowProcessor.ProcessedRow processed = rowProcessor.processFamiliar(
                            company, titular,
                            rowProcessor.toFamiliarCommand(row, row.getParentesco()));
                    fillSuccess(rowResult, processed.memberDto());
                }

                result.setSuccessCount(result.getSuccessCount() + 1);
                progressService.avanzar(context.importId());
                if (!rowResult.getWarnings().isEmpty()) {
                    result.setWarningCount(result.getWarningCount() + 1);
                    progressService.incrementarAdvertencias(context.importId());
                }
            } catch (CustomException ex) {
                rowResult.setStatus("ERROR");
                rowResult.setErrorMessage(ex.getUserMessage() != null ? ex.getUserMessage() : ex.getMessage());
                result.setErrorCount(result.getErrorCount() + 1);
                result.getErrorRows().add(rowResult);
                progressService.avanzarConError(context.importId());
                if (!rowResult.getWarnings().isEmpty()) {
                    result.setWarningCount(result.getWarningCount() + 1);
                    progressService.incrementarAdvertencias(context.importId());
                }
            }

            result.getRows().add(rowResult);
        }

        return result;
    }

    private String mensajeDeError(Throwable error) {
        if (error instanceof CustomException customException && customException.getUserMessage() != null) {
            return customException.getUserMessage();
        }
        return error.getMessage() != null ? error.getMessage() : "Error al procesar la importación";
    }

    private void fillSuccess(MemberBulkImportRowResultDTO rowResult, MemberDTO dto) {
        rowResult.setStatus("SUCCESS");
        rowResult.setMemberUuid(dto.getMemberUuid());
        rowResult.setNumeroSocio(dto.getNumeroSocio());
    }

    private void validateDuplicatesInFile(MemberBulkImportRow row, Set<String> seenEmails, Set<String> seenDnis) {
        if (row.getEmail() != null && !row.getEmail().isBlank()) {
            String key = row.getEmail().trim().toLowerCase(Locale.ROOT);
            if (!seenEmails.add(key)) {
                throw new CustomException(HttpStatus.BAD_REQUEST, "Email duplicado en el archivo",
                        "MemberBulkImportService | fila " + row.getRowNumber());
            }
        }
        if (row.getDni() != null && !row.getDni().isBlank()) {
            String key = row.getDni().trim();
            if (!seenDnis.add(key)) {
                throw new CustomException(HttpStatus.BAD_REQUEST, "DNI duplicado en el archivo",
                        "MemberBulkImportService | fila " + row.getRowNumber());
            }
        }
    }

    private void validateRowBasics(MemberBulkImportRow row) {
        if (row.getNombre() == null || row.getNombre().isBlank()) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Nombre requerido",
                    "MemberBulkImportService | fila " + row.getRowNumber());
        }
        if (row.getApellido() == null || row.getApellido().isBlank()) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Apellido requerido",
                    "MemberBulkImportService | fila " + row.getRowNumber());
        }
        if (row.getEmail() == null || row.getEmail().isBlank()) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Email requerido",
                    "MemberBulkImportService | fila " + row.getRowNumber());
        }
        if (!EMAIL_PATTERN.matcher(row.getEmail().trim()).matches()) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Email inválido",
                    "MemberBulkImportService | fila " + row.getRowNumber());
        }
        if (row.getDni() != null && !row.getDni().isBlank() && !DNI_PATTERN.matcher(row.getDni().trim()).matches()) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "DNI inválido",
                    "El DNI debe contener entre 7 y 11 dígitos numéricos.");
        }
        if (row.getFechaNacimiento() == null) {
            String msg = row.getWarnings().stream().anyMatch(w -> w.startsWith("Fecha de nacimiento"))
                    ? "Fecha de nacimiento inválida (formato esperado dd/MM/yyyy)"
                    : "Fecha de nacimiento requerida";
            throw new CustomException(HttpStatus.BAD_REQUEST, msg,
                    "MemberBulkImportService | fila " + row.getRowNumber());
        }
        if (!row.getFechaNacimiento().isBefore(LocalDate.now())) {
            throw new CustomException(HttpStatus.BAD_REQUEST,
                    "La fecha de nacimiento debe ser anterior a hoy",
                    "MemberBulkImportService | fila " + row.getRowNumber());
        }
        validationHelper.validateDni(row.getDni());
    }

    private void guardarResultado(MemberBulkImportContext context, MemberBulkImportResultDTO result, String status) {
        try {
            MemberBulkImportResult entity = new MemberBulkImportResult();
            entity.setImportId(context.importId());
            entity.setCompany(context.company());
            entity.setImportStatus(status);

            if (result != null) {
                entity.setTotalRows(result.getTotalRows());
                entity.setSuccessCount(result.getSuccessCount());
                entity.setErrorCount(result.getErrorCount());
                entity.setWarningCount(result.getWarningCount());
                entity.setRows(result.getRows());
            } else {
                entity.setTotalRows(context.rows().size());
                entity.setErrorMessage("Error procesando la importación");
            }

            resultRepository.save(entity);
        } catch (Exception e) {
            log.error("Error guardando resultado de importación | importId: {}", context.importId(), e);
        }
    }
}
