package com.blue.open.Code.Controller;

import com.blue.open.Code.Entity.DTO.ReservaAmenityCobroCommand;
import com.blue.open.Code.Entity.DTO.ReservaAmenityCobroResultDTO;
import com.blue.open.Code.Entity.DTO.ReservaAmenityDTO;
import com.blue.open.Code.Entity.Enums.CanalCobroEnum;
import com.blue.open.Code.Entity.Enums.MedioPagoEnum;
import com.blue.open.Code.Entity.Enums.ReservaAmenityCobroTipoEnum;
import com.blue.open.Code.Entity.Enums.ReservaEstadoEnum;
import com.blue.open.Code.Service.Amenities.ReservaAmenityCancelacionService;
import com.blue.open.Code.Service.Amenities.ReservaAmenityCobroService;
import com.blue.open.Code.Service.Amenities.ReservaAmenityConfirmacionService;
import com.blue.open.Core.Service.Auth.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(MockitoExtension.class)
class AmenityReservaCobroControllerTest {

    private StubAuthService stubAuthService;
    private StubReservaAmenityCobroService stubCobroService;
    private StubReservaAmenityConfirmacionService stubConfirmacionService;
    private StubReservaAmenityCancelacionService stubCancelacionService;
    private AmenityReservaCobroController controller;

    @BeforeEach
    void setUp() {
        stubAuthService = new StubAuthService();
        stubCobroService = new StubReservaAmenityCobroService();
        stubConfirmacionService = new StubReservaAmenityConfirmacionService();
        stubCancelacionService = new StubReservaAmenityCancelacionService();
        controller = new AmenityReservaCobroController();
        ReflectionTestUtils.setField(controller, "authService", stubAuthService);
        ReflectionTestUtils.setField(controller, "reservaAmenityCobroService", stubCobroService);
        ReflectionTestUtils.setField(controller, "reservaAmenityConfirmacionService", stubConfirmacionService);
        ReflectionTestUtils.setField(controller, "reservaAmenityCancelacionService", stubCancelacionService);
    }

    @Test
    void cobroSena_delegaAlServiceYRetorna200ConResultado() {
        ReservaAmenityCobroCommand command = new ReservaAmenityCobroCommand();
        command.setCanalCobro(CanalCobroEnum.MANUAL);
        command.setMonto(new BigDecimal("300.00"));
        command.setMedioPago(MedioPagoEnum.EFECTIVO);

        ReservaAmenityCobroResultDTO expected = new ReservaAmenityCobroResultDTO();
        expected.setMovimientoUuid("movimiento-uuid");
        expected.setMonto(new BigDecimal("300.00"));
        expected.setTipoCobro(ReservaAmenityCobroTipoEnum.SENA);
        stubCobroService.setSenaResult(expected);

        ResponseEntity<ReservaAmenityCobroResultDTO> response = controller.cobroSena("reserva-uuid", command);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("movimiento-uuid", response.getBody().getMovimientoUuid());
        assertEquals("reserva-uuid", stubCobroService.getLastSenaUuid());
    }

    @Test
    void cobroSaldo_delegaAlServiceYRetorna200ConResultado() {
        ReservaAmenityCobroCommand command = new ReservaAmenityCobroCommand();
        command.setCanalCobro(CanalCobroEnum.MANUAL);
        command.setMonto(new BigDecimal("700.00"));
        command.setMedioPago(MedioPagoEnum.TRANSFERENCIA);

        ReservaAmenityCobroResultDTO expected = new ReservaAmenityCobroResultDTO();
        expected.setMovimientoUuid("movimiento-saldo-uuid");
        expected.setMonto(new BigDecimal("700.00"));
        expected.setTipoCobro(ReservaAmenityCobroTipoEnum.SALDO);
        stubCobroService.setSaldoResult(expected);

        ResponseEntity<ReservaAmenityCobroResultDTO> response = controller.cobroSaldo("reserva-uuid", command);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("movimiento-saldo-uuid", response.getBody().getMovimientoUuid());
        assertEquals("reserva-uuid", stubCobroService.getLastSaldoUuid());
    }

    @Test
    void confirmar_delegaAlServiceYRetorna200() {
        ReservaAmenityDTO expected = new ReservaAmenityDTO();
        expected.setReservaUuid("reserva-uuid");
        expected.setEstado(ReservaEstadoEnum.CONFIRMADA);
        stubConfirmacionService.setResult(expected);

        ResponseEntity<ReservaAmenityDTO> response = controller.confirmar("reserva-uuid");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(ReservaEstadoEnum.CONFIRMADA, response.getBody().getEstado());
        assertEquals("reserva-uuid", stubConfirmacionService.getLastUuid());
    }

    @Test
    void cobroTotal_delegaAlServiceYRetorna200ConResultado() {
        ReservaAmenityCobroCommand command = new ReservaAmenityCobroCommand();
        command.setCanalCobro(CanalCobroEnum.MANUAL);
        command.setMonto(new BigDecimal("1000.00"));
        command.setMedioPago(MedioPagoEnum.EFECTIVO);

        ReservaAmenityCobroResultDTO expected = new ReservaAmenityCobroResultDTO();
        expected.setMovimientoUuid("movimiento-total-uuid");
        expected.setMonto(new BigDecimal("1000.00"));
        expected.setTipoCobro(ReservaAmenityCobroTipoEnum.TOTAL);
        stubCobroService.setTotalResult(expected);

        ResponseEntity<ReservaAmenityCobroResultDTO> response = controller.cobroTotal("reserva-uuid", command);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("movimiento-total-uuid", response.getBody().getMovimientoUuid());
        assertEquals(ReservaAmenityCobroTipoEnum.TOTAL, response.getBody().getTipoCobro());
        assertEquals("reserva-uuid", stubCobroService.getLastTotalUuid());
    }

    @Test
    void cancelar_delegaAlServiceYRetorna200ConReservaAmenityDTO() {
        ReservaAmenityDTO expected = new ReservaAmenityDTO();
        expected.setReservaUuid("reserva-uuid");
        expected.setEstado(ReservaEstadoEnum.CANCELADA);
        stubCancelacionService.setResult(expected);

        ResponseEntity<ReservaAmenityDTO> response = controller.cancelar("reserva-uuid");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("reserva-uuid", response.getBody().getReservaUuid());
        assertEquals(ReservaEstadoEnum.CANCELADA, response.getBody().getEstado());
        assertEquals("reserva-uuid", stubCancelacionService.getLastUuid());
    }

    private static class StubAuthService extends AuthService {

        private List<Enum<?>> lastRequired;

        List<Enum<?>> getLastRequired() {
            return lastRequired;
        }

        @Override
        public com.blue.open.Code.Entity.Db.User valid(List<Enum<?>> required) {
            lastRequired = required;
            return null;
        }
    }

    private static class StubReservaAmenityCobroService extends ReservaAmenityCobroService {

        private ReservaAmenityCobroResultDTO senaResult;
        private ReservaAmenityCobroResultDTO saldoResult;
        private ReservaAmenityCobroResultDTO totalResult;
        private String lastSenaUuid;
        private String lastSaldoUuid;
        private String lastTotalUuid;

        StubReservaAmenityCobroService() {
            super(null, null, null, null, null, null, null, null, null, null);
        }

        void setSenaResult(ReservaAmenityCobroResultDTO senaResult) {
            this.senaResult = senaResult;
        }

        void setSaldoResult(ReservaAmenityCobroResultDTO saldoResult) {
            this.saldoResult = saldoResult;
        }

        void setTotalResult(ReservaAmenityCobroResultDTO totalResult) {
            this.totalResult = totalResult;
        }

        String getLastSenaUuid() {
            return lastSenaUuid;
        }

        String getLastSaldoUuid() {
            return lastSaldoUuid;
        }

        String getLastTotalUuid() {
            return lastTotalUuid;
        }

        @Override
        public ReservaAmenityCobroResultDTO registrarSena(String reservaUuid, ReservaAmenityCobroCommand command) {
            lastSenaUuid = reservaUuid;
            return senaResult;
        }

        @Override
        public ReservaAmenityCobroResultDTO registrarSaldo(String reservaUuid, ReservaAmenityCobroCommand command) {
            lastSaldoUuid = reservaUuid;
            return saldoResult;
        }

        @Override
        public ReservaAmenityCobroResultDTO registrarTotal(String reservaUuid, ReservaAmenityCobroCommand command) {
            lastTotalUuid = reservaUuid;
            return totalResult;
        }
    }

    private static class StubReservaAmenityConfirmacionService extends ReservaAmenityConfirmacionService {

        private ReservaAmenityDTO result;
        private String lastUuid;

        StubReservaAmenityConfirmacionService() {
            super(null, null, null, null);
        }

        void setResult(ReservaAmenityDTO result) {
            this.result = result;
        }

        String getLastUuid() {
            return lastUuid;
        }

        @Override
        public ReservaAmenityDTO confirmar(String reservaUuid) {
            lastUuid = reservaUuid;
            return result;
        }
    }

    private static class StubReservaAmenityCancelacionService extends ReservaAmenityCancelacionService {

        private ReservaAmenityDTO result;
        private String lastUuid;

        void setResult(ReservaAmenityDTO result) {
            this.result = result;
        }

        String getLastUuid() {
            return lastUuid;
        }

        @Override
        public ReservaAmenityDTO cancelar(String reservaUuid) {
            lastUuid = reservaUuid;
            return result;
        }
    }
}
