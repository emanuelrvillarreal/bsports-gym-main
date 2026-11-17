package com.blue.open.Code.Controller;

import com.blue.open.Code.Auth.PermissionEnum;
import com.blue.open.Code.Auth.RoleEnum;
import com.blue.open.Code.Entity.DTO.CertificadoMedicoDTO;
import com.blue.open.Code.Entity.DTO.CertificadoMedicoEstadoUpdateCommand;
import com.blue.open.Code.Entity.DTO.CertificadoMedicoUpdateCommand;
import com.blue.open.Code.Entity.DTO.MemberAprobarRegistroCommand;
import com.blue.open.Code.Entity.DTO.MemberBackofficeUpdateCommand;
import com.blue.open.Code.Entity.DTO.MemberDTO;
import com.blue.open.Code.Entity.DTO.MemberFichaDTO;
import com.blue.open.Code.Service.MemberFichaService;
import com.blue.open.Code.Entity.DTO.MemberEstadoUpdateCommand;
import com.blue.open.Code.Entity.DTO.MemberFamiliarAltaCommand;
import com.blue.open.Code.Entity.DTO.MemberPaginateRequestDTO;
import com.blue.open.Code.Entity.DTO.MemberPortalFamiliarAltaCommand;
import com.blue.open.Code.Entity.DTO.MemberPublicRegistroCommand;
import com.blue.open.Code.Entity.DTO.MemberRegistroRechazoCommand;
import com.blue.open.Code.Entity.DTO.MemberSolicitudPaginateRequestDTO;
import com.blue.open.Code.Entity.DTO.MemberSolicitudRegistroDTO;
import com.blue.open.Code.Entity.DTO.MemberTitularAltaCommand;
import com.blue.open.Code.Entity.DTO.MemberBulkImportResultsPageDTO;
import com.blue.open.Code.Entity.DTO.MemberBulkImportResultDetailDTO;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Service.CertificadoMedicoEstadoUpdateService;
import com.blue.open.Code.Service.CertificadoMedicoQueryService;
import com.blue.open.Code.Service.CertificadoMedicoUpdateService;
import com.blue.open.Code.Service.CertificadoMedicoUploadService;
import com.blue.open.Code.Service.MemberBackofficeUpdateService;
import com.blue.open.Code.Service.MemberBajaService;
import com.blue.open.Code.Service.MemberBulkImportContext;
import com.blue.open.Code.Service.MemberBulkImportProgressService;
import com.blue.open.Code.Service.MemberBulkImportResultService;
import com.blue.open.Code.Service.MemberBulkImportService;
import com.blue.open.Code.Service.MemberEstadoUpdateService;
import com.blue.open.Code.Service.MemberFamiliarService;
import com.blue.open.Code.Service.MemberPortalFamiliarService;
import com.blue.open.Code.Service.MemberPublicRegistroService;
import com.blue.open.Code.Service.MemberQueryService;
import com.blue.open.Code.Service.MemberRegistroAprobacionService;
import com.blue.open.Code.Service.MemberSolicitudRegistroService;
import com.blue.open.Code.Service.MemberTitularService;
import com.blue.open.Core.Auth.BaseRoleEnum;
import com.blue.open.Core.Service.Auth.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("member")
public class MemberController {

    @Autowired
    private MemberTitularService memberTitularService;

    @Autowired
    private com.blue.open.Code.Service.MemberNumeroSocioService memberNumeroSocioService;

    @Autowired
    private MemberFamiliarService memberFamiliarService;

    @Autowired
    private MemberBackofficeUpdateService memberBackofficeUpdateService;

    @Autowired
    private CertificadoMedicoUploadService certificadoMedicoUploadService;

    @Autowired
    private CertificadoMedicoEstadoUpdateService certificadoMedicoEstadoUpdateService;

    @Autowired
    private CertificadoMedicoUpdateService certificadoMedicoUpdateService;

    @Autowired
    private CertificadoMedicoQueryService certificadoMedicoQueryService;

    @Autowired
    private com.blue.open.Code.Service.CertificadoMedicoResumenService certificadoMedicoResumenService;

    @Autowired
    private MemberBajaService memberBajaService;

    @Autowired
    private MemberPublicRegistroService memberPublicRegistroService;

    @Autowired
    private MemberPortalFamiliarService memberPortalFamiliarService;

    @Autowired
    private AuthService authService;

    @Autowired
    private MemberQueryService memberQueryService;

    @Autowired
    private MemberBulkImportService memberBulkImportService;

    @Autowired
    private MemberBulkImportProgressService memberBulkImportProgressService;

    @Autowired
    private MemberBulkImportResultService memberBulkImportResultService;

    @Autowired
    private MemberEstadoUpdateService memberEstadoUpdateService;

    @Autowired
    private MemberSolicitudRegistroService memberSolicitudRegistroService;

    @Autowired
    private MemberRegistroAprobacionService memberRegistroAprobacionService;

    @Autowired
    private MemberFichaService memberFichaService;

    @Autowired
    private com.blue.open.Code.Service.MemberFotoService memberFotoService;

    @PostMapping("/paginate")
    public ResponseEntity<Page<MemberDTO>> paginate(
            @RequestBody(required = false) MemberPaginateRequestDTO request) {
        authService.valid(List.of(PermissionEnum.MEMBER_VIEW));
        Company company = authService.getCurrentCompanyOrThrow();
        return ResponseEntity.ok(memberQueryService.paginar(request, company.getId()));
    }

    @PostMapping("/solicitudes/paginate")
    public ResponseEntity<Page<MemberSolicitudRegistroDTO>> paginarSolicitudes(
            @RequestBody(required = false) MemberSolicitudPaginateRequestDTO request) {
        return ResponseEntity.ok(memberSolicitudRegistroService.paginar(request));
    }

    @PostMapping("/uuid/{memberUuid}/aprobar-registro")
    public ResponseEntity<MemberDTO> aprobarRegistro(
            @PathVariable String memberUuid,
            @RequestBody(required = false) MemberAprobarRegistroCommand command) {
        return ResponseEntity.ok(memberRegistroAprobacionService.aprobarRegistro(memberUuid, command));
    }

    @PostMapping("/uuid/{memberUuid}/rechazar-registro")
    public ResponseEntity<MemberDTO> rechazarRegistro(
            @PathVariable String memberUuid,
            @RequestBody(required = false) MemberRegistroRechazoCommand command) {
        return ResponseEntity.ok(memberRegistroAprobacionService.rechazarRegistro(memberUuid, command));
    }

    @PostMapping("/public/titular")
    public ResponseEntity<MemberDTO> registroTitularPublico(
            @RequestHeader(value = "X-Company-Uuid", required = false) String companyUuid,
            @Valid @RequestBody MemberPublicRegistroCommand command) {
        return ResponseEntity.ok(memberPublicRegistroService.registroTitular(companyUuid, command));
    }

    @PostMapping("/titular")
    public ResponseEntity<MemberDTO> altaTitular(@Valid @RequestBody MemberTitularAltaCommand command) {
        return ResponseEntity.ok(memberTitularService.altaTitular(command));
    }

    /** Número que se asignará al próximo socio si no se elige uno. */
    @GetMapping("/numero-socio/siguiente")
    public ResponseEntity<Map<String, String>> siguienteNumeroSocio() {
        return ResponseEntity.ok(Map.of("numeroSocio", memberNumeroSocioService.siguienteDelClubActual()));
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> importar(@RequestPart("file") MultipartFile file) {
        MemberBulkImportContext context = memberBulkImportService.preparar(file);
        memberBulkImportService.importar(context);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("message", "Procesando");
        response.put("importId", context.importId());
        response.put("filasTotales", context.rows().size());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @GetMapping(value = "/import/{importId}/progress", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter progresoImportacion(@PathVariable String importId) {
        return memberBulkImportProgressService.suscribir(importId);
    }

    @PostMapping("/portal/familiar")
    public ResponseEntity<MemberDTO> altaFamiliarPortal(
            @Valid @RequestBody MemberPortalFamiliarAltaCommand command) {
        return ResponseEntity.ok(memberPortalFamiliarService.altaFamiliar(command));
    }

    @PostMapping("/familiar")
    public ResponseEntity<MemberDTO> altaFamiliar(@Valid @RequestBody MemberFamiliarAltaCommand command) {
        return ResponseEntity.ok(memberFamiliarService.altaFamiliar(command));
    }

    @PutMapping("/uuid/{memberUuid}")
    public ResponseEntity<MemberDTO> actualizar(
            @PathVariable String memberUuid,
            @Valid @RequestBody MemberBackofficeUpdateCommand command) {
        return ResponseEntity.ok(memberBackofficeUpdateService.actualizar(memberUuid, command));
    }

    @GetMapping("/uuid/{memberUuid}/ficha")
    public ResponseEntity<MemberFichaDTO> ficha(@PathVariable String memberUuid) {
        return ResponseEntity.ok(memberFichaService.ficha(memberUuid));
    }

    @PostMapping(value = "/uuid/{memberUuid}/foto", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MemberDTO> subirFoto(
            @PathVariable String memberUuid,
            @RequestPart("file") MultipartFile file) {
        return ResponseEntity.ok(memberFotoService.subir(memberUuid, file));
    }

    @org.springframework.web.bind.annotation.DeleteMapping("/uuid/{memberUuid}/foto")
    public ResponseEntity<MemberDTO> eliminarFoto(@PathVariable String memberUuid) {
        return ResponseEntity.ok(memberFotoService.eliminar(memberUuid));
    }

    @GetMapping("/uuid/{memberUuid}/familiares")
    public ResponseEntity<List<MemberDTO>> familiares(@PathVariable String memberUuid) {
        authService.valid(List.of(PermissionEnum.MEMBER_VIEW));
        Company company = authService.getCurrentCompanyOrThrow();
        return ResponseEntity.ok(memberQueryService.listarFamiliares(memberUuid, company.getId()));
    }

    @PostMapping("/uuid/{memberUuid}/baja")
    public ResponseEntity<MemberDTO> darBaja(@PathVariable String memberUuid) {
        return ResponseEntity.ok(memberBajaService.darBajaLogica(memberUuid));
    }

    @PutMapping("/uuid/{memberUuid}/estado")
    public ResponseEntity<MemberDTO> actualizarEstado(
            @PathVariable String memberUuid,
            @Valid @RequestBody MemberEstadoUpdateCommand command) {
        return ResponseEntity.ok(memberEstadoUpdateService.actualizarEstado(memberUuid, command));
    }

    @PostMapping(value = "/uuid/{memberUuid}/certificado", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CertificadoMedicoDTO> subirCertificado(
            @PathVariable String memberUuid,
            @RequestPart("file") MultipartFile file) {
        authService.valid(List.of(PermissionEnum.MEMBER_CERTIFICADO_UPLOAD));
        return ResponseEntity.ok(certificadoMedicoUploadService.subir(memberUuid, file, true));
    }

    @PutMapping("/certificado/uuid/{certificadoUuid}/estado")
    public ResponseEntity<CertificadoMedicoDTO> actualizarEstadoCertificado(
            @PathVariable String certificadoUuid,
            @Valid @RequestBody CertificadoMedicoEstadoUpdateCommand command) {
        authService.valid(List.of(PermissionEnum.MEMBER_CERTIFICADO_CHANGE_STATUS));
        return ResponseEntity.ok(certificadoMedicoEstadoUpdateService.actualizarEstado(certificadoUuid, command));
    }

    @PutMapping("/certificado/uuid/{certificadoUuid}")
    public ResponseEntity<CertificadoMedicoDTO> actualizarCertificado(
            @PathVariable String certificadoUuid,
            @Valid @RequestBody CertificadoMedicoUpdateCommand command) {
        authService.valid(List.of(PermissionEnum.MEMBER_CERTIFICADO_CHANGE_STATUS));
        return ResponseEntity.ok(certificadoMedicoUpdateService.actualizar(certificadoUuid, command));
    }

    @PostMapping("/certificados/resumen")
    public ResponseEntity<com.blue.open.Code.Entity.DTO.CertificadoMedicoResumenDTO> resumenCertificados(
            @RequestBody(required = false) com.blue.open.Code.Entity.DTO.CertificadoMedicoResumenRequestDTO request) {
        authService.valid(List.of(PermissionEnum.MEMBER_VIEW));
        Company company = authService.getCurrentCompanyOrThrow();
        return ResponseEntity.ok(certificadoMedicoResumenService.resumir(request, company.getId()));
    }

    @GetMapping("/uuid/{memberUuid}/certificado")
    public ResponseEntity<List<CertificadoMedicoDTO>> listarCertificados(@PathVariable String memberUuid) {
        authService.valid(List.of(PermissionEnum.MEMBER_VIEW));
        Company company = authService.getCurrentCompanyOrThrow();
        return ResponseEntity.ok(certificadoMedicoQueryService.listar(memberUuid, company.getId()));
    }

    @GetMapping("/import/results")
    public ResponseEntity<MemberBulkImportResultsPageDTO> obtenerResultadosImportacion(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        authService.valid(List.of(PermissionEnum.MEMBER_CREATE, PermissionEnum.FAMILY_CREATE));
        Company company = authService.getCurrentCompanyOrThrow();
        return ResponseEntity.ok(memberBulkImportResultService.obtenerResultados(company.getId(), page, size));
    }

    @GetMapping("/import/{importId}/result")
    public ResponseEntity<MemberBulkImportResultDetailDTO> obtenerDetalleImportacion(@PathVariable String importId) {
        authService.valid(List.of(PermissionEnum.MEMBER_CREATE, PermissionEnum.FAMILY_CREATE));
        return ResponseEntity.ok(memberBulkImportResultService.obtenerDetalle(importId));
    }
}
