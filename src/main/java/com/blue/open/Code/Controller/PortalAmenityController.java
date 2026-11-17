package com.blue.open.Code.Controller;

import com.blue.open.Code.Auth.PermissionEnum;
import com.blue.open.Code.Entity.DTO.AmenityDTO;
import com.blue.open.Code.Entity.DTO.PortalReservaAmenityAltaCommand;
import com.blue.open.Code.Entity.DTO.PortalReservaAmenityPagoCommand;
import com.blue.open.Code.Entity.DTO.PortalReservaPaginateRequestDTO;
import com.blue.open.Code.Entity.DTO.ReservaAmenityCobroResultDTO;
import com.blue.open.Code.Entity.DTO.ReservaAmenityDTO;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.Member;
import com.blue.open.Code.Service.Amenities.AmenityListadoService;
import com.blue.open.Code.Service.Amenities.PortalAmenityReservaService;
import com.blue.open.Code.Service.Amenities.ReservaQueryService;
import com.blue.open.Code.Service.MemberAuthService;
import com.blue.open.Core.Service.Auth.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("portal/amenities")
public class PortalAmenityController {

    @Autowired
    private AuthService authService;

    @Autowired
    private MemberAuthService memberAuthService;

    @Autowired
    private AmenityListadoService amenityListadoService;

    @Autowired
    private ReservaQueryService reservaQueryService;

    @Autowired
    private PortalAmenityReservaService portalAmenityReservaService;

    @GetMapping
    public ResponseEntity<List<AmenityDTO>> listar() {
        authService.valid(List.of(PermissionEnum.PORTAL_AMENITIES_VIEW));
        Company company = authService.getCurrentCompanyOrThrow();
        Member member = memberAuthService.getCurrentMemberOrThrow(company.getId());
        memberAuthService.requireTitular(member);
        memberAuthService.requirePortalEligible(member);
        return ResponseEntity.ok(amenityListadoService.listarActivasPortal(company.getId()));
    }

    @PostMapping("/reservas/paginate")
    public ResponseEntity<Page<ReservaAmenityDTO>> paginarReservas(
            @RequestBody(required = false) PortalReservaPaginateRequestDTO request) {
        authService.valid(List.of(PermissionEnum.PORTAL_AMENITIES_VIEW));
        Company company = authService.getCurrentCompanyOrThrow();
        Member member = memberAuthService.getCurrentMemberOrThrow(company.getId());
        memberAuthService.requireTitular(member);
        return ResponseEntity.ok(
                reservaQueryService.paginarPorMember(request, company.getId(), member.getId()));
    }

    @PostMapping("/{amenityUuid}/reservar")
    public ResponseEntity<ReservaAmenityCobroResultDTO> reservar(
            @PathVariable String amenityUuid,
            @Valid @RequestBody PortalReservaAmenityAltaCommand command) {
        return ResponseEntity.ok(portalAmenityReservaService.reservar(amenityUuid, command));
    }

    @PostMapping("/reservas/{reservaUuid}/pago")
    public ResponseEntity<ReservaAmenityCobroResultDTO> reintentarPago(
            @PathVariable String reservaUuid,
            @RequestBody(required = false) PortalReservaAmenityPagoCommand command) {
        return ResponseEntity.ok(portalAmenityReservaService.reintentarPago(reservaUuid, command));
    }
}
