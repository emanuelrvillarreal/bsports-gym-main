package com.blue.open.Code.Service;

import com.blue.open.Code.Entity.DTO.SuscripcionVerificacionResumenDTO;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.CompanySuscripcionCrm;
import com.blue.open.Code.Entity.Db.Factura;
import com.blue.open.Code.Entity.Enums.FacturaEstadoEnum;
import com.blue.open.Code.Repository.CompanyRepository;
import com.blue.open.Code.Repository.CompanySuscripcionCrmRepository;
import com.blue.open.Code.Repository.FacturaRepository;
import com.blue.open.Code.Service.support.RecordingCompanySuscripcionCrmService;
import com.blue.open.Crm.Entity.DTO.Response.CrmPlanesClienteResponseDTO;
import com.blue.open.Crm.Entity.DTO.Response.CrmSuscripcionActivaDTO;
import com.blue.open.Crm.Service.CrmIntegracionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SuscripcionVerificacionServiceTest {

    private static final int STATUS_ACTIVE = 0;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private FacturaRepository facturaRepository;

    @Mock
    private CompanyAccesoService companyAccesoService;

    @Mock
    private CompanySuscripcionCrmRepository companySuscripcionCrmRepository;

    // Mockito no puede mockear CrmIntegracionService en esta JVM (Java 25): el mock-maker
    // "inline" (bytebuddy) tira VerifyError al retransformar la clase, probablemente por la
    // cantidad de lambdas que genera cada metodo (un Supplier por llamada al CRM). En vez de
    // @Mock, se usa una subclase que overridea el unico metodo que este service llama; el
    // constructor recibe null porque nunca se usa (el override no lo necesita).
    private CrmPlanesClienteResponseDTO respuestaObtenerPlanesCliente;
    private boolean obtenerPlanesClienteLlamado;

    private final CrmIntegracionService crmIntegracionService = new CrmIntegracionService(null, null) {
        @Override
        public CrmPlanesClienteResponseDTO obtenerPlanesCliente(String cuit) {
            obtenerPlanesClienteLlamado = true;
            return respuestaObtenerPlanesCliente;
        }
    };

    private SuscripcionVerificacionService service;
    private RecordingCompanySuscripcionCrmService companySuscripcionCrmService;

    @BeforeEach
    void setUp() {
        companySuscripcionCrmService = new RecordingCompanySuscripcionCrmService();

        service = new SuscripcionVerificacionService();
        ReflectionTestUtils.setField(service, "companyRepository", companyRepository);
        ReflectionTestUtils.setField(service, "facturaRepository", facturaRepository);
        ReflectionTestUtils.setField(service, "crmIntegracionService", crmIntegracionService);
        ReflectionTestUtils.setField(service, "companyAccesoService", companyAccesoService);
        ReflectionTestUtils.setField(service, "companySuscripcionCrmRepository", companySuscripcionCrmRepository);
        ReflectionTestUtils.setField(service, "companySuscripcionCrmService", companySuscripcionCrmService);

        // Sin facturas por vencer, salvo que el test diga lo contrario
        lenient().when(facturaRepository.findByEstadoAndFechaVencimientoLessThanAndStatus(
                eq(FacturaEstadoEnum.PENDIENTE), any(LocalDate.class), eq(STATUS_ACTIVE), any(Pageable.class)))
                .thenReturn(List.of());
    }

    // Dada una company con historial de facturacion CRM, cuya suscripcion no esta ANULADA y su
    // fechaProximaFacturacion es el mes que viene, cuando verificarTodas, entonces desbloquea el
    // acceso, nunca lo bloquea, y espeja la suscripcion que informo el CRM.
    @Test
    void verificarTodas_suscripcionVigenteProximaFacturacionMesQueViene_desbloquea() {
        Company company = companyConFacturas(1L, "20111111111");
        CrmSuscripcionActivaDTO suscripcion = stubPlanes(company, "ACTIVA", LocalDateTime.now().plusMonths(1));

        SuscripcionVerificacionResumenDTO resumen = service.verificarTodas(LocalDate.now());

        verify(companyAccesoService).desbloquear(company);
        verify(companyAccesoService, never()).bloquear(any());
        assertSame(suscripcion, companySuscripcionCrmService.getLastSuscripcion());
        assertEquals(1, resumen.getClubesEvaluados());
        assertEquals(1, resumen.getClubesDesbloqueados());
    }

    // Dada una company con una factura VENCIDA, cuando verificarTodas, entonces la bloquea SIN
    // consultar al CRM: el motivo local alcanza para cortar y ahorra la llamada HTTP.
    @Test
    void verificarTodas_conFacturaVencida_bloqueaSinConsultarAlCrm() {
        Company company = companyConFacturas(2L, "20222222222");
        when(facturaRepository.existsByCompanyAndEstadoAndStatus(company, FacturaEstadoEnum.VENCIDA, STATUS_ACTIVE))
                .thenReturn(true);

        service.verificarTodas(LocalDate.now());

        verify(companyAccesoService).bloquear(company);
        verify(companyAccesoService, never()).desbloquear(any());
        assertFalse(obtenerPlanesClienteLlamado);
        assertFalse(companySuscripcionCrmService.wasCalled());
    }

    // El caso que motivo unificar los jobs: un club bloqueado por tener la suscripcion ANULADA y
    // sin ninguna factura vencida NO se desbloquea. Antes el job de facturas lo desbloqueaba por no
    // encontrarle facturas VENCIDA, deshaciendo lo que el job de vigencia habia decidido.
    @Test
    void verificarTodas_suscripcionAnuladaSinFacturasVencidas_bloqueaYNoDesbloquea() {
        Company company = companyConFacturas(3L, "20333333333");
        CrmSuscripcionActivaDTO suscripcion = stubPlanes(company, "ANULADA", LocalDateTime.now().plusMonths(1));

        service.verificarTodas(LocalDate.now());

        verify(companyAccesoService).bloquear(company);
        verify(companyAccesoService, never()).desbloquear(any());
        // La anulacion tambien se espeja: el CRM es la fuente de verdad
        assertSame(suscripcion, companySuscripcionCrmService.getLastSuscripcion());
    }

    // Dada una company con suscripcion ACTIVA y vigente pero con una factura VENCIDA, cuando
    // verificarTodas, entonces bloquea igual: la deuda pesa aunque la suscripcion este al dia.
    @Test
    void verificarTodas_suscripcionVigenteConFacturaVencida_bloqueaIgual() {
        Company company = companyConFacturas(4L, "20444444444");
        when(facturaRepository.existsByCompanyAndEstadoAndStatus(company, FacturaEstadoEnum.VENCIDA, STATUS_ACTIVE))
                .thenReturn(true);

        service.verificarTodas(LocalDate.now());

        verify(companyAccesoService).bloquear(company);
        verify(companyAccesoService, never()).desbloquear(any());
    }

    // Dada una company con suscripcion ACTIVA cuya fechaProximaFacturacion fue hace 3 dias (mas
    // alla de los 2 dias de gracia), cuando verificarTodas, entonces bloquea: el ciclo pagado ya
    // vencio sin que llegara una renovacion.
    @Test
    void verificarTodas_fechaProximaFacturacionVencidaMasAlaDeLaGracia_bloquea() {
        Company company = companyConFacturas(5L, "20555555555");
        stubPlanes(company, "ACTIVA", LocalDateTime.now().minusDays(3));

        service.verificarTodas(LocalDate.now());

        verify(companyAccesoService).bloquear(company);
        verify(companyAccesoService, never()).desbloquear(any());
    }

    // Dada una company SIN historial de facturacion pero con su suscripcion ya registrada (club
    // recien dado de alta), cuando verificarTodas, entonces igual la procesa: el guard es
    // "gestionada por el CRM", no "tiene facturas".
    @Test
    void verificarTodas_sinFacturasPeroConSuscripcionRegistrada_laProcesa() {
        Company company = company(6L, "20666666666");
        when(facturaRepository.existsByCompanyAndStatus(company, STATUS_ACTIVE)).thenReturn(false);
        when(companySuscripcionCrmRepository.findFirstByCompanyIdAndStatus(6L, STATUS_ACTIVE))
                .thenReturn(Optional.of(new CompanySuscripcionCrm()));
        stubPlanes(company, "ACTIVA", LocalDateTime.now().plusMonths(1));

        service.verificarTodas(LocalDate.now());

        verify(companyAccesoService).desbloquear(company);
    }

    // Dada una company sin facturas y sin suscripcion registrada, cuando verificarTodas, entonces
    // no la toca ni la cuenta: no es un club gestionado por el CRM.
    @Test
    void verificarTodas_niFacturasNiSuscripcion_noLaProcesa() {
        Company company = company(7L, "20777777777");
        when(facturaRepository.existsByCompanyAndStatus(company, STATUS_ACTIVE)).thenReturn(false);
        when(companySuscripcionCrmRepository.findFirstByCompanyIdAndStatus(7L, STATUS_ACTIVE))
                .thenReturn(Optional.empty());

        SuscripcionVerificacionResumenDTO resumen = service.verificarTodas(LocalDate.now());

        verify(companyAccesoService, never()).bloquear(any());
        verify(companyAccesoService, never()).desbloquear(any());
        assertFalse(obtenerPlanesClienteLlamado);
        assertEquals(0, resumen.getClubesEvaluados());
    }

    // Dada una company cuyo CUIT el CRM no relaciona con ninguna suscripcion de este sistema
    // externo, cuando verificarTodas, entonces bloquea y no espeja nada.
    @Test
    void verificarTodas_sinSuscripcionesEnElCrm_bloqueaYNoEspeja() {
        Company company = companyConFacturas(8L, "20888888888");
        CrmPlanesClienteResponseDTO planes = new CrmPlanesClienteResponseDTO();
        planes.setPlanes(List.of());
        respuestaObtenerPlanesCliente = planes;

        service.verificarTodas(LocalDate.now());

        verify(companyAccesoService).bloquear(company);
        assertFalse(companySuscripcionCrmService.wasCalled());
    }

    // Dado que el espejado falla, cuando verificarTodas, entonces el bloqueo se aplica igual:
    // refrescar la fila es accesorio, resolver el acceso es lo critico.
    @Test
    void verificarTodas_falloAlEspejar_igualAplicaElBloqueo() {
        Company company = companyConFacturas(9L, "20999999999");
        stubPlanes(company, "ANULADA", null);
        companySuscripcionCrmService.setExceptionToThrow(new IllegalStateException("db caida"));

        service.verificarTodas(LocalDate.now());

        verify(companyAccesoService).bloquear(company);
    }

    // Las dos fases corren en la misma invocacion: primero se marcan las facturas vencidas y
    // despues se resuelve el acceso de cada club, todo en una sola corrida del job.
    @Test
    void verificarTodas_marcaFacturasVencidasYResuelveAccesoEnLaMismaCorrida() {
        Company company = companyConFacturas(10L, "20101010109");
        stubPlanes(company, "ACTIVA", LocalDateTime.now().plusMonths(1));

        Factura pendiente = Factura.crear(company, null, "20123456789", "uuid-crm-1", List.of("plan"), List.of(), null);
        when(facturaRepository.findByEstadoAndFechaVencimientoLessThanAndStatus(
                eq(FacturaEstadoEnum.PENDIENTE), any(LocalDate.class), eq(STATUS_ACTIVE), any(Pageable.class)))
                .thenReturn(List.of(pendiente))
                .thenReturn(List.of());

        SuscripcionVerificacionResumenDTO resumen = service.verificarTodas(LocalDate.now());

        assertEquals(FacturaEstadoEnum.VENCIDA, pendiente.getEstado());
        assertEquals(1, resumen.getFacturasVencidas());
        verify(companyAccesoService).desbloquear(company);
    }

    // Dado que el CRM falla para un club, cuando verificarTodas, entonces se cuenta el error y no
    // se cambia su acceso: falla abierta, nunca un desbloqueo a ciegas.
    @Test
    void verificarTodas_falloDelCrm_noCambiaElAccesoYCuentaElError() {
        Company company = companyConFacturas(11L, "20111111112");
        respuestaObtenerPlanesCliente = null; // NPE dentro del try por club

        SuscripcionVerificacionResumenDTO resumen = service.verificarTodas(LocalDate.now());

        verify(companyAccesoService, never()).bloquear(any());
        verify(companyAccesoService, never()).desbloquear(any());
        assertEquals(1, resumen.getErroresCrm());
        assertTrue(obtenerPlanesClienteLlamado);
    }

    private Company company(Long id, String cuit) {
        Company company = new Company();
        company.setId(id);
        company.setCuit(cuit);
        when(companyRepository.findByStatus(STATUS_ACTIVE)).thenReturn(List.of(company));
        return company;
    }

    private Company companyConFacturas(Long id, String cuit) {
        Company company = company(id, cuit);
        when(facturaRepository.existsByCompanyAndStatus(company, STATUS_ACTIVE)).thenReturn(true);
        return company;
    }

    private CrmSuscripcionActivaDTO stubPlanes(Company company, String estado, LocalDateTime proximaFacturacion) {
        CrmSuscripcionActivaDTO suscripcion = new CrmSuscripcionActivaDTO();
        suscripcion.setEstadoSuscripcion(estado);
        suscripcion.setFechaProximaFacturacion(proximaFacturacion);

        CrmPlanesClienteResponseDTO planes = new CrmPlanesClienteResponseDTO();
        planes.setCuit(company.getCuit());
        planes.setPlanes(List.of(suscripcion));
        respuestaObtenerPlanesCliente = planes;
        return suscripcion;
    }
}
