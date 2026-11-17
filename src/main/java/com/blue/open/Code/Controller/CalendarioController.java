package com.blue.open.Code.Controller;

import com.blue.open.Code.Entity.DTO.CalendarioConsultaCommand;
import com.blue.open.Code.Entity.DTO.CalendarioSlotDTO;
import com.blue.open.Code.Service.Amenities.CalendarioQueryService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("calendario")
public class CalendarioController {

    @Autowired
    private CalendarioQueryService calendarioQueryService;

    @PostMapping
    public ResponseEntity<List<CalendarioSlotDTO>> listar(@Valid @RequestBody CalendarioConsultaCommand command) {
        LocalDateTime desde = command.getDesde().atStartOfDay();
        LocalDateTime hasta = command.getHasta().atTime(LocalTime.MAX);
        return ResponseEntity.ok(calendarioQueryService.listar(desde, hasta));
    }
}
