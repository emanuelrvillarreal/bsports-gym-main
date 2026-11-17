package com.blue.open.Code.Controller;

import com.blue.open.Code.Auth.PermissionEnum;
import com.blue.open.Code.Entity.DTO.DescuentoAltaCommand;
import com.blue.open.Code.Entity.DTO.DescuentoAsignadoDTO;
import com.blue.open.Code.Entity.DTO.DescuentoDTO;
import com.blue.open.Code.Entity.DTO.DescuentoEdicionCommand;
import com.blue.open.Code.Entity.DTO.DescuentoFinalizarCommand;
import com.blue.open.Code.Entity.DTO.DescuentoPaginateRequestDTO;
import com.blue.open.Code.Entity.DTO.InscripcionActividadDescuentoCommand;
import com.blue.open.Code.Entity.DTO.MemberDescuentoCommand;
import com.blue.open.Code.Entity.DTO.MemberDescuentoPaginateRequestDTO;
import com.blue.open.Code.Entity.DTO.MemberDescuentosDTO;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Service.Descuento.DescuentoAltaService;
import com.blue.open.Code.Service.Descuento.DescuentoBajaService;
import com.blue.open.Code.Service.Descuento.DescuentoEdicionService;
import com.blue.open.Code.Service.Descuento.DescuentoQueryService;
import com.blue.open.Code.Service.DescuentoAsignacionService;
import com.blue.open.Code.Service.MemberDescuentoQueryService;
import com.blue.open.Core.Service.Auth.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("descuento")
public class DescuentoController {

    @Autowired
    private AuthService authService;

    @Autowired
    private MemberDescuentoQueryService memberDescuentoQueryService;

    @Autowired
    private DescuentoAsignacionService descuentoAsignacionService;

    @Autowired
    private DescuentoAltaService descuentoAltaService;

    @Autowired
    private DescuentoEdicionService descuentoEdicionService;

    @Autowired
    private DescuentoBajaService descuentoBajaService;

    @Autowired
    private DescuentoQueryService descuentoQueryService;

    @PostMapping
    public ResponseEntity<DescuentoDTO> alta(@Valid @RequestBody DescuentoAltaCommand command) {
        authService.valid(List.of(PermissionEnum.DESCUENTO_CREATE));
        return ResponseEntity.status(HttpStatus.CREATED).body(descuentoAltaService.alta(command));
    }

    @PutMapping("/{descuentoUuid}")
    public ResponseEntity<DescuentoDTO> editar(
            @PathVariable String descuentoUuid,
            @Valid @RequestBody DescuentoEdicionCommand command) {
        authService.valid(List.of(PermissionEnum.DESCUENTO_EDIT));
        return ResponseEntity.ok(descuentoEdicionService.editar(descuentoUuid, command));
    }

    @DeleteMapping("/{descuentoUuid}")
    public ResponseEntity<Void> bajaLogica(@PathVariable String descuentoUuid) {
        authService.valid(List.of(PermissionEnum.DESCUENTO_DELETE));
        descuentoBajaService.bajaLogica(descuentoUuid);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/catalogo/paginate")
    public ResponseEntity<Page<DescuentoDTO>> paginarCatalogo(
            @RequestBody(required = false) DescuentoPaginateRequestDTO request) {
        authService.valid(List.of(PermissionEnum.DESCUENTO_VIEW));
        Company company = authService.getCurrentCompanyOrThrow();
        return ResponseEntity.ok(descuentoQueryService.paginar(request, company.getId()));
    }

    @PostMapping("/paginate")
    public ResponseEntity<Page<MemberDescuentosDTO>> paginate(
            @RequestBody(required = false) MemberDescuentoPaginateRequestDTO request) {
        authService.valid(List.of(PermissionEnum.MEMBER_VIEW));
        Company company = authService.getCurrentCompanyOrThrow();
        return ResponseEntity.ok(memberDescuentoQueryService.paginar(request, company.getId()));
    }

    @PostMapping("/member/{memberUuid}")
    public ResponseEntity<DescuentoAsignadoDTO> asignarASocio(
            @PathVariable String memberUuid,
            @Valid @RequestBody MemberDescuentoCommand command) {
        authService.valid(List.of(PermissionEnum.MEMBER_EDIT));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(descuentoAsignacionService.asignarASocio(memberUuid, command));
    }

    @DeleteMapping("/member-descuento/{uuid}")
    public ResponseEntity<Void> bajaMemberDescuento(@PathVariable String uuid) {
        authService.valid(List.of(PermissionEnum.MEMBER_EDIT));
        descuentoAsignacionService.bajaMemberDescuento(uuid);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/member-descuento/{uuid}/finalizar")
    public ResponseEntity<DescuentoAsignadoDTO> finalizarMemberDescuento(
            @PathVariable String uuid,
            @Valid @RequestBody DescuentoFinalizarCommand command) {
        authService.valid(List.of(PermissionEnum.MEMBER_EDIT));
        return ResponseEntity.ok(descuentoAsignacionService.finalizarMemberDescuento(uuid, command));
    }

    @PostMapping("/inscripcion-actividad/{inscripcionActividadUuid}")
    public ResponseEntity<DescuentoAsignadoDTO> asignarAInscripcion(
            @PathVariable String inscripcionActividadUuid,
            @Valid @RequestBody InscripcionActividadDescuentoCommand command) {
        authService.valid(List.of(PermissionEnum.ACTIVITY_ENROLL));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(descuentoAsignacionService.asignarAInscripcion(inscripcionActividadUuid, command));
    }

    @DeleteMapping("/inscripcion-actividad-descuento/{uuid}")
    public ResponseEntity<Void> bajaInscripcionActividadDescuento(@PathVariable String uuid) {
        authService.valid(List.of(PermissionEnum.ACTIVITY_ENROLL));
        descuentoAsignacionService.bajaInscripcionActividadDescuento(uuid);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/inscripcion-actividad-descuento/{uuid}/finalizar")
    public ResponseEntity<DescuentoAsignadoDTO> finalizarInscripcionActividadDescuento(
            @PathVariable String uuid,
            @Valid @RequestBody DescuentoFinalizarCommand command) {
        authService.valid(List.of(PermissionEnum.ACTIVITY_ENROLL));
        return ResponseEntity.ok(descuentoAsignacionService.finalizarInscripcionActividadDescuento(uuid, command));
    }
}
