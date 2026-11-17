package com.blue.open.Code.Controller;

import com.blue.open.Code.Entity.DTO.CalendarioConsultaCommand;
import com.blue.open.Code.Entity.DTO.CalendarioSlotDTO;
import com.blue.open.Code.Service.Actividades.CalendarioFiltradoQueryService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@RestController
public class PortalCalendarioController {

    @Autowired
    private CalendarioFiltradoQueryService calendarioFiltradoQueryService;

    @PostMapping("portal/calendario")
    public ResponseEntity<List<CalendarioSlotDTO>> listarSocio(@Valid @RequestBody CalendarioConsultaCommand command) {
        LocalDateTime desde = command.getDesde().atStartOfDay();
        LocalDateTime hasta = command.getHasta().atTime(LocalTime.MAX);
        return ResponseEntity.ok(calendarioFiltradoQueryService.listarSocio(desde, hasta));
    }

    @PostMapping("portal/profesor/calendario")
    public ResponseEntity<List<CalendarioSlotDTO>> listarProfesor(@Valid @RequestBody CalendarioConsultaCommand command) {
        LocalDateTime desde = command.getDesde().atStartOfDay();
        LocalDateTime hasta = command.getHasta().atTime(LocalTime.MAX);
        return ResponseEntity.ok(calendarioFiltradoQueryService.listarProfesor(desde, hasta));
    }
}
