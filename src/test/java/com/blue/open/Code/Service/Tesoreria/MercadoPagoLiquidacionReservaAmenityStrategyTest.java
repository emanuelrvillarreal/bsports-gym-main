package com.blue.open.Code.Service.Tesoreria;

import com.blue.open.Code.Entity.Db.Amenity;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.Member;
import com.blue.open.Code.Entity.Db.MercadoPagoCobro;
import com.blue.open.Code.Entity.Db.MercadoPagoCobroIntent;
import com.blue.open.Code.Entity.Db.Movimiento;
import com.blue.open.Code.Entity.Db.Reserva;
import com.blue.open.Code.Entity.Db.Slot;
import com.blue.open.Code.Entity.Db.User;
import com.blue.open.Code.Entity.Enums.MedioPagoEnum;
import com.blue.open.Code.Entity.Enums.MercadoPagoCobroIntentEstadoEnum;
import com.blue.open.Code.Entity.Enums.MercadoPagoCobroTipoEnum;
import com.blue.open.Code.Entity.Enums.MovimientoEstadoEnum;
import com.blue.open.Code.Entity.Enums.MovimientoTipoEnum;
import com.blue.open.Code.Entity.Enums.ReservaAmenityCobroTipoEnum;
import com.blue.open.Code.Entity.Enums.ReservaEstadoEnum;
import com.blue.open.Code.Entity.Enums.SlotEstadoEnum;
import com.blue.open.Code.Repository.MovimientoRepository;
import com.blue.open.Code.Repository.ReservaRepository;
import com.blue.open.Code.Repository.SlotRepository;
import com.blue.open.Code.Service.Tesoreria.Cobro.MercadoPagoLiquidacionReservaAmenityStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MercadoPagoLiquidacionReservaAmenityStrategyTest {

    @Mock
    private MovimientoRepository movimientoRepository;

    @Mock
    private ReservaRepository reservaRepository;

    @Mock
    private SlotRepository slotRepository;

    private MercadoPagoLiquidacionReservaAmenityStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new MercadoPagoLiquidacionReservaAmenityStrategy(
                movimientoRepository, reservaRepository, slotRepository);
    }

    @Test
    void liquidar_sena_creaMovimientoYConfirmaReserva() {
        Company company = new Company();
        company.setId(1L);

        Member titular = new Member();
        titular.setId(10L);

        Amenity amenity = new Amenity();
        amenity.setNombre("Parrilla 1");

        Slot slot = new Slot();
        slot.setEstado(SlotEstadoEnum.PENDIENTE_DE_PAGO);

        Reserva reserva = new Reserva();
        reserva.setUuid("reserva-uuid");
        reserva.setEstado(ReservaEstadoEnum.PENDIENTE_SENA);
        reserva.setMontoTotal(new BigDecimal("1000.00"));
        reserva.setMontoSena(new BigDecimal("300.00"));
        reserva.setSaldoPendiente(new BigDecimal("1000.00"));
        reserva.setMember(titular);
        reserva.setAmenity(amenity);
        reserva.setSlot(slot);

        User registradoPor = new User();
        registradoPor.setId(5L);

        MercadoPagoCobroIntent intent = new MercadoPagoCobroIntent();
        intent.setUuid("intent-uuid");
        intent.setCompany(company);
        intent.setPaymentId(888L);
        intent.setEstado(MercadoPagoCobroIntentEstadoEnum.PENDIENTE);

        MercadoPagoCobro cobro = new MercadoPagoCobro();
        cobro.setCompany(company);
        cobro.setTipo(MercadoPagoCobroTipoEnum.RESERVA_AMENITY);
        cobro.setMontoTotal(new BigDecimal("300.00"));
        cobro.setTitulo("Seña reserva amenity - Parrilla 1");
        cobro.setReserva(reserva);
        cobro.setReservaCobroTipo(ReservaAmenityCobroTipoEnum.SENA);
        cobro.setRegistradoPor(registradoPor);
        cobro.setMercadoPagoCobroIntent(intent);
        intent.setMercadoPagoCobro(cobro);

        when(reservaRepository.findByUuidAndCompanyIdWithDetails("reserva-uuid", 1L))
                .thenReturn(Optional.of(reserva));
        when(movimientoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(reservaRepository.save(reserva)).thenReturn(reserva);
        when(slotRepository.save(slot)).thenReturn(slot);

        strategy.liquidar(intent);

        ArgumentCaptor<Movimiento> captor = ArgumentCaptor.forClass(Movimiento.class);
        verify(movimientoRepository).save(captor.capture());
        Movimiento movimiento = captor.getValue();

        assertEquals(MovimientoTipoEnum.INGRESO, movimiento.getTipo());
        assertEquals(MovimientoEstadoEnum.ACTIVO, movimiento.getEstado());
        assertEquals(new BigDecimal("300.00"), movimiento.getMonto());
        assertEquals(MedioPagoEnum.MERCADO_PAGO, movimiento.getMedioPago());
        assertEquals(reserva, movimiento.getReserva());
        assertEquals(ReservaEstadoEnum.CONFIRMADA, reserva.getEstado());
        assertEquals(new BigDecimal("700.00"), reserva.getSaldoPendiente());
        assertEquals(SlotEstadoEnum.CONFIRMADO, slot.getEstado());
        assertEquals(MercadoPagoCobroTipoEnum.RESERVA_AMENITY, strategy.getTipo());
    }

    @Test
    void liquidar_saldo_creaMovimientoYDejaSaldoEnCero() {
        Company company = new Company();
        company.setId(1L);

        Member titular = new Member();
        titular.setId(10L);

        Reserva reserva = new Reserva();
        reserva.setUuid("reserva-uuid");
        reserva.setEstado(ReservaEstadoEnum.CONFIRMADA);
        reserva.setSaldoPendiente(new BigDecimal("700.00"));
        reserva.setMember(titular);

        User registradoPor = new User();
        registradoPor.setId(5L);

        MercadoPagoCobroIntent intent = new MercadoPagoCobroIntent();
        intent.setUuid("intent-uuid");
        intent.setCompany(company);
        intent.setPaymentId(999L);

        MercadoPagoCobro cobro = new MercadoPagoCobro();
        cobro.setCompany(company);
        cobro.setTipo(MercadoPagoCobroTipoEnum.RESERVA_AMENITY);
        cobro.setMontoTotal(new BigDecimal("700.00"));
        cobro.setTitulo("Saldo reserva amenity - Parrilla 1");
        cobro.setReserva(reserva);
        cobro.setReservaCobroTipo(ReservaAmenityCobroTipoEnum.SALDO);
        cobro.setRegistradoPor(registradoPor);
        cobro.setMercadoPagoCobroIntent(intent);
        intent.setMercadoPagoCobro(cobro);

        when(reservaRepository.findByUuidAndCompanyIdWithDetails("reserva-uuid", 1L))
                .thenReturn(Optional.of(reserva));
        when(movimientoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(reservaRepository.save(reserva)).thenReturn(reserva);

        strategy.liquidar(intent);

        assertEquals(BigDecimal.ZERO, reserva.getSaldoPendiente());
        verify(movimientoRepository).save(any());
    }

    @Test
    void liquidar_total_creaMovimientoConfirmaReservaYSaldoEnCero() {
        Company company = new Company();
        company.setId(1L);

        Member titular = new Member();
        titular.setId(10L);

        Amenity amenity = new Amenity();
        amenity.setNombre("Parrilla 1");

        Slot slot = new Slot();
        slot.setEstado(SlotEstadoEnum.PENDIENTE_DE_PAGO);

        Reserva reserva = new Reserva();
        reserva.setUuid("reserva-uuid");
        reserva.setEstado(ReservaEstadoEnum.PENDIENTE_SENA);
        reserva.setMontoTotal(new BigDecimal("1000.00"));
        reserva.setSaldoPendiente(new BigDecimal("1000.00"));
        reserva.setMember(titular);
        reserva.setAmenity(amenity);
        reserva.setSlot(slot);

        User registradoPor = new User();
        registradoPor.setId(5L);

        MercadoPagoCobroIntent intent = new MercadoPagoCobroIntent();
        intent.setUuid("intent-total-uuid");
        intent.setCompany(company);
        intent.setPaymentId(1001L);

        MercadoPagoCobro cobro = new MercadoPagoCobro();
        cobro.setCompany(company);
        cobro.setTipo(MercadoPagoCobroTipoEnum.RESERVA_AMENITY);
        cobro.setMontoTotal(new BigDecimal("1000.00"));
        cobro.setTitulo("Pago total reserva amenity - Parrilla 1");
        cobro.setReserva(reserva);
        cobro.setReservaCobroTipo(ReservaAmenityCobroTipoEnum.TOTAL);
        cobro.setRegistradoPor(registradoPor);
        cobro.setMercadoPagoCobroIntent(intent);
        intent.setMercadoPagoCobro(cobro);

        when(reservaRepository.findByUuidAndCompanyIdWithDetails("reserva-uuid", 1L))
                .thenReturn(Optional.of(reserva));
        when(movimientoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(reservaRepository.save(reserva)).thenReturn(reserva);
        when(slotRepository.save(slot)).thenReturn(slot);

        strategy.liquidar(intent);

        assertEquals(ReservaEstadoEnum.CONFIRMADA, reserva.getEstado());
        assertEquals(BigDecimal.ZERO, reserva.getSaldoPendiente());
        assertEquals(SlotEstadoEnum.CONFIRMADO, slot.getEstado());
        verify(movimientoRepository).save(any());
    }
}
