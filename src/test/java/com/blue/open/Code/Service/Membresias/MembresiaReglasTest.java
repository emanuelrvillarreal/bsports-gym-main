package com.blue.open.Code.Service.Membresias;

import com.blue.open.Code.Entity.Db.Actividad;
import com.blue.open.Code.Entity.Db.Member;
import com.blue.open.Code.Entity.Db.Membresia;
import com.blue.open.Code.Entity.Db.MembresiaCongelamiento;
import com.blue.open.Code.Entity.Db.Pase;
import com.blue.open.Code.Entity.Enums.CuotaEstadoEnum;
import com.blue.open.Code.Entity.Enums.DiaSemanaEnum;
import com.blue.open.Code.Entity.Enums.MembresiaEstadoEnum;
import com.blue.open.Code.Entity.Enums.MembresiaSituacionEnum;
import com.blue.open.Code.Entity.Enums.PaseModalidadEnum;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MembresiaReglasTest {

    /** Jueves 5/11/2026 a las 10:00. */
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 5, 10, 0);
    private static final LocalDate HOY = AHORA.toLocalDate();

    private static Membresia membresia(LocalDate inicio, LocalDate fin) {
        Pase pase = new Pase();
        pase.setNombre("Libre");
        pase.setModalidad(PaseModalidadEnum.MENSUAL);
        Member socio = new Member();
        socio.setId(1L);
        Membresia m = new Membresia();
        m.setPase(pase);
        m.setMember(socio);
        m.setEstado(MembresiaEstadoEnum.ACTIVA);
        m.setFechaInicio(inicio);
        m.setFechaFin(fin);
        m.setFechaFinOriginal(fin);
        return m;
    }

    private static Membresia vigente() {
        return membresia(HOY.minusDays(5), HOY.plusDays(20));
    }

    @Test
    void duracionesDeLasModalidades() {
        LocalDate inicio = LocalDate.of(2026, 1, 31);
        assertEquals(LocalDate.of(2026, 2, 27), PaseModalidadEnum.MENSUAL.fin(inicio, null));
        // 31/01 + 3 meses = 30/04 (no existe el 31): el último día de vigencia es el anterior.
        assertEquals(LocalDate.of(2026, 4, 29), PaseModalidadEnum.TRIMESTRAL.fin(inicio, null));
        assertEquals(LocalDate.of(2027, 1, 30), PaseModalidadEnum.ANUAL.fin(inicio, null));
        assertEquals(inicio, PaseModalidadEnum.DIARIO.fin(inicio, null));
        assertEquals(LocalDate.of(2026, 2, 6), PaseModalidadEnum.SEMANAL.fin(inicio, null));
        assertEquals(LocalDate.of(2026, 3, 1), PaseModalidadEnum.POR_INGRESOS.fin(inicio, 30));
        assertThrows(IllegalArgumentException.class, () -> PaseModalidadEnum.PERSONALIZADO.fin(inicio, null));
    }

    @Test
    void soloPagadaYDentroDeFechasEsVigente() {
        Membresia m = vigente();
        assertEquals(MembresiaSituacionEnum.VIGENTE, MembresiaReglas.situacion(m, CuotaEstadoEnum.PAGADA, HOY));
        assertEquals(MembresiaSituacionEnum.PENDIENTE_PAGO, MembresiaReglas.situacion(m, CuotaEstadoEnum.PENDIENTE, HOY));
        assertEquals(MembresiaSituacionEnum.PENDIENTE_PAGO, MembresiaReglas.situacion(m, CuotaEstadoEnum.VENCIDA, HOY));
        assertEquals(MembresiaSituacionEnum.PROGRAMADA,
                MembresiaReglas.situacion(membresia(HOY.plusDays(1), HOY.plusDays(30)), CuotaEstadoEnum.PAGADA, HOY));
        assertEquals(MembresiaSituacionEnum.VENCIDA,
                MembresiaReglas.situacion(membresia(HOY.minusDays(30), HOY.minusDays(1)), CuotaEstadoEnum.PENDIENTE, HOY));
        m.setEstado(MembresiaEstadoEnum.BAJA);
        assertEquals(MembresiaSituacionEnum.BAJA, MembresiaReglas.situacion(m, CuotaEstadoEnum.PAGADA, HOY));
    }

    @Test
    void ultimoDiaTodaviaHabilita() {
        Membresia m = membresia(HOY.minusDays(29), HOY);
        assertNull(MembresiaReglas.motivoRechazo(m, CuotaEstadoEnum.PAGADA, AHORA.withHour(23), null, null));
    }

    @Test
    void sinIngresosDisponiblesQuedaAgotada() {
        Membresia m = vigente();
        m.setIngresosPermitidos(8);
        m.setIngresosUsados(8);
        assertEquals(MembresiaSituacionEnum.AGOTADA, MembresiaReglas.situacion(m, CuotaEstadoEnum.PAGADA, HOY));
        assertEquals(0, m.ingresosDisponibles());
        m.setIngresosUsados(7);
        assertNull(MembresiaReglas.motivoRechazo(m, CuotaEstadoEnum.PAGADA, AHORA, null, null));
    }

    @Test
    void congeladaNoHabilitaEInformaHastaCuando() {
        Membresia m = vigente();
        MembresiaCongelamiento c = new MembresiaCongelamiento();
        c.setFechaDesde(HOY.minusDays(1));
        c.setFechaHasta(HOY.plusDays(3));
        m.getCongelamientos().add(c);
        assertEquals(MembresiaSituacionEnum.CONGELADA, MembresiaReglas.situacion(m, CuotaEstadoEnum.PAGADA, HOY));
        assertTrue(MembresiaReglas.motivoRechazo(m, CuotaEstadoEnum.PAGADA, AHORA, null, null).contains("08/11/2026"));
        c.setStatus(1); // anulado
        assertEquals(MembresiaSituacionEnum.VIGENTE, MembresiaReglas.situacion(m, CuotaEstadoEnum.PAGADA, HOY));
    }

    @Test
    void diasYHorarioPermitidos() {
        Membresia m = vigente();
        m.getPase().definirDias(EnumSet.of(DiaSemanaEnum.LUNES, DiaSemanaEnum.VIERNES));
        assertTrue(MembresiaReglas.motivoRechazo(m, CuotaEstadoEnum.PAGADA, AHORA, null, null).contains("jueves"));
        m.getPase().definirDias(EnumSet.of(DiaSemanaEnum.JUEVES));
        m.getPase().setHoraDesde(LocalTime.of(7, 0));
        m.getPase().setHoraHasta(LocalTime.of(9, 0));
        assertTrue(MembresiaReglas.motivoRechazo(m, CuotaEstadoEnum.PAGADA, AHORA, null, null).contains("07:00 a 09:00"));
        assertNull(MembresiaReglas.motivoRechazo(m, CuotaEstadoEnum.PAGADA, AHORA.withHour(8), null, null));
    }

    @Test
    void horarioQueCruzaLaMedianoche() {
        assertTrue(MembresiaReglas.dentroDeHorario(LocalTime.of(22, 0), LocalTime.of(2, 0), LocalTime.of(23, 30)));
        assertTrue(MembresiaReglas.dentroDeHorario(LocalTime.of(22, 0), LocalTime.of(2, 0), LocalTime.of(1, 0)));
        assertFalse(MembresiaReglas.dentroDeHorario(LocalTime.of(22, 0), LocalTime.of(2, 0), LocalTime.of(12, 0)));
        assertTrue(MembresiaReglas.dentroDeHorario(null, null, LocalTime.NOON));
    }

    @Test
    void actividadNoIncluida() {
        Membresia m = vigente();
        Actividad yoga = new Actividad();
        yoga.setId(10L);
        yoga.setNombre("Yoga");
        Actividad futbol = new Actividad();
        futbol.setId(20L);
        futbol.setNombre("Fútbol");
        // Sin actividades cargadas: acceso general.
        assertNull(MembresiaReglas.motivoRechazo(m, CuotaEstadoEnum.PAGADA, AHORA, futbol, null));
        m.getPase().getActividades().add(yoga);
        assertNull(MembresiaReglas.motivoRechazo(m, CuotaEstadoEnum.PAGADA, AHORA, yoga, null));
        assertTrue(MembresiaReglas.motivoRechazo(m, CuotaEstadoEnum.PAGADA, AHORA, futbol, null).contains("Fútbol"));
        // Ingreso general al club (sin actividad): no se controla la actividad.
        assertNull(MembresiaReglas.motivoRechazo(m, CuotaEstadoEnum.PAGADA, AHORA, null, null));
    }

    @Test
    void congelamientosNoSePisan() {
        Membresia m = vigente();
        MembresiaCongelamiento c = new MembresiaCongelamiento();
        c.setFechaDesde(HOY.plusDays(2));
        c.setFechaHasta(HOY.plusDays(6));
        m.getCongelamientos().add(c);
        assertTrue(MembresiaReglas.seSuperpone(m, HOY.plusDays(6), HOY.plusDays(9)));
        assertFalse(MembresiaReglas.seSuperpone(m, HOY.plusDays(7), HOY.plusDays(9)));
        assertEquals(5, MembresiaReglas.diasEntre(HOY.plusDays(2), HOY.plusDays(6)));
    }

    @Test
    void cubreAIntegrantesDelPaseFamiliar() {
        Membresia m = vigente();
        Member hijo = new Member();
        hijo.setId(2L);
        m.getIntegrantes().add(hijo);
        assertTrue(m.cubreA(1L));
        assertTrue(m.cubreA(2L));
        assertFalse(m.cubreA(3L));
    }
}
