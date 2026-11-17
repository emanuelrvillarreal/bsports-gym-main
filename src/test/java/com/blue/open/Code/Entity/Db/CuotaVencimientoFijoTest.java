package com.blue.open.Code.Entity.Db;

import com.blue.open.Code.Entity.Enums.PeriodicidadCuotaEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CuotaVencimientoFijoTest {

    private Company company;
    private Member socio;
    private Actividad actividad;

    @BeforeEach
    void setUp() {
        company = new Company();
        company.setId(1L);

        socio = new Member();
        socio.setId(100L);
        socio.setCompany(company);

        actividad = new Actividad();
        actividad.setId(5L);
        actividad.setNombre("Yoga");
        actividad.setCompany(company);
        actividad.setImporteCuota(new BigDecimal("5000.00"));
        actividad.setPeriodicidadCuota(PeriodicidadCuotaEnum.MENSUAL);
        actividad.setDiasVencimientoCuota(30);
    }

    @Test
    void proximoDiaDelMes_diaYaPasado_vaAlMesSiguiente() {
        assertEquals(LocalDate.of(2026, 4, 10), Cuota.proximoDiaDelMes(LocalDate.of(2026, 3, 17), 10));
    }

    @Test
    void proximoDiaDelMes_diaPorVenir_quedaEnElMismoMes() {
        assertEquals(LocalDate.of(2026, 3, 10), Cuota.proximoDiaDelMes(LocalDate.of(2026, 3, 5), 10));
    }

    @Test
    void proximoDiaDelMes_mismoDia_quedaEseDia() {
        assertEquals(LocalDate.of(2026, 3, 10), Cuota.proximoDiaDelMes(LocalDate.of(2026, 3, 10), 10));
    }

    @Test
    void deActividad_conDiaFijo_generada17DeMarzo_vence10DeAbril() {
        actividad.setDiaVencimientoFijo(10);

        Cuota cuota = Cuota.deActividad(company, socio, actividad, LocalDate.of(2026, 3, 17), List.of());

        assertEquals(LocalDate.of(2026, 3, 17), cuota.getFechaGeneracion());
        assertEquals(LocalDate.of(2026, 4, 10), cuota.getVencimiento());
    }

    @Test
    void deActividad_conDiaFijo_generada5DeMarzo_vence10DeMarzo() {
        actividad.setDiaVencimientoFijo(10);

        Cuota cuota = Cuota.deActividad(company, socio, actividad, LocalDate.of(2026, 3, 5), List.of());

        assertEquals(LocalDate.of(2026, 3, 10), cuota.getVencimiento());
    }

    @Test
    void deActividad_sinDiaFijo_venceSegunDiasDeVencimiento() {
        Cuota cuota = Cuota.deActividad(company, socio, actividad, LocalDate.of(2026, 3, 17), List.of());

        assertEquals(LocalDate.of(2026, 4, 16), cuota.getVencimiento());
    }
}
