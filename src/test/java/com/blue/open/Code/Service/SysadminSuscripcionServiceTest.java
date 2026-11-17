package com.blue.open.Code.Service;

import com.blue.open.Code.Entity.DTO.SysadminCambioPlanRequestDTO;
import com.blue.open.Code.Entity.DTO.SysadminSuscripcionRequestDTO;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Repository.CompanyRepository;
import com.blue.open.Code.Repository.CompanySuscripcionCrmRepository;
import com.blue.open.Code.Service.support.RecordingClubCrmSincronizacionService;
import com.blue.open.Code.Service.support.RecordingCompanySuscripcionCrmService;
import com.blue.open.Core.Exceptions.CustomException;
import com.blue.open.Crm.Entity.DTO.Request.CrmDesuscripcionRequestDTO;
import com.blue.open.Crm.Entity.DTO.Response.CrmCancelarSuscripcionesResponseDTO;
import com.blue.open.Crm.Entity.DTO.Response.CrmDesuscripcionResponseDTO;
import com.blue.open.Crm.Entity.DTO.Response.CrmPlanesClienteResponseDTO;
import com.blue.open.Crm.Entity.DTO.Response.CrmSuscripcionActivaDTO;
import com.blue.open.Crm.Entity.DTO.Response.CrmSuscripcionResponseDTO;
import com.blue.open.Crm.Service.CrmIntegracionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SysadminSuscripcionServiceTest {

    private static final int STATUS_ACTIVE = 0;
    private static final String COMPANY_UUID = "club-uuid";
    private static final String CUIT = "20111111111";

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private CompanySuscripcionCrmRepository companySuscripcionCrmRepository;

    @Mock
    private CompanyAccesoService companyAccesoService;

    // Igual que en SuscripcionVerificacionServiceTest: CrmIntegracionService no se puede mockear con
    // Mockito en esta JVM (Java 25), asi que se usa una subclase que registra las llamadas.
    private CrmPlanesClienteResponseDTO respuestaPlanesCliente;
    private CrmSuscripcionResponseDTO respuestaSuscribir;
    private CrmCancelarSuscripcionesResponseDTO respuestaCancelar;
    private CrmDesuscripcionRequestDTO desuscribirRequest;
    private String cancelarCuit;

    private final CrmIntegracionService crmIntegracionService = new CrmIntegracionService(null, null) {
        @Override
        public CrmPlanesClienteResponseDTO obtenerPlanesCliente(String cuit) {
            return respuestaPlanesCliente;
        }

        @Override
        public CrmDesuscripcionResponseDTO desuscribirCliente(CrmDesuscripcionRequestDTO request) {
            desuscribirRequest = request;
            return new CrmDesuscripcionResponseDTO();
        }

        @Override
        public CrmCancelarSuscripcionesResponseDTO cancelarSuscripcionesCliente(String cuit) {
            cancelarCuit = cuit;
            return respuestaCancelar;
        }
    };

    private SysadminSuscripcionService service;
    private RecordingClubCrmSincronizacionService clubCrmSincronizacionService;
    private RecordingCompanySuscripcionCrmService companySuscripcionCrmService;
    private Company company;

    @BeforeEach
    void setUp() {
        clubCrmSincronizacionService = new RecordingClubCrmSincronizacionService();
        companySuscripcionCrmService = new RecordingCompanySuscripcionCrmService();

        service = new SysadminSuscripcionService();
        ReflectionTestUtils.setField(service, "companyRepository", companyRepository);
        ReflectionTestUtils.setField(service, "companySuscripcionCrmRepository", companySuscripcionCrmRepository);
        ReflectionTestUtils.setField(service, "companySuscripcionCrmService", companySuscripcionCrmService);
        ReflectionTestUtils.setField(service, "clubCrmSincronizacionService", clubCrmSincronizacionService);
        ReflectionTestUtils.setField(service, "crmIntegracionService", crmIntegracionService);
        ReflectionTestUtils.setField(service, "companyAccesoService", companyAccesoService);

        company = new Company();
        company.setId(10L);
        company.setUuid(COMPANY_UUID);
        company.setCuit(CUIT);

        // La suscripcion al plan pasa por ClubCrmSincronizacionService.suscribir, no por el
        // CrmIntegracionService de este test
        respuestaSuscribir = new CrmSuscripcionResponseDTO();
        clubCrmSincronizacionService.setRespuesta(respuestaSuscribir);
        respuestaCancelar = new CrmCancelarSuscripcionesResponseDTO();
        respuestaPlanesCliente = new CrmPlanesClienteResponseDTO();

        lenient().when(companyRepository.findFirstByUuidWithStatusIsActive(COMPANY_UUID)).thenReturn(company);
        lenient().when(companySuscripcionCrmRepository.findFirstByCompanyIdWithPlan(10L, STATUS_ACTIVE))
                .thenReturn(Optional.empty());
    }

    // Dado un club sin suscripciones previas en el CRM, cuando suscribir, entonces suscribe al plan
    // y persiste la suscripcion, sin intentar desuscribir nada ni dar de alta el cliente.
    @Test
    void suscribir_sinSuscripcionesPrevias_suscribeSinCrearCliente() {
        respuestaPlanesCliente.setPlanes(List.of());

        service.suscribir(COMPANY_UUID, suscripcionRequest("plan-nuevo"));

        assertNull(desuscribirRequest);
        assertEquals("plan-nuevo", clubCrmSincronizacionService.getLastSuscribirCodigoPlan());
        assertEquals(CUIT, clubCrmSincronizacionService.getLastSuscribirCuit());
        assertTrue(companySuscripcionCrmService.wasRegistrarCalled());
        assertSame(respuestaSuscribir, companySuscripcionCrmService.getLastSuscripcionResponse());
    }

    // Dado un club bloqueado (el caso del recien dado de alta, que nace sin plan), cuando
    // suscribir, entonces le devuelve el acceso en el momento en vez de dejarlo esperando al job
    // de vigencia.
    @Test
    void suscribir_clubBloqueado_loDesbloquea() {
        respuestaPlanesCliente.setPlanes(List.of());
        when(companyAccesoService.estaBloqueada(company)).thenReturn(true);

        service.suscribir(COMPANY_UUID, suscripcionRequest("plan-nuevo"));

        verify(companyAccesoService).desbloquear(company);
    }

    // Dado un club que ya tenia acceso, cuando suscribir, entonces no lo toca.
    @Test
    void suscribir_clubSinBloquear_noLoDesbloquea() {
        respuestaPlanesCliente.setPlanes(List.of());
        when(companyAccesoService.estaBloqueada(company)).thenReturn(false);

        service.suscribir(COMPANY_UUID, suscripcionRequest("plan-nuevo"));

        verify(companyAccesoService, never()).desbloquear(any(Company.class));
    }

    // Dado un club que ya tiene una suscripcion vigente y otra anulada, cuando suscribir, entonces
    // desuscribe solo de la vigente: mandar la anulada haria que el CRM responda 404.
    @Test
    void suscribir_conSuscripcionVigente_desuscribeSoloDeLasNoAnuladas() {
        respuestaPlanesCliente.setPlanes(List.of(
                suscripcionActiva("plan-viejo", "ACTIVA"),
                suscripcionActiva("plan-anulado", "ANULADA")));

        service.suscribir(COMPANY_UUID, suscripcionRequest("plan-nuevo"));

        assertEquals(List.of("plan-viejo"), desuscribirRequest.getCodigosPlan());
        assertEquals(CUIT, desuscribirRequest.getCuit());
        assertEquals("plan-nuevo", clubCrmSincronizacionService.getLastSuscribirCodigoPlan());
    }

    // Dado un club cuyas suscripciones estan todas anuladas, cuando suscribir, entonces no llama a
    // desuscribir: el CRM devolveria 404 y cortaria el alta.
    @Test
    void suscribir_conTodasLasSuscripcionesAnuladas_noDesuscribe() {
        respuestaPlanesCliente.setPlanes(List.of(suscripcionActiva("plan-viejo", "ANULADA")));

        service.suscribir(COMPANY_UUID, suscripcionRequest("plan-nuevo"));

        assertNull(desuscribirRequest);
        assertTrue(companySuscripcionCrmService.wasRegistrarCalled());
    }

    // Dado un cambio de plan, cuando cambiarPlan, entonces cancela todas las suscripciones (lo que
    // compensa la deuda en el CRM) y suscribe al plan nuevo.
    @Test
    void cambiarPlan_cancelaTodasYSuscribeAlNuevo() {
        service.cambiarPlan(COMPANY_UUID, cambioPlanRequest("plan-nuevo"));

        assertEquals(CUIT, cancelarCuit);
        assertEquals("plan-nuevo", clubCrmSincronizacionService.getLastSuscribirCodigoPlan());
        assertTrue(companySuscripcionCrmService.wasRegistrarCalled());
        assertSame(respuestaSuscribir, companySuscripcionCrmService.getLastSuscripcionResponse());
    }

    // Dada la baja de un club, cuando desuscribir, entonces cancela en el CRM, marca la suscripcion
    // local como anulada y le corta el acceso en el momento, sin esperar al job de vigencia.
    @Test
    void desuscribir_cancelaAnulaLocalYBloqueaAcceso() {
        CrmCancelarSuscripcionesResponseDTO respuesta = service.desuscribir(COMPANY_UUID);

        assertSame(respuestaCancelar, respuesta);
        assertEquals(CUIT, cancelarCuit);
        assertTrue(companySuscripcionCrmService.wasAnularCalled());
        assertSame(company, companySuscripcionCrmService.getLastAnularCompany());
        verify(companyAccesoService).bloquear(company);
    }

    // Dado un club sin CUIT cargado, cuando desuscribir, entonces corta antes de salir al CRM: el
    // CRM identifica al cliente por CUIT.
    @Test
    void desuscribir_clubSinCuit_corta() {
        company.setCuit(null);

        CustomException ex = assertThrows(CustomException.class, () -> service.desuscribir(COMPANY_UUID));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertNull(cancelarCuit);
        assertFalse(companySuscripcionCrmService.wasAnularCalled());
    }

    // Dado un uuid que no corresponde a ningun club activo, cuando suscribir, entonces 404 sin
    // tocar el CRM.
    @Test
    void suscribir_companyInexistente_devuelve404() {
        when(companyRepository.findFirstByUuidWithStatusIsActive("otro-uuid")).thenReturn(null);

        CustomException ex = assertThrows(CustomException.class,
                () -> service.suscribir("otro-uuid", suscripcionRequest("plan-nuevo")));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    private SysadminSuscripcionRequestDTO suscripcionRequest(String codigoPlan) {
        SysadminSuscripcionRequestDTO request = new SysadminSuscripcionRequestDTO();
        request.setCodigoPlan(codigoPlan);
        return request;
    }

    private SysadminCambioPlanRequestDTO cambioPlanRequest(String codigoPlan) {
        SysadminCambioPlanRequestDTO request = new SysadminCambioPlanRequestDTO();
        request.setCodigoPlan(codigoPlan);
        return request;
    }

    private CrmSuscripcionActivaDTO suscripcionActiva(String codigoPlan, String estado) {
        CrmSuscripcionActivaDTO suscripcion = new CrmSuscripcionActivaDTO();
        suscripcion.setCodigoPlan(codigoPlan);
        suscripcion.setEstadoSuscripcion(estado);
        return suscripcion;
    }
}
