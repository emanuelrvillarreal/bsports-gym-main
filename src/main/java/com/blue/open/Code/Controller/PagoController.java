package com.blue.open.Code.Controller;

import com.blue.open.Code.Entity.DTO.PagoDetalleDTO;
import com.blue.open.Code.Service.Tesoreria.Cobro.MercadoPagoCobroConsultaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("pago")
@RequiredArgsConstructor
public class PagoController {

    private final MercadoPagoCobroConsultaService mercadoPagoCobroConsultaService;

    @GetMapping("/{externalReferenceMp}")
    @Operation(summary = "Obtener pago por external reference de Mercado Pago",
            description = "Devuelve título, detalle, monto total, estado e ítems del cobro asociado a la external reference. Requiere JWT y X-Company-Uuid.")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<PagoDetalleDTO> obtenerPorExternalReference(@PathVariable String externalReferenceMp) {
        return ResponseEntity.ok(mercadoPagoCobroConsultaService.obtenerPorExternalReference(externalReferenceMp));
    }
}
