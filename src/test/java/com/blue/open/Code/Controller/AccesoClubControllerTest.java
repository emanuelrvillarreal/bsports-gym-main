package com.blue.open.Code.Controller;

import com.blue.open.Code.Entity.DTO.AccesoClubItemDTO;
import com.blue.open.Code.Entity.DTO.AccesoClubRegistroCommand;
import com.blue.open.Code.Entity.DTO.AccesoClubRegistroDTO;
import com.blue.open.Code.Entity.DTO.ClubPresenciaItemDTO;
import com.blue.open.Code.Entity.DTO.ClubPresenciaListDTO;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.User;
import com.blue.open.Code.Entity.Enums.AccesoClubTipoEnum;
import com.blue.open.Code.Service.AccesoClub.AccesoClubPresenciaLiveService;
import com.blue.open.Code.Service.AccesoClub.AccesoClubQueryService;
import com.blue.open.Code.Service.AccesoClub.AccesoClubRegistroService;
import com.blue.open.Code.Service.AccesoClub.ClubPresenciaQueryService;
import com.blue.open.Core.Service.Auth.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccesoClubControllerTest {

    private static final String USER_UUID = "user-uuid-1";
    private static final LocalDate DESDE = LocalDate.of(2026, 6, 17);
    private static final LocalDate HASTA = LocalDate.of(2026, 6, 18);

    private StubAccesoClubRegistroService stubRegistroService;
    private StubAccesoClubQueryService stubQueryService;
    private StubClubPresenciaQueryService stubPresenciaQueryService;
    private StubAccesoClubPresenciaLiveService stubLiveService;
    private StubAuthService stubAuthService;

    private AccesoClubController controller;

    @BeforeEach
    void setUp() {
        stubRegistroService = new StubAccesoClubRegistroService();
        stubQueryService = new StubAccesoClubQueryService();
        stubPresenciaQueryService = new StubClubPresenciaQueryService();
        stubLiveService = new StubAccesoClubPresenciaLiveService();
        stubAuthService = new StubAuthService();

        controller = new AccesoClubController();
        ReflectionTestUtils.setField(controller, "accesoClubRegistroService", stubRegistroService);
        ReflectionTestUtils.setField(controller, "accesoClubQueryService", stubQueryService);
        ReflectionTestUtils.setField(controller, "clubPresenciaQueryService", stubPresenciaQueryService);
        ReflectionTestUtils.setField(controller, "accesoClubPresenciaLiveService", stubLiveService);
        ReflectionTestUtils.setField(controller, "authService", stubAuthService);
    }

    @Test
    void registrar_delegaAlServiceYRetorna201() {
        AccesoClubRegistroCommand command = new AccesoClubRegistroCommand();
        command.setUserUuid(USER_UUID);

        AccesoClubRegistroDTO expected = new AccesoClubRegistroDTO();
        expected.setAccesoUuid("acceso-uuid");
        expected.setUserUuid(USER_UUID);
        expected.setTipo(AccesoClubTipoEnum.INGRESO);
        expected.setPresente(true);
        stubRegistroService.setResult(expected);

        ResponseEntity<AccesoClubRegistroDTO> response = controller.registrar(command);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(AccesoClubTipoEnum.INGRESO, response.getBody().getTipo());
        assertTrue(response.getBody().isPresente());
        assertEquals(USER_UUID, stubRegistroService.getLastUserUuid());
    }

    @Test
    void listarPorRango_delegaAlServiceConRangoExpandidoYRetorna200() {
        AccesoClubItemDTO item = new AccesoClubItemDTO();
        item.setAccesoUuid("acceso-uuid");
        item.setUserUuid(USER_UUID);
        item.setTipo(AccesoClubTipoEnum.EGRESO);
        stubQueryService.setResult(List.of(item));

        ResponseEntity<List<AccesoClubItemDTO>> response = controller.listarPorRango(DESDE, HASTA);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
        assertEquals(AccesoClubTipoEnum.EGRESO, response.getBody().get(0).getTipo());
        assertEquals(DESDE.atStartOfDay(), stubQueryService.getLastDesde());
        assertEquals(HASTA.atTime(LocalTime.MAX), stubQueryService.getLastHasta());
    }

    @Test
    void listarPresentes_delegaAlServiceYRetorna200() {
        ClubPresenciaItemDTO item = new ClubPresenciaItemDTO();
        item.setUserUuid(USER_UUID);
        item.setNombre("Juan");
        item.setPresente(true);
        item.setUltimoAccesoAt(LocalDateTime.of(2026, 6, 17, 10, 30));
        stubPresenciaQueryService.setResult(new ClubPresenciaListDTO(List.of(item)));

        ResponseEntity<ClubPresenciaListDTO> response = controller.listarPresentes();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getCount());
        assertEquals(1, response.getBody().getPresentes().size());
        assertTrue(response.getBody().getPresentes().get(0).isPresente());
        assertTrue(stubPresenciaQueryService.isLastPresente());
    }

    @Test
    void suscribirAActualizaciones_delegaAlServiceConCompanyIdDeLaCompanyActual() {
        Company company = new Company();
        company.setId(77L);
        stubAuthService.setCompany(company);

        SseEmitter expected = new SseEmitter();
        stubLiveService.setResult(expected);

        SseEmitter result = controller.suscribirAActualizaciones();

        assertEquals(expected, result);
        assertEquals(77L, stubLiveService.getLastCompanyId());
    }

    private static class StubAccesoClubRegistroService extends AccesoClubRegistroService {

        private AccesoClubRegistroDTO result;
        private String lastUserUuid;

        StubAccesoClubRegistroService() {
            super(null, null, null, null, null, null, null, null, null);
        }

        void setResult(AccesoClubRegistroDTO result) {
            this.result = result;
        }

        String getLastUserUuid() {
            return lastUserUuid;
        }

        @Override
        public AccesoClubRegistroDTO registrar(String userUuid, String actividadUuid, String amenityUuid) {
            lastUserUuid = userUuid;
            return result;
        }
    }

    private static class StubAccesoClubQueryService extends AccesoClubQueryService {

        private List<AccesoClubItemDTO> result;
        private LocalDateTime lastDesde;
        private LocalDateTime lastHasta;

        void setResult(List<AccesoClubItemDTO> result) {
            this.result = result;
        }

        LocalDateTime getLastDesde() {
            return lastDesde;
        }

        LocalDateTime getLastHasta() {
            return lastHasta;
        }

        @Override
        public List<AccesoClubItemDTO> listarPorRango(LocalDateTime desde, LocalDateTime hasta) {
            lastDesde = desde;
            lastHasta = hasta;
            return result;
        }
    }

    private static class StubClubPresenciaQueryService extends ClubPresenciaQueryService {

        private ClubPresenciaListDTO result;
        private boolean lastPresente;

        void setResult(ClubPresenciaListDTO result) {
            this.result = result;
        }

        boolean isLastPresente() {
            return lastPresente;
        }

        @Override
        public ClubPresenciaListDTO listarPorPresente(boolean presente) {
            lastPresente = presente;
            return result;
        }
    }

    private static class StubAccesoClubPresenciaLiveService extends AccesoClubPresenciaLiveService {

        private SseEmitter result;
        private Long lastCompanyId;

        void setResult(SseEmitter result) {
            this.result = result;
        }

        Long getLastCompanyId() {
            return lastCompanyId;
        }

        @Override
        public SseEmitter suscribir(Long companyId) {
            lastCompanyId = companyId;
            return result;
        }
    }

    private static class StubAuthService extends AuthService {

        private Company company = new Company();

        void setCompany(Company company) {
            this.company = company;
        }

        @Override
        public User valid() {
            return new User();
        }

        @Override
        public User valid(List<Enum<?>> required) {
            return valid();
        }

        @Override
        public Company getCurrentCompanyOrThrow() {
            return company;
        }
    }
}
