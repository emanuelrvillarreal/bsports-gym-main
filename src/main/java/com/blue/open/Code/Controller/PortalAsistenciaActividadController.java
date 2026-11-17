package com.blue.open.Code.Controller;

import java.time.LocalDate;
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
import com.blue.open.Code.Entity.DTO.AsistenciaLoteCommand;
import com.blue.open.Code.Entity.DTO.AsistenciaPaginateRequestDTO;
import com.blue.open.Code.Entity.DTO.AsistenciaPlanillaDTO;
import com.blue.open.Code.Service.Actividades.AsistenciaConsultaQueryService;
import com.blue.open.Code.Service.Actividades.AsistenciaLoteSaveService;
import com.blue.open.Code.Service.Actividades.AsistenciaPlanillaQueryService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("portal/profesor/asistencia")
public class PortalAsistenciaActividadController {

    @Autowired
    private AsistenciaPlanillaQueryService planillaQueryService;

    @Autowired
    private AsistenciaLoteSaveService loteSaveService;

    @Autowired
    private AsistenciaConsultaQueryService consultaQueryService;

    @Autowired
    private AsistenciaEstadisticasService estadisticasService;

    @PostMapping("/paginate")
    public ResponseEntity<Page<AsistenciaActividadConsultaDTO>> paginate(
            @Valid @RequestBody AsistenciaPaginateRequestDTO request) {
        return ResponseEntity.ok(consultaQueryService.paginar(request, true));
    }

    @GetMapping("/slot/{slotUuid}")
    public ResponseEntity<AsistenciaPlanillaDTO> obtenerPlanilla(@PathVariable String slotUuid) {
        return ResponseEntity.ok(planillaQueryService.obtenerPlanilla(slotUuid));
    }

    @PutMapping("/slot/{slotUuid}")
    public ResponseEntity<AsistenciaPlanillaDTO> guardarLote(
            @PathVariable String slotUuid,
            @Valid @RequestBody AsistenciaLoteCommand command) {
        return ResponseEntity.ok(loteSaveService.guardarLote(slotUuid, command));
    }

    /** Seguimiento de los alumnos de las actividades asignadas al profesor. */
    @PostMapping("/seguimiento")
    public ResponseEntity<AsistenciaEstadisticasDTO> seguimiento(
            @Valid @RequestBody AsistenciaEstadisticasRequestDTO request) {
        return ResponseEntity.ok(estadisticasService.estadisticasProfesor(request));
    }

    @GetMapping("/seguimiento/inscripcion/{inscripcionUuid}")
    public ResponseEntity<AsistenciaAlumnoDetalleDTO> detalleAlumno(
            @PathVariable String inscripcionUuid,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ResponseEntity.ok(estadisticasService.detalleProfesor(inscripcionUuid, desde, hasta));
    }

    /** Historial del alumno, limitado a las actividades del profesor. */
    @GetMapping("/seguimiento/trayectoria/{memberUuid}")
    public ResponseEntity<TrayectoriaDeportivaDTO> trayectoriaAlumno(@PathVariable String memberUuid) {
        return ResponseEntity.ok(estadisticasService.trayectoriaProfesor(memberUuid));
    }
}
