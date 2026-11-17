package com.blue.open.Code.Service.Tesoreria;

import com.blue.open.Code.Entity.Enums.PeriodicidadCuotaEnum;
import com.blue.open.Code.Entity.DTO.CuotaSocialAltaResultDTO;
import com.blue.open.Code.Entity.DTO.CuotaSocialGeneracionResultDTO;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.Cuota;
import com.blue.open.Code.Entity.Db.CuotaSocialRango;
import com.blue.open.Code.Entity.Db.Member;
import com.blue.open.Code.Entity.Enums.CuotaEstadoEnum;
import com.blue.open.Code.Entity.Enums.CuotaTipoEnum;
import com.blue.open.Code.Repository.CompanyRepository;
import com.blue.open.Code.Repository.CuotaRepository;
import com.blue.open.Code.Repository.MemberDescuentoRepository;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
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
class CuotaSocialGeneracionServiceTest {

    private static final Long COMPANY_ID = 1L;
    private static final Long MEMBER_ID = 10L;
    private static final LocalDate PROXIMA_GENERACION = LocalDate.of(2026, 10, 2);
    private static final PeriodicidadCuotaEnum PERIODICIDAD_CUOTA_SOCIAL = PeriodicidadCuotaEnum.SEMANAL;
    private static final int DIAS_VENCIMIENTO_CUOTA = 10;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private CuotaRepository cuotaRepository;

    @Mock
    private MemberDescuentoRepository memberDescuentoRepository;

    @Mock
    private TransactionTemplate transactionTemplate;

    private CuotaSocialGeneracionService cuotaSocialGeneracionService;

    private Company company;

    private Member socio;

    @BeforeEach
    void setUp() {
        cuotaSocialGeneracionService = new CuotaSocialGeneracionService();
        ReflectionTestUtils.setField(cuotaSocialGeneracionService, "companyRepository", companyRepository);
        ReflectionTestUtils.setField(cuotaSocialGeneracionService, "memberRepository", memberRepository);
        ReflectionTestUtils.setField(cuotaSocialGeneracionService, "cuotaRepository", cuotaRepository);
        ReflectionTestUtils.setField(cuotaSocialGeneracionService, "memberDescuentoRepository",
                memberDescuentoRepository);
        ReflectionTestUtils.setField(cuotaSocialGeneracionService, "transactionTemplate", transactionTemplate);

        company = new Company();
        company.setId(COMPANY_ID);
        company.setCuotaSocialHabilitada(true);
        company.setPeriodicidadCuotaSocial(PERIODICIDAD_CUOTA_SOCIAL);
        company.setDiasVencimientoCuota(DIAS_VENCIMIENTO_CUOTA);

        socio = socioConProximaGeneracion(company, PROXIMA_GENERACION);
    }

    // ---------------------------------------------------------------------------------------
    // generarHastaParaCompany: corrida de generación (job / endpoint manual)
    // ---------------------------------------------------------------------------------------

    @Test
    void generarHastaParaCompany_hoyIgualAProximaGeneracion_generaUnaCuotaYAvanzaSieteDias() {
        prepararCorridaDeGeneracion();
        LocalDate hoy = LocalDate.of(2026, 10, 2);

        CuotaSocialGeneracionResultDTO result =
                cuotaSocialGeneracionService.generarHastaParaCompany(COMPANY_ID, hoy);

        assertEquals(1, result.getCuotasGeneradas());
        assertEquals(0, result.getCuotasOmitidas());
        List<Cuota> cuotas = capturarCuotasGuardadas();
        assertEquals(1, cuotas.size());
        assertEquals(LocalDate.of(2026, 10, 2), cuotas.get(0).getFechaGeneracion());
        assertEquals(LocalDate.of(2026, 10, 9), socio.getProximaGeneracionCuota());
    }

    @Test
    void generarHastaParaCompany_hoyUnDiaDespuesDeProximaGeneracion_generaUnaCuotaYAvanzaSieteDias() {
        prepararCorridaDeGeneracion();
        LocalDate hoy = LocalDate.of(2026, 10, 3);

        CuotaSocialGeneracionResultDTO result =
                cuotaSocialGeneracionService.generarHastaParaCompany(COMPANY_ID, hoy);

        assertEquals(1, result.getCuotasGeneradas());
        assertEquals(0, result.getCuotasOmitidas());
        List<Cuota> cuotas = capturarCuotasGuardadas();
        assertEquals(1, cuotas.size());
        // La cuota toma la fecha programada, no la de hoy: así no se corre el ciclo.
        assertEquals(LocalDate.of(2026, 10, 2), cuotas.get(0).getFechaGeneracion());
        assertEquals(LocalDate.of(2026, 10, 9), socio.getProximaGeneracionCuota());
    }

    @Test
    void generarHastaParaCompany_hoyAntesDeProximaGeneracion_noGeneraCuotas() {
        prepararCorridaDeGeneracion();
        LocalDate hoy = LocalDate.of(2026, 10, 1);

        CuotaSocialGeneracionResultDTO result =
                cuotaSocialGeneracionService.generarHastaParaCompany(COMPANY_ID, hoy);

        assertEquals(0, result.getCuotasGeneradas());
        assertEquals(0, result.getCuotasOmitidas());
        verify(cuotaRepository, never()).saveAll(anyList());
        assertEquals(PROXIMA_GENERACION, socio.getProximaGeneracionCuota());
    }

    @Test
    void generarHastaParaCompany_hoyVeintiunDiasDespuesDeProximaGeneracion_generaCuatroCuotas() {
        prepararCorridaDeGeneracion();
        // 21 días con ciclo de 7: el 23/10 también es día de generación, así que son 4 ciclos.
        LocalDate hoy = PROXIMA_GENERACION.plusDays(21);

        CuotaSocialGeneracionResultDTO result =
                cuotaSocialGeneracionService.generarHastaParaCompany(COMPANY_ID, hoy);

        assertEquals(4, result.getCuotasGeneradas());
        assertEquals(0, result.getCuotasOmitidas());
        List<Cuota> cuotas = capturarCuotasGuardadas();
        assertEquals(List.of(
                        LocalDate.of(2026, 10, 2),
                        LocalDate.of(2026, 10, 9),
                        LocalDate.of(2026, 10, 16),
                        LocalDate.of(2026, 10, 23)),
                cuotas.stream().map(Cuota::getFechaGeneracion).toList());
        assertEquals(LocalDate.of(2026, 10, 30), socio.getProximaGeneracionCuota());
    }

    // ---------------------------------------------------------------------------------------
    // generarPorAlta: primera cuota, en el alta del socio
    // ---------------------------------------------------------------------------------------

    @Test
    void generarPorAlta_clubConCuotaSocial_generaCuotaConFechaDeAltaYAvanzaPuntero() {
        prepararAltaDeSocio();
        LocalDate fechaAlta = LocalDate.of(2026, 10, 2);

        CuotaSocialAltaResultDTO result = cuotaSocialGeneracionService.generarPorAlta(MEMBER_ID, fechaAlta);

        Cuota cuota = capturarCuotaGuardada();
        assertEquals(CuotaTipoEnum.SOCIAL, cuota.getTipo());
        assertEquals(LocalDate.of(2026, 10, 2), cuota.getFechaGeneracion());
        assertEquals(LocalDate.of(2026, 10, 12), cuota.getVencimiento());
        assertEquals(CuotaEstadoEnum.PENDIENTE, cuota.getEstado());
        assertTrue(result.isGenerada());
        // El alta ya generó la cuota de la fecha de alta: la próxima queda un ciclo después.
        assertEquals(LocalDate.of(2026, 10, 9), socio.getProximaGeneracionCuota());
    }

    @Test
    void generarPorAlta_clubSinCuotaSocial_omiteSinGuardarYNoAvanzaPuntero() {
        company.setCuotaSocialHabilitada(false);
        when(memberRepository.findByIdWithTitular(MEMBER_ID)).thenReturn(Optional.of(socio));

        CuotaSocialAltaResultDTO result = cuotaSocialGeneracionService.generarPorAlta(MEMBER_ID, PROXIMA_GENERACION);

        assertTrue(result.isOmitida());
        assertFalse(result.isGenerada());
        assertEquals("El club no tiene cuota social habilitada", result.getMotivoOmitida());
        verify(cuotaRepository, never()).save(any(Cuota.class));
        assertEquals(PROXIMA_GENERACION, socio.getProximaGeneracionCuota());
    }

    @Test
    void generarPorAlta_rangoConImporteCero_generaCuotaPagada() {
        prepararAltaDeSocio();
        socio.getCuotaSocialRango().setImporte(BigDecimal.ZERO);

        cuotaSocialGeneracionService.generarPorAlta(MEMBER_ID, PROXIMA_GENERACION);

        Cuota cuota = capturarCuotaGuardada();
        assertEquals(CuotaEstadoEnum.PAGADA, cuota.getEstado());
        assertEquals(LocalDate.of(2026, 10, 9), socio.getProximaGeneracionCuota());
    }

    @Test
    void generarPorAlta_sinMemberId_lanzaBadRequest() {
        CustomException ex = assertThrows(CustomException.class, () ->
                cuotaSocialGeneracionService.generarPorAlta(null, PROXIMA_GENERACION));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        verify(cuotaRepository, never()).save(any(Cuota.class));
    }

    /** Stubs de la corrida por fecha: transacción, club y el socio pendiente. */
    private void prepararCorridaDeGeneracion() {
        // El template ejecuta el callback en el mismo hilo, sin transacción real.
        when(transactionTemplate.execute(any())).thenAnswer(invocation ->
                invocation.<TransactionCallback<?>>getArgument(0).doInTransaction(null));
        when(companyRepository.findById(COMPANY_ID)).thenReturn(Optional.of(company));

        // Se devuelve el socio sin filtrar por fecha: así lo que se prueba es la regla del
        // servicio (generar mientras proximaGeneracionCuota <= hasta), no el WHERE de la query.
        when(memberRepository.findConGeneracionCuotaPendiente(eq(COMPANY_ID), anyCollection(), any()))
                .thenReturn(List.of(socio));
    }

    /** Stubs del alta de socio: el socio y el save de la cuota. */
    private void prepararAltaDeSocio() {
        when(memberRepository.findByIdWithTitular(MEMBER_ID)).thenReturn(Optional.of(socio));
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

    private static Member socioConProximaGeneracion(Company company, LocalDate proximaGeneracion) {
        CuotaSocialRango rango = new CuotaSocialRango();
        rango.setId(40L);
        rango.setImporte(new BigDecimal("15000.00"));

        Member socio = new Member();
        socio.setId(MEMBER_ID);
        socio.setCompany(company);
        socio.setCuotaSocialRango(rango);
        socio.setFechaAlta(proximaGeneracion);
        socio.setProximaGeneracionCuota(proximaGeneracion);
        return socio;
    }
}
