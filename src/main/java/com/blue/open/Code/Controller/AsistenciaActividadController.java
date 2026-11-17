package com.blue.open.Code.Controller;

import org.springframework.web.bind.annotation.PutMapping;
import java.time.LocalDate;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.format.annotation.DateTimeFormat;
import com.blue.open.Code.Service.Actividades.AsistenciaUmbralesService;
import com.blue.open.Code.Service.Actividades.AsistenciaEstadisticasService;
import com.blue.open.Code.Entity.DTO.TrayectoriaDeportivaDTO;
import com.blue.open.Code.Entity.DTO.AsistenciaUmbralesDTO;
import com.blue.open.Code.Entity.DTO.AsistenciaEstadisticasRequestDTO;
import com.blue.open.Code.Entity.DTO.AsistenciaEstadisticasDTO;
import com.blue.open.Code.Entity.DTO.AsistenciaAlumnoDetalleDTO;
import com.blue.open.Code.Entity.DTO.AsistenciaActividadConsultaDTO;
import com.blue.open.Code.Entity.DTO.AsistenciaPaginateRequestDTO;
import com.blue.open.Code.Service.Actividades.AsistenciaConsultaQueryService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("asistencia-actividad")
public class AsistenciaActividadController {

    @Autowired
    private AsistenciaConsultaQueryService consultaQueryService;

    @Autowired
    private AsistenciaEstadisticasService estadisticasService;

    @Autowired
    private AsistenciaUmbralesService umbralesService;

    @PostMapping("/paginate")
    public ResponseEntity<Page<AsistenciaActividadConsultaDTO>> paginate(
            @Valid @RequestBody AsistenciaPaginateRequestDTO request) {
        return ResponseEntity.ok(consultaQueryService.paginar(request, false));
    }

    /** Estadisticas de asistencia del club con filtros combinables. Solo ADMIN. */
    @PostMapping("/estadisticas")
    public ResponseEntity<AsistenciaEstadisticasDTO> estadisticas(
            @Valid @RequestBody AsistenciaEstadisticasRequestDTO request) {
        return ResponseEntity.ok(estadisticasService.estadisticasInstitucionales(request));
    }

    @GetMapping("/estadisticas/inscripcion/{inscripcionUuid}")
    public ResponseEntity<AsistenciaAlumnoDetalleDTO> detalle(
            @PathVariable String inscripcionUuid,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ResponseEntity.ok(estadisticasService.detalleInstitucional(inscripcionUuid, desde, hasta));
    }

    /** Trayectoria deportiva completa del socio (actividades actuales y anteriores). Solo ADMIN. */
    @GetMapping("/trayectoria/{memberUuid}")
    public ResponseEntity<TrayectoriaDeportivaDTO> trayectoria(@PathVariable String memberUuid) {
        return ResponseEntity.ok(estadisticasService.trayectoriaInstitucional(memberUuid));
    }

    @GetMapping("/umbrales")
    public ResponseEntity<AsistenciaUmbralesDTO> obtenerUmbrales() {
        return ResponseEntity.ok(umbralesService.obtenerActual());
    }

    @PutMapping("/umbrales")
    public ResponseEntity<AsistenciaUmbralesDTO> guardarUmbrales(@Valid @RequestBody AsistenciaUmbralesDTO request) {
        return ResponseEntity.ok(umbralesService.guardar(request));
    }
}
