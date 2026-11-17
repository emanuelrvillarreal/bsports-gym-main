package com.blue.open.Code.Service.Amenities;

import com.blue.open.Code.Entity.Db.Amenity;
import com.blue.open.Code.Entity.DTO.ReservaAmenityMontoResult;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReservaAmenityMontoCalculatorTest {

    // RF-07 / CA-07 / TC-10: Dado reserva 45 min, cuando calcular, entonces factura mínimo 1 hora
    @Test
    void calcular_45Minutos_facturaMinimoUnaHora() {
        Amenity amenity = buildAmenity(new BigDecimal("1000.00"), new BigDecimal("30.00"));
        LocalDateTime inicio = LocalDateTime.of(2026, 6, 7, 14, 0);
        LocalDateTime fin = inicio.plusMinutes(45);

        ReservaAmenityMontoResult result = ReservaAmenityMontoCalculator.calcular(amenity, inicio, fin);

        assertEquals(new BigDecimal("1000.00"), result.getMontoTotal());
        assertEquals(new BigDecimal("300.00"), result.getMontoSena());
        assertEquals(new BigDecimal("1000.00"), result.getSaldoPendiente());
    }

    // RF-07 / CA-07 / TC-11: Dado reserva 2 horas, cuando calcular, entonces factura proporcional
    @Test
    void calcular_dosHoras_facturaProporcional() {
        Amenity amenity = buildAmenity(new BigDecimal("2000.00"), new BigDecimal("50.00"));
        LocalDateTime inicio = LocalDateTime.of(2026, 6, 7, 10, 0);
        LocalDateTime fin = inicio.plusHours(2);

        ReservaAmenityMontoResult result = ReservaAmenityMontoCalculator.calcular(amenity, inicio, fin);

        assertEquals(new BigDecimal("4000.00"), result.getMontoTotal());
        assertEquals(new BigDecimal("2000.00"), result.getMontoSena());
        assertEquals(new BigDecimal("4000.00"), result.getSaldoPendiente());
    }

    @Test
    void calcular_amenityGratuito_devuelveMontosEnCero() {
        Amenity amenity = buildAmenity(BigDecimal.ZERO, BigDecimal.ZERO);
        LocalDateTime inicio = LocalDateTime.of(2026, 6, 7, 14, 0);
        LocalDateTime fin = inicio.plusHours(2);

        ReservaAmenityMontoResult result = ReservaAmenityMontoCalculator.calcular(amenity, inicio, fin);

        assertEquals(BigDecimal.ZERO.setScale(2), result.getMontoTotal());
        assertEquals(BigDecimal.ZERO.setScale(2), result.getMontoSena());
        assertEquals(BigDecimal.ZERO.setScale(2), result.getSaldoPendiente());
    }

    private Amenity buildAmenity(BigDecimal montoHora, BigDecimal porcentajeSena) {
        Amenity amenity = new Amenity();
        amenity.setMontoHora(montoHora);
        amenity.setPorcentajeSena(porcentajeSena);
        return amenity;
    }
}
