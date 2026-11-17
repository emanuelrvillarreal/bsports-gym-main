package com.blue.open.Code.Service.Tesoreria;

import com.blue.open.Code.Entity.Enums.PeriodicidadCuotaEnum;
import com.blue.open.Code.Entity.DTO.CuotaActividadAltaResultDTO;
import com.blue.open.Code.Entity.DTO.CuotaActividadGeneracionResultDTO;
import com.blue.open.Code.Entity.Db.Actividad;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.Cuota;
import com.blue.open.Code.Entity.Db.InscripcionActividad;
import com.blue.open.Code.Entity.Db.Member;
import com.blue.open.Code.Entity.Enums.CuotaEstadoEnum;
import com.blue.open.Code.Entity.Enums.CuotaTipoEnum;
import com.blue.open.Code.Repository.ActividadRepository;
import com.blue.open.Code.Repository.CompanyRepository;
import com.blue.open.Code.Repository.CuotaRepository;
import com.blue.open.Code.Repository.InscripcionActividadDescuentoRepository;
import com.blue.open.Code.Repository.InscripcionActividadRepository;
import com.blue.open.Code.Repository.MemberRepository;
import com.blue.open.Core.Exceptions.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CuotaActividadGeneracionServiceTest {

    private static final Long COMPANY_ID = 1L;
    private static final Long MEMBER_ID = 10L;
    private static final Long ACTIVIDAD_ID = 20L;
    private static final Long INSCRIPCION_ID = 30L;
    private static final LocalDate PROXIMA_GENERACION = LocalDate.of(2026, 10, 2);
    private static final PeriodicidadCuotaEnum PERIODICIDAD_CUOTA = PeriodicidadCuotaEnum.SEMANAL;
    private static final int DIAS_VENCIMIENTO_CUOTA = 10;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private ActividadRepository actividadRepository;

    @Mock
    private InscripcionActividadRepository inscripcionActividadRepository;

    @Mock
    private CuotaRepository cuotaRepository;

    @Mock
    private InscripcionActividadDescuentoRepository inscripcionActividadDescuentoRepository;

    @Mock
    private TransactionTemplate transactionTemplate;

    private CuotaActividadGeneracionService cuotaActividadGeneracionService;

    private Company company;

    private InscripcionActividad inscripcion;

    @BeforeEach
    void setUp() {
        cuotaActividadGeneracionService = new CuotaActividadGeneracionService();
        ReflectionTestUtils.setField(cuotaActividadGeneracionService, "companyRepository", companyRepository);
        ReflectionTestUtils.setField(cuotaActividadGeneracionService, "memberRepository", memberRepository);
        ReflectionTestUtils.setField(cuotaActividadGeneracionService, "actividadRepository", actividadRepository);
        ReflectionTestUtils.setField(cuotaActividadGeneracionService, "inscripcionActividadRepository",
                inscripcionActividadRepository);
        ReflectionTestUtils.setField(cuotaActividadGeneracionService, "cuotaRepository", cuotaRepository);
        ReflectionTestUtils.setField(cuotaActividadGeneracionService, "inscripcionActividadDescuentoRepository",
                inscripcionActividadDescuentoRepository);
        ReflectionTestUtils.setField(cuotaActividadGeneracionService, "transactionTemplate", transactionTemplate);
        // Sin pases que cubran actividades: se cobra todo como siempre.
        ReflectionTestUtils.setField(cuotaActividadGeneracionService, "coberturaPases",
                new com.blue.open.Code.Service.Membresias.MembresiaCoberturaActividades() {
                    @Override
                    public boolean cubre(Long memberId, Long actividadId, java.time.LocalDate fecha) {
                        return false;
                    }
                });

        company = new Company();
        company.setId(COMPANY_ID);

        inscripcion = inscripcionConProximaGeneracion(company, PROXIMA_GENERACION);
    }

    // ---------------------------------------------------------------------------------------
    // generarHastaParaCompany: corrida de generación (job / endpoint manual)
    // ---------------------------------------------------------------------------------------

    @Test
    void generarHastaParaCompany_hoyIgualAProximaGeneracion_generaUnaCuotaYAvanzaSieteDias() {
        prepararCorridaDeGeneracion();
        LocalDate hoy = LocalDate.of(2026, 10, 2);

        CuotaActividadGeneracionResultDTO result =
                cuotaActividadGeneracionService.generarHastaParaCompany(COMPANY_ID, hoy);

        assertEquals(1, result.getCuotasGeneradas());
        assertEquals(0, result.getCuotasOmitidas());
        List<Cuota> cuotas = capturarCuotasGuardadas();
        assertEquals(1, cuotas.size());
        assertEquals(LocalDate.of(2026, 10, 2), cuotas.get(0).getFechaGeneracion());
        assertEquals(LocalDate.of(2026, 10, 9), inscripcion.getProximaGeneracion());
    }

    @Test
    void generarHastaParaCompany_hoyUnDiaDespuesDeProximaGeneracion_generaUnaCuotaYAvanzaSieteDias() {
        prepararCorridaDeGeneracion();
        LocalDate hoy = LocalDate.of(2026, 10, 3);

        CuotaActividadGeneracionResultDTO result =
                cuotaActividadGeneracionService.generarHastaParaCompany(COMPANY_ID, hoy);

        assertEquals(1, result.getCuotasGeneradas());
        assertEquals(0, result.getCuotasOmitidas());
        List<Cuota> cuotas = capturarCuotasGuardadas();
        assertEquals(1, cuotas.size());
        // La cuota toma la fecha programada, no la de hoy: así no se corre el ciclo.
        assertEquals(LocalDate.of(2026, 10, 2), cuotas.get(0).getFechaGeneracion());
        assertEquals(LocalDate.of(2026, 10, 9), inscripcion.getProximaGeneracion());
    }

    @Test
    void generarHastaParaCompany_hoyAntesDeProximaGeneracion_noGeneraCuotas() {
        prepararCorridaDeGeneracion();
        LocalDate hoy = LocalDate.of(2026, 10, 1);

        CuotaActividadGeneracionResultDTO result =
                cuotaActividadGeneracionService.generarHastaParaCompany(COMPANY_ID, hoy);

        assertEquals(0, result.getCuotasGeneradas());
        assertEquals(0, result.getCuotasOmitidas());
        verify(cuotaRepository, never()).saveAll(anyList());
        assertEquals(PROXIMA_GENERACION, inscripcion.getProximaGeneracion());
    }

    @Test
    void generarHastaParaCompany_hoyVeintiunDiasDespuesDeProximaGeneracion_generaCuatroCuotas() {
        prepararCorridaDeGeneracion();
        // 21 días con ciclo de 7: el 23/10 también es día de generación, así que son 4 ciclos.
        LocalDate hoy = PROXIMA_GENERACION.plusDays(21);

        CuotaActividadGeneracionResultDTO result =
                cuotaActividadGeneracionService.generarHastaParaCompany(COMPANY_ID, hoy);

        assertEquals(4, result.getCuotasGeneradas());
        assertEquals(0, result.getCuotasOmitidas());
        List<Cuota> cuotas = capturarCuotasGuardadas();
        assertEquals(List.of(
                        LocalDate.of(2026, 10, 2),
                        LocalDate.of(2026, 10, 9),
                        LocalDate.of(2026, 10, 16),
                        LocalDate.of(2026, 10, 23)),
                cuotas.stream().map(Cuota::getFechaGeneracion).toList());
        assertEquals(LocalDate.of(2026, 10, 30), inscripcion.getProximaGeneracion());
    }

    // ---------------------------------------------------------------------------------------
    // generarPorInscripcion: primera cuota, en el alta de la inscripción
    // ---------------------------------------------------------------------------------------

    @Test
    void generarPorInscripcion_generaCuotaConFechaDeInscripcionYVencimientoPorDiasDeLaActividad() {
        prepararAltaDeInscripcion();
        LocalDate fechaInscripcion = LocalDate.of(2026, 10, 2);

        CuotaActividadAltaResultDTO result = cuotaActividadGeneracionService.generarPorInscripcion(
                MEMBER_ID, ACTIVIDAD_ID, INSCRIPCION_ID, fechaInscripcion);

        Cuota cuota = capturarCuotaGuardada();
        assertEquals(CuotaTipoEnum.ACTIVIDAD, cuota.getTipo());
        assertEquals(LocalDate.of(2026, 10, 2), cuota.getFechaGeneracion());
        assertEquals(LocalDate.of(2026, 10, 12), cuota.getVencimiento());
        assertEquals(CuotaEstadoEnum.PENDIENTE, cuota.getEstado());
        assertTrue(result.isGenerada());
        assertEquals(LocalDate.of(2026, 10, 12), result.getVencimiento());
    }

    @Test
    void generarPorInscripcion_sinFechaDeInscripcion_usaHoy() {
        prepararAltaDeInscripcion();

        cuotaActividadGeneracionService.generarPorInscripcion(MEMBER_ID, ACTIVIDAD_ID, INSCRIPCION_ID, null);

        Cuota cuota = capturarCuotaGuardada();
        assertEquals(LocalDate.now(), cuota.getFechaGeneracion());
        assertEquals(LocalDate.now().plusDays(DIAS_VENCIMIENTO_CUOTA), cuota.getVencimiento());
    }

    @Test
    void generarPorInscripcion_sinIds_lanzaBadRequest() {
        CustomException ex = assertThrows(CustomException.class, () ->
                cuotaActividadGeneracionService.generarPorInscripcion(
                        MEMBER_ID, ACTIVIDAD_ID, null, PROXIMA_GENERACION));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        verify(cuotaRepository, never()).save(any(Cuota.class));
    }

    @Test
    void generarPorInscripcion_actividadSinImporte_propagaErrorSinGuardar() {
        // La excepción debe llegar al alta de la inscripción para que se caiga junto con la cuota.
        inscripcion.getActividad().setImporteCuota(null);
        when(memberRepository.findByIdWithTitular(MEMBER_ID)).thenReturn(Optional.of(inscripcion.getMember()));
        when(actividadRepository.findById(ACTIVIDAD_ID)).thenReturn(Optional.of(inscripcion.getActividad()));

        CustomException ex = assertThrows(CustomException.class, () ->
                cuotaActividadGeneracionService.generarPorInscripcion(
                        MEMBER_ID, ACTIVIDAD_ID, INSCRIPCION_ID, PROXIMA_GENERACION));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        verify(cuotaRepository, never()).save(any(Cuota.class));
    }

    /** Stubs de la corrida por fecha: transacción, club y la inscripción pendiente. */
    private void prepararCorridaDeGeneracion() {
        // El template ejecuta el callback en el mismo hilo, sin transacción real.
        when(transactionTemplate.execute(any())).thenAnswer(invocation ->
                invocation.<TransactionCallback<?>>getArgument(0).doInTransaction(null));
        when(companyRepository.findById(COMPANY_ID)).thenReturn(Optional.of(company));

        // Se devuelve la inscripción sin filtrar por fecha: así lo que se prueba es la regla del
        // servicio (generar mientras proximaGeneracion <= hasta), no el WHERE de la query.
        when(inscripcionActividadRepository.findConGeneracionPendiente(eq(COMPANY_ID), anyCollection(), any()))
                .thenReturn(List.of(inscripcion));
    }

    /** Stubs del alta de inscripción: socio, actividad y el save de la cuota. */
    private void prepararAltaDeInscripcion() {
        when(memberRepository.findByIdWithTitular(MEMBER_ID)).thenReturn(Optional.of(inscripcion.getMember()));
        when(actividadRepository.findById(ACTIVIDAD_ID)).thenReturn(Optional.of(inscripcion.getActividad()));
        when(cuotaRepository.save(any(Cuota.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @SuppressWarnings("unchecked")
    private List<Cuota> capturarCuotasGuardadas() {
        ArgumentCaptor<List<Cuota>> captor = ArgumentCaptor.forClass(List.class);
        verify(cuotaRepository).saveAll(captor.capture());
        return captor.getValue();
    }

    private Cuota capturarCuotaGuardada() {
        ArgumentCaptor<Cuota> captor = ArgumentCaptor.forClass(Cuota.class);
        verify(cuotaRepository).save(captor.capture());
        return captor.getValue();
    }

    private static InscripcionActividad inscripcionConProximaGeneracion(Company company, LocalDate proximaGeneracion) {
        Member persona = new Member();
        persona.setId(MEMBER_ID);
        persona.setCompany(company);

        Actividad actividad = new Actividad();
        actividad.setId(ACTIVIDAD_ID);
        actividad.setCompany(company);
        actividad.setNombre("Natación");
        actividad.setImporteCuota(new BigDecimal("10000.00"));
        actividad.setPeriodicidadCuota(PERIODICIDAD_CUOTA);
        actividad.setDiasVencimientoCuota(DIAS_VENCIMIENTO_CUOTA);

        InscripcionActividad inscripcion = new InscripcionActividad();
        inscripcion.setId(INSCRIPCION_ID);
        inscripcion.setCompany(company);
        inscripcion.setMember(persona);
        inscripcion.setActividad(actividad);
        inscripcion.setFechaInscripcion(proximaGeneracion);
        inscripcion.setProximaGeneracion(proximaGeneracion);
        return inscripcion;
    }
}
