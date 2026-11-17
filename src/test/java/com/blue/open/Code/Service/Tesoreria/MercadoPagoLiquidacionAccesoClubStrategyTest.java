package com.blue.open.Code.Service.Tesoreria;

import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.EventoAcceso;
import com.blue.open.Code.Entity.Db.MercadoPagoCobro;
import com.blue.open.Code.Entity.Db.MercadoPagoCobroIntent;
import com.blue.open.Code.Entity.Db.Movimiento;
import com.blue.open.Code.Entity.Db.User;
import com.blue.open.Code.Entity.Enums.MedioPagoEnum;
import com.blue.open.Code.Entity.Enums.MercadoPagoCobroIntentEstadoEnum;
import com.blue.open.Code.Entity.Enums.MercadoPagoCobroTipoEnum;
import com.blue.open.Code.Entity.Enums.MovimientoEstadoEnum;
import com.blue.open.Code.Entity.Enums.MovimientoTipoEnum;
import com.blue.open.Code.Repository.MovimientoRepository;
import com.blue.open.Code.Service.Tesoreria.Cobro.MercadoPagoLiquidacionAccesoClubStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MercadoPagoLiquidacionAccesoClubStrategyTest {

    @Mock
    private MovimientoRepository movimientoRepository;

    private MercadoPagoLiquidacionAccesoClubStrategy strategy;

    @BeforeEach
    void setUp() {
        // No se mockea EventoAccesoHistorialService: este test no genera EventoAccesoMercadoPagoCobroDatos,
        // así que la strategy nunca lo invoca.
        strategy = new MercadoPagoLiquidacionAccesoClubStrategy(movimientoRepository, null);
    }

    @Test
    void liquidar_creaMovimientoConConceptoYEventoAcceso() {
        Company company = new Company();
        company.setId(1L);

        EventoAcceso evento = new EventoAcceso();
        evento.setId(20L);
        evento.setUuid("evento-uuid");
        evento.setTitulo("Noche de socios");
        evento.setMonto(new BigDecimal("4500.00"));

        User registradoPor = new User();
        registradoPor.setId(5L);

        MercadoPagoCobroIntent intent = new MercadoPagoCobroIntent();
        intent.setUuid("intent-uuid");
        intent.setCompany(company);
        intent.setPaymentId(777L);
        intent.setEstado(MercadoPagoCobroIntentEstadoEnum.PENDIENTE);

        MercadoPagoCobro cobro = new MercadoPagoCobro();
        cobro.setCompany(company);
        cobro.setTipo(MercadoPagoCobroTipoEnum.ACCESO_CLUB);
        cobro.setMontoTotal(new BigDecimal("4500.00"));
        cobro.setTitulo("Acceso invitado - Noche de socios");
        cobro.setEventoAcceso(evento);
        cobro.setRegistradoPor(registradoPor);
        cobro.setMercadoPagoCobroIntent(intent);
        intent.setMercadoPagoCobro(cobro);

        when(movimientoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        strategy.liquidar(intent);

        ArgumentCaptor<Movimiento> captor = ArgumentCaptor.forClass(Movimiento.class);
        verify(movimientoRepository).save(captor.capture());
        Movimiento movimiento = captor.getValue();

        assertEquals(MovimientoTipoEnum.INGRESO, movimiento.getTipo());
        assertEquals(MovimientoEstadoEnum.ACTIVO, movimiento.getEstado());
        assertEquals(new BigDecimal("4500.00"), movimiento.getMonto());
        assertEquals(MedioPagoEnum.MERCADO_PAGO, movimiento.getMedioPago());
        assertEquals("Acceso invitado - Noche de socios", movimiento.getConcepto());
        assertEquals(evento, movimiento.getEventoAcceso());
        assertEquals(registradoPor, movimiento.getRegistradoPor());
        assertEquals(company, movimiento.getCompany());
        assertNull(movimiento.getPersona());
        assertEquals(MercadoPagoCobroTipoEnum.ACCESO_CLUB, strategy.getTipo());
    }
}
