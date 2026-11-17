package com.blue.open.Code.Controller;

import com.blue.open.Code.Auth.PermissionEnum;
import com.blue.open.Code.Entity.DTO.ReservaAmenityCobroCommand;
import com.blue.open.Code.Entity.DTO.ReservaAmenityCobroResultDTO;
import com.blue.open.Code.Entity.DTO.ReservaAmenityDTO;
import com.blue.open.Code.Entity.DTO.ReservaPaginateRequestDTO;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Service.Amenities.ReservaAmenityCancelacionService;
import com.blue.open.Code.Service.Amenities.ReservaAmenityCobroService;
import com.blue.open.Code.Service.Amenities.ReservaAmenityConfirmacionService;
import com.blue.open.Code.Service.Amenities.ReservaQueryService;
import com.blue.open.Core.Service.Auth.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("amenities/reservas")
public class AmenityReservaCobroController {

    @Autowired
    private AuthService authService;

    @Autowired
    private ReservaAmenityCobroService reservaAmenityCobroService;

    @Autowired
    private ReservaAmenityConfirmacionService reservaAmenityConfirmacionService;

    @Autowired
    private ReservaAmenityCancelacionService reservaAmenityCancelacionService;

    @Autowired
    private ReservaQueryService reservaQueryService;

    @PostMapping("/paginate")
    public ResponseEntity<Page<ReservaAmenityDTO>> paginate(
            @RequestBody(required = false) ReservaPaginateRequestDTO request) {
        authService.valid(List.of(PermissionEnum.AMENITY_VIEW));
        Company company = authService.getCurrentCompanyOrThrow();
        return ResponseEntity.ok(reservaQueryService.paginar(request, company.getId()));
    }

    @PostMapping("/{uuid}/confirmar")
    public ResponseEntity<ReservaAmenityDTO> confirmar(@PathVariable String uuid) {
        authService.valid(List.of(PermissionEnum.AMENITY_RESERVATION_COLLECT));
        return ResponseEntity.ok(reservaAmenityConfirmacionService.confirmar(uuid));
    }

    @PostMapping("/{uuid}/cobro-sena")
    public ResponseEntity<ReservaAmenityCobroResultDTO> cobroSena(
            @PathVariable String uuid,
            @Valid @RequestBody ReservaAmenityCobroCommand command) {
        authService.valid(List.of(PermissionEnum.TREASURY_COLLECT, PermissionEnum.AMENITY_RESERVATION_COLLECT));
        return ResponseEntity.ok(reservaAmenityCobroService.registrarSena(uuid, command));
    }

    @PostMapping("/{uuid}/cobro-saldo")
    public ResponseEntity<ReservaAmenityCobroResultDTO> cobroSaldo(
            @PathVariable String uuid,
            @Valid @RequestBody ReservaAmenityCobroCommand command) {
        authService.valid(List.of(PermissionEnum.TREASURY_COLLECT, PermissionEnum.AMENITY_RESERVATION_COLLECT));
        return ResponseEntity.ok(reservaAmenityCobroService.registrarSaldo(uuid, command));
    }

    @PostMapping("/{uuid}/cobro-total")
    public ResponseEntity<ReservaAmenityCobroResultDTO> cobroTotal(
            @PathVariable String uuid,
            @Valid @RequestBody ReservaAmenityCobroCommand command) {
        authService.valid(List.of(PermissionEnum.TREASURY_COLLECT, PermissionEnum.AMENITY_RESERVATION_COLLECT));
        return ResponseEntity.ok(reservaAmenityCobroService.registrarTotal(uuid, command));
    }

    @DeleteMapping("/{uuid}")
    public ResponseEntity<ReservaAmenityDTO> cancelar(@PathVariable String uuid) {
        return ResponseEntity.ok(reservaAmenityCancelacionService.cancelar(uuid));
    }
}
