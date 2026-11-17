package com.blue.open.Code.Controller;

import com.blue.open.Code.Entity.DTO.MembresiaDTO;
import com.blue.open.Code.Service.Membresias.MembresiaReporteService;
import com.blue.open.Code.Service.Membresias.MembresiaService;
import com.blue.open.Code.Service.Membresias.PaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Pases y membresías. Configurar pases, indicadores y reportes: ADMIN (PASSES_MANAGE). Vender, renovar,
 * congelar y dar de baja desde el legajo: ADMIN y recepción (MEMBERSHIPS_MANAGE).
 */
@RestController
@RequestMapping("membresias")
@SecurityRequirement(name = "bearerAuth")
public class MembresiaController {

    private static final MediaType XLSX =
            MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    @Autowired
    private PaseService paseService;

    @Autowired
    private MembresiaService membresiaService;

    @Autowired
    private MembresiaReporteService reporteService;

    // ------------------------------------------------------------ Pases

    @GetMapping("/pases")
    @Operation(summary = "Pases del club", description = "Todos, con la cantidad de membresías vigentes. Solo ADMIN.")
    public ResponseEntity<List<MembresiaDTO.Pase>> pases() {
        return ResponseEntity.ok(paseService.listar());
    }

    @GetMapping("/pases/disponibles")
    @Operation(summary = "Pases a la venta hoy", description = "Para contratar desde el legajo del socio.")
    public ResponseEntity<List<MembresiaDTO.Pase>> disponibles() {
        return ResponseEntity.ok(paseService.disponibles());
    }

    @PostMapping("/pases")
    @Operation(summary = "Crear pase")
    public ResponseEntity<MembresiaDTO.Pase> crearPase(@Valid @RequestBody MembresiaDTO.PaseCommand command) {
        return ResponseEntity.ok(paseService.crear(command));
    }

    @PutMapping("/pases/{uuid}")
    @Operation(summary = "Editar pase", description = "Para dejar de venderlo, desactivarlo (las membresías existentes siguen).")
    public ResponseEntity<MembresiaDTO.Pase> editarPase(@PathVariable String uuid, @Valid @RequestBody MembresiaDTO.PaseCommand command) {
        return ResponseEntity.ok(paseService.editar(uuid, command));
    }

    @DeleteMapping("/pases/{uuid}")
    @Operation(summary = "Eliminar pase", description = "Solo si nunca se vendió.")
    public ResponseEntity<Void> eliminarPase(@PathVariable String uuid) {
        paseService.eliminar(uuid);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/config")
    @Operation(summary = "Configuración del módulo")
    public ResponseEntity<MembresiaDTO.Config> config() {
        return ResponseEntity.ok(paseService.config());
    }

    @PutMapping("/config")
    @Operation(summary = "Guardar configuración", description = "Exigir pase vigente para ingresar con el QR. Solo ADMIN.")
    public ResponseEntity<MembresiaDTO.Config> guardarConfig(@RequestBody MembresiaDTO.Config config) {
        return ResponseEntity.ok(paseService.guardarConfig(config));
    }

    // ------------------------------------------------------------ Legajo y operaciones

    @GetMapping("/socio/{memberUuid}")
    @Operation(summary = "Membresías del socio", description = "Vigente, historial, cobros, congelamientos e ingresos.")
    public ResponseEntity<MembresiaDTO.Legajo> legajo(@PathVariable String memberUuid) {
        return ResponseEntity.ok(membresiaService.legajo(memberUuid));
    }

    @PostMapping
    @Operation(summary = "Contratar pase", description = "Crea la membresía y su cuota (tipo PASE), que se cobra por tesorería.")
    public ResponseEntity<MembresiaDTO.Membresia> contratar(@Valid @RequestBody MembresiaDTO.ContratarCommand command) {
        return ResponseEntity.ok(membresiaService.contratar(command));
    }

    @PostMapping("/{uuid}/renovar")
    @Operation(summary = "Renovar", description = "Nueva membresía del mismo pase desde el día siguiente al vencimiento (o hoy si ya venció).")
    public ResponseEntity<MembresiaDTO.Membresia> renovar(@PathVariable String uuid,
                                                          @RequestBody(required = false) MembresiaDTO.RenovarCommand command) {
        return ResponseEntity.ok(membresiaService.renovar(uuid, command));
    }

    @PostMapping("/{uuid}/cambiar-plan")
    @Operation(summary = "Cambiar de plan", description = "La actual queda reemplazada (si no estaba paga, su cuota se anula).")
    public ResponseEntity<MembresiaDTO.Membresia> cambiarPlan(@PathVariable String uuid,
                                                              @Valid @RequestBody MembresiaDTO.CambiarPlanCommand command) {
        return ResponseEntity.ok(membresiaService.cambiarPlan(uuid, command));
    }

    @PostMapping("/{uuid}/congelar")
    @Operation(summary = "Congelar", description = "Suspende el pase entre dos fechas; si el pase lo indica, corre el vencimiento.")
    public ResponseEntity<MembresiaDTO.Membresia> congelar(@PathVariable String uuid,
                                                           @Valid @RequestBody MembresiaDTO.CongelarCommand command) {
        return ResponseEntity.ok(membresiaService.congelar(uuid, command));
    }

    @DeleteMapping("/congelamientos/{uuid}")
    @Operation(summary = "Anular congelamiento cargado por error", description = "Devuelve el vencimiento a como estaba.")
    public ResponseEntity<MembresiaDTO.Membresia> anularCongelamiento(@PathVariable String uuid) {
        return ResponseEntity.ok(membresiaService.anularCongelamiento(uuid));
    }

    @PostMapping("/{uuid}/baja")
    @Operation(summary = "Dar de baja", description = "Queda en el historial. Si la cuota no estaba paga, se anula.")
    public ResponseEntity<MembresiaDTO.Membresia> baja(@PathVariable String uuid,
                                                       @Valid @RequestBody MembresiaDTO.BajaCommand command) {
        return ResponseEntity.ok(membresiaService.darDeBaja(uuid, command));
    }

    // ------------------------------------------------------------ Reportes

    @PostMapping("/paginate")
    @Operation(summary = "Listado de membresías", description = "Filtros: socio, pase, situación, origen y período (fecha de inicio). Solo ADMIN.")
    public ResponseEntity<Page<MembresiaDTO.Fila>> paginar(@Valid @RequestBody MembresiaDTO.PaginateRequest request) {
        return ResponseEntity.ok(reporteService.paginar(request));
    }

    @PostMapping("/excel")
    @Operation(summary = "Exportar a Excel", description = "Mismos filtros que el listado, con resumen por pase. Solo ADMIN.")
    public ResponseEntity<byte[]> excel(@Valid @RequestBody MembresiaDTO.PaginateRequest request) {
        MembresiaReporteService.Archivo archivo = reporteService.excel(request);
        return ResponseEntity.ok()
                .contentType(XLSX)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(archivo.nombre(), StandardCharsets.UTF_8).build().toString())
                .header(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, HttpHeaders.CONTENT_DISPOSITION)
                .body(archivo.contenido());
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Indicadores de membresías", description = "Activas, vencidas, próximas a vencer, altas, renovaciones, bajas e ingresos por pase. Solo ADMIN.")
    public ResponseEntity<MembresiaDTO.Dashboard> dashboard() {
        return ResponseEntity.ok(reporteService.dashboard());
    }
}
