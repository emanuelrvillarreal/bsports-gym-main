package com.blue.open.Code.Service.Amenities;

import com.blue.open.Code.Entity.DTO.ReservaAmenityCobroCommand;
import com.blue.open.Code.Entity.DTO.ReservaAmenityCobroResultDTO;
import com.blue.open.Code.Entity.Db.Amenity;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.CompanyMercadoPago;
import com.blue.open.Code.Entity.Db.Member;
import com.blue.open.Code.Entity.Db.Reserva;
import com.blue.open.Code.Entity.Db.Slot;
import com.blue.open.Code.Entity.Db.User;
import com.blue.open.Code.Entity.Enums.CanalCobroEnum;
import com.blue.open.Code.Entity.Enums.MedioPagoEnum;
import com.blue.open.Code.Entity.Enums.ReservaAmenityCobroTipoEnum;
import com.blue.open.Code.Entity.Enums.ReservaEstadoEnum;
import com.blue.open.Code.Entity.Enums.SlotEstadoEnum;
import com.blue.open.Code.Repository.MovimientoRepository;
import com.blue.open.Code.Repository.ReservaRepository;
import com.blue.open.Code.Repository.SlotRepository;
import com.blue.open.Code.Service.QrCodeService;
import com.blue.open.Code.Service.Tesoreria.Cobro.MercadoPagoClientService;
import com.blue.open.Code.Service.Tesoreria.Cobro.MercadoPagoCobroIntentService;
import com.blue.open.Code.Service.Tesoreria.Cobro.MercadoPagoPreferenceBuilder;
import com.blue.open.Code.Service.Tesoreria.Cobro.MercadoPagoRawJsonSerializer;
import com.blue.open.Code.Service.Tesoreria.Cobro.MercadoPagoTrazaService;
import com.blue.open.Code.Service.support.StubAuthService;
import com.blue.open.Core.Exceptions.CustomException;
import com.blue.open.Core.Security.JacksonConfig;
import com.mercadopago.client.preference.PreferenceRequest;
import com.mercadopago.resources.preference.Preference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservaAmenityCobroServiceTest {

    @Mock
    private ReservaRepository reservaRepository;

    @Mock
    private MovimientoRepository movimientoRepository;

    @Mock
    private SlotRepository slotRepository;

    @Mock
    private MercadoPagoCobroIntentService mercadoPagoCobroIntentService;

    private StubAuthService stubAuthService;
    private StubMercadoPagoClientService stubClientService;
    private StubMercadoPagoPreferenceBuilder stubPreferenceBuilder;
    private ReservaAmenityCobroService service;

    @BeforeEach
    void setUp() {
        stubAuthService = new StubAuthService();
        stubClientService = new StubMercadoPagoClientService();
        stubPreferenceBuilder = new StubMercadoPagoPreferenceBuilder();
        MercadoPagoTrazaService trazaService = new NoOpMercadoPagoTrazaService();
        MercadoPagoRawJsonSerializer rawJsonSerializer =
                new MercadoPagoRawJsonSerializer(new JacksonConfig().objectMapper());

        service = new ReservaAmenityCobroService(
                stubAuthService,
                reservaRepository,
                movimientoRepository,
                slotRepository,
                stubClientService,
                stubPreferenceBuilder,
                mercadoPagoCobroIntentService,
                trazaService,
                rawJsonSerializer,
                new QrCodeService());

        Company company = new Company();
        company.setId(1L);
        stubAuthService.setCompany(company);
        stubAuthService.setUser(new User());
    }

    @Test
    void registrarSena_manual_confirmaSlotYReserva() {
        Reserva reserva = buildReservaPagada(new BigDecimal("1000.00"), new BigDecimal("300.00"), new BigDecimal("1000.00"));

        when(reservaRepository.findByUuidAndCompanyIdWithDetails("reserva-uuid", 1L))
                .thenReturn(Optional.of(reserva));
        when(movimientoRepository.save(any())).thenAnswer(inv -> {
            var m = inv.getArgument(0, com.blue.open.Code.Entity.Db.Movimiento.class);
            m.setUuid("mov-uuid");
            return m;
        });
        when(reservaRepository.save(reserva)).thenReturn(reserva);
        when(slotRepository.save(reserva.getSlot())).thenReturn(reserva.getSlot());

        ReservaAmenityCobroResultDTO result = service.registrarSena("reserva-uuid", manualCommand(new BigDecimal("300.00")));

        assertEquals(ReservaEstadoEnum.CONFIRMADA, reserva.getEstado());
        assertEquals(new BigDecimal("700.00"), reserva.getSaldoPendiente());
        assertEquals(SlotEstadoEnum.CONFIRMADO, reserva.getSlot().getEstado());
        assertEquals(ReservaAmenityCobroTipoEnum.SENA, result.getTipoCobro());
        assertNotNull(result.getMovimientoUuid());
        verify(movimientoRepository).save(any());
    }

    @Test
    void registrarSena_mercadoPago_retornaQrSinConfirmarReserva() {
        Reserva reserva = buildReservaPagada(new BigDecimal("1000.00"), new BigDecimal("300.00"), new BigDecimal("1000.00"));

        when(reservaRepository.findByUuidAndCompanyIdWithDetails("reserva-uuid", 1L))
                .thenReturn(Optional.of(reserva));

        stubPreferenceBuilder.setNextRequest(PreferenceRequest.builder().build());
        stubClientService.setNextPreference(preference("https://mp.test/checkout"));

        ReservaAmenityCobroCommand command = new ReservaAmenityCobroCommand();
        command.setCanalCobro(CanalCobroEnum.MERCADO_PAGO);

        ReservaAmenityCobroResultDTO result = service.registrarSena("reserva-uuid", command);

        assertEquals(ReservaEstadoEnum.PENDIENTE_SENA, reserva.getEstado());
        assertEquals(SlotEstadoEnum.PENDIENTE_DE_PAGO, reserva.getSlot().getEstado());
        assertNotNull(result.getQr());
        assertNull(result.getMovimientoUuid());
        verify(mercadoPagoCobroIntentService).crearParaReservaAmenity(
                any(), any(), any(), eq(reserva), eq(ReservaAmenityCobroTipoEnum.SENA),
                eq(new BigDecimal("300.00")), any(), any());
    }

    @Test
    void registrarSena_reservaGratuita_rechazaCobro() {
        Reserva reserva = buildReservaPagada(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);

        when(reservaRepository.findByUuidAndCompanyIdWithDetails("reserva-uuid", 1L))
                .thenReturn(Optional.of(reserva));

        assertThrows(CustomException.class,
                () -> service.registrarSena("reserva-uuid", manualCommand(new BigDecimal("0.01"))));
    }

    @Test
    void registrarTotal_manual_confirmaReservaYSaldoEnCero() {
        Reserva reserva = buildReservaPagada(new BigDecimal("1000.00"), new BigDecimal("300.00"), new BigDecimal("1000.00"));

        when(reservaRepository.findByUuidAndCompanyIdWithDetails("reserva-uuid", 1L))
                .thenReturn(Optional.of(reserva));
        when(movimientoRepository.save(any())).thenAnswer(inv -> {
            var m = inv.getArgument(0, com.blue.open.Code.Entity.Db.Movimiento.class);
            m.setUuid("mov-total-uuid");
            return m;
        });
        when(reservaRepository.save(reserva)).thenReturn(reserva);
        when(slotRepository.save(reserva.getSlot())).thenReturn(reserva.getSlot());

        ReservaAmenityCobroResultDTO result = service.registrarTotal(
                "reserva-uuid", manualCommand(new BigDecimal("1000.00")));

        assertEquals(ReservaEstadoEnum.CONFIRMADA, reserva.getEstado());
        assertEquals(BigDecimal.ZERO, reserva.getSaldoPendiente());
        assertEquals(SlotEstadoEnum.CONFIRMADO, reserva.getSlot().getEstado());
        assertEquals(ReservaAmenityCobroTipoEnum.TOTAL, result.getTipoCobro());
        assertNotNull(result.getMovimientoUuid());
        verify(movimientoRepository).save(any());
    }

    @Test
    void registrarTotal_mercadoPago_retornaQrSinConfirmarReserva() {
        Reserva reserva = buildReservaPagada(new BigDecimal("1000.00"), new BigDecimal("300.00"), new BigDecimal("1000.00"));

        when(reservaRepository.findByUuidAndCompanyIdWithDetails("reserva-uuid", 1L))
                .thenReturn(Optional.of(reserva));

        stubPreferenceBuilder.setNextRequest(PreferenceRequest.builder().build());
        stubClientService.setNextPreference(preference("https://mp.test/checkout-total"));

        ReservaAmenityCobroCommand command = new ReservaAmenityCobroCommand();
        command.setCanalCobro(CanalCobroEnum.MERCADO_PAGO);

        ReservaAmenityCobroResultDTO result = service.registrarTotal("reserva-uuid", command);

        assertEquals(ReservaEstadoEnum.PENDIENTE_SENA, reserva.getEstado());
        assertEquals(new BigDecimal("1000.00"), reserva.getSaldoPendiente());
        assertNotNull(result.getQr());
        verify(mercadoPagoCobroIntentService).crearParaReservaAmenity(
                any(), any(), any(), eq(reserva), eq(ReservaAmenityCobroTipoEnum.TOTAL),
                eq(new BigDecimal("1000.00")), any(), any());
    }

    @Test
    void registrarTotal_reservaGratuita_rechazaCobro() {
        Reserva reserva = buildReservaPagada(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);

        when(reservaRepository.findByUuidAndCompanyIdWithDetails("reserva-uuid", 1L))
                .thenReturn(Optional.of(reserva));

        assertThrows(CustomException.class,
                () -> service.registrarTotal("reserva-uuid", manualCommand(new BigDecimal("0.01"))));
    }

    private Reserva buildReservaPagada(BigDecimal total, BigDecimal sena, BigDecimal saldo) {
        Amenity amenity = new Amenity();
        amenity.setNombre("Parrilla 1");

        Member titular = new Member();
        titular.setId(10L);

        Slot slot = new Slot();
        slot.setEstado(SlotEstadoEnum.PENDIENTE_DE_PAGO);

        Reserva reserva = new Reserva();
        reserva.setUuid("reserva-uuid");
        reserva.setEstado(ReservaEstadoEnum.PENDIENTE_SENA);
        reserva.setMontoTotal(total);
        reserva.setMontoSena(sena);
        reserva.setSaldoPendiente(saldo);
        reserva.setAmenity(amenity);
        reserva.setMember(titular);
        reserva.setSlot(slot);
        return reserva;
    }

    private ReservaAmenityCobroCommand manualCommand(BigDecimal monto) {
        ReservaAmenityCobroCommand command = new ReservaAmenityCobroCommand();
        command.setCanalCobro(CanalCobroEnum.MANUAL);
        command.setMonto(monto);
        command.setMedioPago(MedioPagoEnum.EFECTIVO);
        return command;
    }

    private Preference preference(String initPoint) {
        Preference preference = new Preference();
        ReflectionTestUtils.setField(preference, "initPoint", initPoint);
        return preference;
    }

    private static class StubMercadoPagoClientService extends MercadoPagoClientService {

        private Preference nextPreference;

        StubMercadoPagoClientService() {
            super(null, null, null);
        }

        void setNextPreference(Preference nextPreference) {
            this.nextPreference = nextPreference;
        }

        @Override
        public CompanyMercadoPago resolveOperativo(Company company) {
            return new CompanyMercadoPago();
        }

        @Override
        public Preference createPreference(Company company, PreferenceRequest request) {
            return nextPreference;
        }
    }

    private static class StubMercadoPagoPreferenceBuilder extends MercadoPagoPreferenceBuilder {

        private PreferenceRequest nextRequest;

        StubMercadoPagoPreferenceBuilder() {
            super(null);
        }

        void setNextRequest(PreferenceRequest nextRequest) {
            this.nextRequest = nextRequest;
        }

        @Override
        public PreferenceRequest buildParaReservaAmenity(
                Company company,
                String reservaUuid,
                String concepto,
                BigDecimal monto,
                String externalReference) {
            return nextRequest;
        }
    }

    private static class NoOpMercadoPagoTrazaService extends MercadoPagoTrazaService {

        NoOpMercadoPagoTrazaService() {
            super(null);
        }

        @Override
        public void registrar(com.blue.open.Code.Entity.DTO.MercadoPagoTrazaRegistro registro) {
        }
    }
}
