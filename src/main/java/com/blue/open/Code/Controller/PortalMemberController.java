package com.blue.open.Code.Controller;

import com.blue.open.Code.Auth.PermissionEnum;
import com.blue.open.Code.Entity.DTO.CertificadoMedicoDTO;
import com.blue.open.Code.Entity.DTO.MemberDTO;
import com.blue.open.Code.Entity.DTO.MemberPortalProfileCommand;
import com.blue.open.Code.Entity.DTO.PortalMemberCredencialDTO;
import com.blue.open.Code.Entity.DTO.PortalMemberGrupoDTO;
import com.blue.open.Code.Service.CertificadoMedicoUploadService;
import com.blue.open.Code.Service.MemberPortalCredencialService;
import com.blue.open.Code.Service.MemberPortalProfileService;
import com.blue.open.Core.Service.Auth.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("portal/member")
public class PortalMemberController {

    @Autowired
    private MemberPortalProfileService memberPortalProfileService;

    @Autowired
    private MemberPortalCredencialService memberPortalCredencialService;

    @Autowired
    private AuthService authService;

    @Autowired
    private com.blue.open.Code.Service.MemberDescuentoQueryService memberDescuentoQueryService;

    @Autowired
    private CertificadoMedicoUploadService certificadoMedicoUploadService;

    @GetMapping("/credencial")
    public ResponseEntity<PortalMemberCredencialDTO> obtenerCredencial() {
        return ResponseEntity.ok(memberPortalCredencialService.obtenerCredencial());
    }

    @GetMapping("/descuentos")
    public ResponseEntity<java.util.List<com.blue.open.Code.Entity.DTO.MemberDescuentosDTO>> misDescuentos() {
        return ResponseEntity.ok(memberDescuentoQueryService.portal());
    }

    @GetMapping("/grupo")
    public ResponseEntity<PortalMemberGrupoDTO> obtenerGrupoFamiliar() {
        return ResponseEntity.ok(memberPortalCredencialService.obtenerGrupoFamiliar());
    }

    @GetMapping("/perfil")
    public ResponseEntity<MemberDTO> obtenerPerfil() {
        return ResponseEntity.ok(memberPortalProfileService.obtenerPerfil());
    }

    @PutMapping("/perfil")
    public ResponseEntity<MemberDTO> actualizarPerfil(
            @Valid @RequestBody MemberPortalProfileCommand command,
            @RequestParam(required = false) String memberUuid) {
        return ResponseEntity.ok(memberPortalProfileService.actualizarPerfil(command, memberUuid));
    }

    @PostMapping(value = "/uuid/{memberUuid}/certificado", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CertificadoMedicoDTO> subirCertificado(
            @PathVariable String memberUuid,
            @RequestPart("file") MultipartFile file) {
        authService.valid(List.of(PermissionEnum.PORTAL_CERTIFICADO_UPLOAD));
        return ResponseEntity.ok(certificadoMedicoUploadService.subir(memberUuid, file, false));
    }
}
