package com.blue.open.Code.Controller;

import com.blue.open.Code.Auth.PermissionEnum;
import com.blue.open.Code.Entity.DTO.AmenityAltaCommand;
import com.blue.open.Code.Entity.DTO.AmenityDTO;
import com.blue.open.Code.Entity.DTO.AmenityEdicionCommand;
import com.blue.open.Code.Entity.DTO.ReservaAmenityAltaCommand;
import com.blue.open.Code.Entity.DTO.ReservaAmenityDTO;
import com.blue.open.Code.Service.Amenities.AmenityAltaService;
import com.blue.open.Code.Service.Amenities.AmenityBajaService;
import com.blue.open.Code.Service.Amenities.AmenityEdicionService;
import com.blue.open.Code.Service.Amenities.AmenityListadoService;
import com.blue.open.Code.Service.Amenities.ReservaAmenityAltaService;
import com.blue.open.Core.Service.Auth.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("amenities")
public class AmenityController {

    @Autowired
    private AmenityAltaService amenityAltaService;

    @Autowired
    private AmenityEdicionService amenityEdicionService;

    @Autowired
    private AmenityBajaService amenityBajaService;

    @Autowired
    private AmenityListadoService amenityListadoService;

    @Autowired
    private ReservaAmenityAltaService reservaAmenityAltaService;

    @Autowired
    private AuthService authService;

    @PostMapping
    public ResponseEntity<AmenityDTO> alta(@Valid @RequestBody AmenityAltaCommand command) {
        return ResponseEntity.ok(amenityAltaService.alta(command));
    }

    @PutMapping("/{uuid}")
    public ResponseEntity<AmenityDTO> editar(
            @PathVariable String uuid,
            @Valid @RequestBody AmenityEdicionCommand command) {
        return ResponseEntity.ok(amenityEdicionService.editar(uuid, command));
    }

    @DeleteMapping("/{uuid}")
    public ResponseEntity<Void> desactivar(@PathVariable String uuid) {
        amenityBajaService.desactivar(uuid);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<List<AmenityDTO>> listar() {
        return ResponseEntity.ok(amenityListadoService.listarActivasStaff());
    }

    @PostMapping("/reservas")
    public ResponseEntity<ReservaAmenityDTO> crearReserva(@Valid @RequestBody ReservaAmenityAltaCommand command) {
        authService.valid(List.of(PermissionEnum.AMENITY_RESERVATION_CREATE));
        return ResponseEntity.ok(reservaAmenityAltaService.crear(command));
    }
}
