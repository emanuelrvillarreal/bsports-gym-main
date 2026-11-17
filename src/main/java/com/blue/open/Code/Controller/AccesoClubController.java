package com.blue.open.Code.Controller;

import com.blue.open.Code.Auth.PermissionEnum;
import com.blue.open.Code.Entity.DTO.AccesoClubItemDTO;
import com.blue.open.Code.Entity.DTO.AccesoClubRegistroCommand;
import com.blue.open.Code.Entity.DTO.AccesoClubRegistroDTO;
import com.blue.open.Code.Entity.DTO.ClubPresenciaListDTO;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Service.AccesoClub.AccesoClubPresenciaLiveService;
import com.blue.open.Code.Service.AccesoClub.AccesoClubQueryService;
import com.blue.open.Code.Service.AccesoClub.AccesoClubRegistroService;
import com.blue.open.Code.Service.AccesoClub.ClubPresenciaQueryService;
import com.blue.open.Core.Service.Auth.AuthService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("acceso-club")
public class AccesoClubController {

    @Autowired
    private AccesoClubRegistroService accesoClubRegistroService;

    @Autowired
    private AccesoClubQueryService accesoClubQueryService;

    @Autowired
    private ClubPresenciaQueryService clubPresenciaQueryService;

    @Autowired
    private AccesoClubPresenciaLiveService accesoClubPresenciaLiveService;

    @Autowired
    private AuthService authService;

    @PostMapping
    public ResponseEntity<AccesoClubRegistroDTO> registrar(@Valid @RequestBody AccesoClubRegistroCommand command) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(accesoClubRegistroService.registrar(command.getUserUuid(), command.getActividadUuid(), command.getAmenityUuid()));
    }

    @GetMapping
    public ResponseEntity<List<AccesoClubItemDTO>> listarPorRango(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        LocalDateTime desdeDt = desde.atStartOfDay();
        LocalDateTime hastaDt = hasta.atTime(LocalTime.MAX);
        return ResponseEntity.ok(accesoClubQueryService.listarPorRango(desdeDt, hastaDt));
    }

    @GetMapping("/presentes")
    public ResponseEntity<ClubPresenciaListDTO> listarPresentes() {
        return ResponseEntity.ok(clubPresenciaQueryService.listarPorPresente(true));
    }

    @GetMapping(value = "/live", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter suscribirAActualizaciones() {
        authService.valid(List.of(PermissionEnum.CLUB_ACCESS_VIEW));
        Company company = authService.getCurrentCompanyOrThrow();
        return accesoClubPresenciaLiveService.suscribir(company.getId());
    }
}
