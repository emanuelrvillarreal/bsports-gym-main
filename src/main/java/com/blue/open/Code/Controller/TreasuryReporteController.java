package com.blue.open.Code.Controller;

import com.blue.open.Code.Auth.PermissionEnum;
import com.blue.open.Code.Entity.DTO.ReporteComparacionDTO;
import com.blue.open.Code.Entity.DTO.ReporteCuotasDTO;
import com.blue.open.Code.Entity.DTO.ReporteEconomicoDTO;
import com.blue.open.Code.Entity.DTO.ReporteEconomicoRequestDTO;
import com.blue.open.Code.Service.Tesoreria.ReporteEconomicoExcelService;
import com.blue.open.Code.Service.Tesoreria.ReporteEconomicoQueryService;
import com.blue.open.Core.Service.Auth.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Reportes y exportaciones de Tesoreria. Solo ADMIN (permiso TREASURY_REPORTS): recepcion conserva
 * sus permisos actuales y no accede automaticamente a la informacion financiera consolidada.
 * Siempre sobre el club del header X-Company-Uuid.
 */
@RestController
@RequestMapping("treasury/reportes")
public class TreasuryReporteController {

    private static final MediaType XLSX =
            MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    @Autowired
    private AuthService authService;

    @Autowired
    private ReporteEconomicoQueryService queryService;

    @Autowired
    private ReporteEconomicoExcelService excelService;

    @PostMapping("/movimientos")
    @Operation(summary = "Reporte de movimientos", description = "Movimientos económicos con filtros combinables, resumen y agrupaciones. Solo ADMIN.")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ReporteEconomicoDTO> movimientos(@Valid @RequestBody ReporteEconomicoRequestDTO request) {
        authService.valid(List.of(PermissionEnum.TREASURY_REPORTS));
        Long companyId = authService.getCurrentCompanyOrThrow().getId();
        return ResponseEntity.ok(queryService.reporte(request, companyId));
    }

    @PostMapping("/movimientos/excel")
    @Operation(summary = "Exportar movimientos a Excel", description = "XLSX con los mismos filtros del reporte: movimientos, resumen, por categoría, por actividad, por medio de pago y mensual. Solo ADMIN.")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<byte[]> excel(@Valid @RequestBody ReporteEconomicoRequestDTO request) {
        ReporteEconomicoExcelService.Archivo archivo = excelService.exportar(request);
        return ResponseEntity.ok()
                .contentType(XLSX)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(archivo.nombre(), StandardCharsets.UTF_8).build().toString())
                .header(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.CONTENT_DISPOSITION)
                .body(archivo.contenido());
    }

    @PostMapping("/comparacion")
    @Operation(summary = "Comparar períodos", description = "Ingresos, egresos y balance de dos períodos con los mismos filtros, con diferencia y variación porcentual. Solo ADMIN.")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ReporteComparacionDTO.Respuesta> comparacion(@Valid @RequestBody ReporteComparacionDTO.Request request) {
        authService.valid(List.of(PermissionEnum.TREASURY_REPORTS));
        Long companyId = authService.getCurrentCompanyOrThrow().getId();
        return ResponseEntity.ok(queryService.comparar(request, companyId));
    }

    @PostMapping("/cuotas")
    @Operation(summary = "Reporte de cuotas", description = "Cuotas generadas, pagadas, pendientes, vencidas y anuladas, separadas de la recaudación efectiva. Solo ADMIN.")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ReporteCuotasDTO.Respuesta> cuotas(@RequestBody ReporteCuotasDTO.Request request) {
        authService.valid(List.of(PermissionEnum.TREASURY_REPORTS));
        Long companyId = authService.getCurrentCompanyOrThrow().getId();
        return ResponseEntity.ok(queryService.reporteCuotas(request, companyId));
    }
}
