package com.blue.open.Code.Service;

import com.blue.open.Code.Entity.Enums.PeriodicidadCuotaEnum;
import com.blue.open.Code.Entity.DTO.ClubRegistrationRequestDTO;
import com.blue.open.Code.Entity.DTO.ClubRegistrationResponseDTO;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.CompanyMercadoPago;
import com.blue.open.Code.Entity.Db.CompanySuscripcionCrm;
import com.blue.open.Code.Entity.Db.User;
import com.blue.open.Code.Mapper.CompanyMapper;
import com.blue.open.Code.Mapper.UserMapper;
import com.blue.open.Core.Mapper.PermissionMapper;
import com.blue.open.Core.Mapper.RoleMapper;
import com.blue.open.Core.Mapper.UserCompanyRolesMapper;
import com.blue.open.Code.Repository.CompanyRepository;
import com.blue.open.Code.Service.support.RecordingClubCrmSincronizacionService;
import com.blue.open.Code.Service.support.RecordingCompanySuscripcionCrmService;
import com.blue.open.Core.Entity.Db.Permission;
import com.blue.open.Core.Entity.Db.Role;
import com.blue.open.Core.Entity.Db.UserCompanyRole;
import com.blue.open.Core.Exceptions.CustomException;
import com.blue.open.Core.Repository.PermissionRepository;
import com.blue.open.Core.Repository.RoleRepository;
import com.blue.open.Core.Repository.UserCompanyRoleRepository;
import com.blue.open.Core.Repository.UserRepository;
import com.blue.open.Crm.Entity.DTO.Response.CrmPlanesClienteResponseDTO;
import com.blue.open.Crm.Entity.DTO.Response.CrmSuscripcionActivaDTO;
import com.blue.open.Crm.Service.CrmIntegracionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SysadminClubServiceTest {

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserCompanyRoleRepository userCompanyRoleRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PermissionRepository permissionRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private CompanyAccesoService companyAccesoService;

    private SysadminClubService sysadminClubService;

    private RecordingClubCrmSincronizacionService clubCrmSincronizacionService;

    private RecordingCompanySuscripcionCrmService companySuscripcionCrmService;

    // Igual que en SysadminSuscripcionServiceTest: CrmIntegracionService no se puede mockear con
    // Mockito en esta JVM (Java 25), asi que se usa una subclase que devuelve lo preparado
    private CrmPlanesClienteResponseDTO respuestaPlanesCliente;
    private String planesClienteCuit;

    private final CrmIntegracionService crmIntegracionService = new CrmIntegracionService(null, null) {
        @Override
        public CrmPlanesClienteResponseDTO obtenerPlanesCliente(String cuit) {
            planesClienteCuit = cuit;
            return respuestaPlanesCliente;
        }
    };

    private static final String VALID_CUIT = CuitValidatorServiceTest.VALID_CUIT_FOR_TESTS;

    @BeforeEach
    void setUp() {
        ClubRoleProvisioningService clubRoleProvisioningService = new ClubRoleProvisioningService();
        ReflectionTestUtils.setField(clubRoleProvisioningService, "roleRepository", roleRepository);
        ReflectionTestUtils.setField(clubRoleProvisioningService, "permissionRepository", permissionRepository);

        sysadminClubService = new SysadminClubService();
        ReflectionTestUtils.setField(sysadminClubService, "companyRepository", companyRepository);
        ReflectionTestUtils.setField(sysadminClubService, "userRepository", userRepository);
        ReflectionTestUtils.setField(sysadminClubService, "userCompanyRoleRepository", userCompanyRoleRepository);
        ReflectionTestUtils.setField(sysadminClubService, "companyMapper", new CompanyMapper());

        UserMapper userMapper = new UserMapper();
        UserCompanyRolesMapper userCompanyRolesMapper = new UserCompanyRolesMapper();
        RoleMapper roleMapper = new RoleMapper();
        ReflectionTestUtils.setField(roleMapper, "permissionMapper", new PermissionMapper());
        ReflectionTestUtils.setField(userCompanyRolesMapper, "roleMapper", roleMapper);
        ReflectionTestUtils.setField(userCompanyRolesMapper, "companyRepository", companyRepository);
        ReflectionTestUtils.setField(userCompanyRolesMapper, "roleRepository", roleRepository);
        // El mapper consulta el bloqueo de acceso del club al armar la respuesta del alta
        ReflectionTestUtils.setField(userCompanyRolesMapper, "companyAccesoService", companyAccesoService);
        lenient().when(companyAccesoService.estaBloqueada(any(Company.class))).thenReturn(false);
        ReflectionTestUtils.setField(userMapper, "userCompanyRolesMapper", userCompanyRolesMapper);
        ReflectionTestUtils.setField(sysadminClubService, "userMapper", userMapper);
        ReflectionTestUtils.setField(sysadminClubService, "clubRoleProvisioningService", clubRoleProvisioningService);
        ReflectionTestUtils.setField(sysadminClubService, "passwordEncoder", passwordEncoder);

        // El alta registra el club en el CRM y despues le pregunta por sus planes: sin estos
        // dobles, registerClub revienta con NPE antes de terminar
        clubCrmSincronizacionService = new RecordingClubCrmSincronizacionService();
        companySuscripcionCrmService = new RecordingCompanySuscripcionCrmService();
        ReflectionTestUtils.setField(sysadminClubService, "clubCrmSincronizacionService", clubCrmSincronizacionService);
        ReflectionTestUtils.setField(sysadminClubService, "companySuscripcionCrmService", companySuscripcionCrmService);
        ReflectionTestUtils.setField(sysadminClubService, "crmIntegracionService", crmIntegracionService);
        ReflectionTestUtils.setField(sysadminClubService, "companyAccesoService", companyAccesoService);

        // Por defecto el CRM no le conoce ningun plan al club recien registrado
        respuestaPlanesCliente = new CrmPlanesClienteResponseDTO();
        respuestaPlanesCliente.setPlanes(List.of());
    }

    @Test
    void registerClub_reusesDeletedCompanyByCuit() {
        stubNoActiveCompany();
        Company deleted = new Company();
        deleted.setId(99L);
        deleted.setUuid("club-uuid-reused");
        deleted.setStatus(-1);
        when(companyRepository.findFirstByCuitAndStatus(VALID_CUIT, -1)).thenReturn(deleted);
        when(companyRepository.findFirstByEmailInstitucionalIgnoreCaseAndStatus("info@catnorte.com", -1))
                .thenReturn(null);
        when(passwordEncoder.encode("ClaveSegura123!")).thenReturn("$2a$encoded");
        when(companyRepository.save(any(Company.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setUuid("admin-uuid-new");
            if (u.getUserCompanyRoles() == null) {
                u.setUserCompanyRoles(new ArrayList<>());
            }
            return u;
        });
        when(userCompanyRoleRepository.save(any(UserCompanyRole.class))).thenAnswer(invocation -> invocation.getArgument(0));
        stubRoleProvisioning();

        ClubRegistrationResponseDTO response = sysadminClubService.registerClub(validRequest());

        assertEquals("club-uuid-reused", response.getCompany().getUuid());
        assertEquals(0, response.getCompany().getStatus());

        ArgumentCaptor<Company> companyCaptor = ArgumentCaptor.forClass(Company.class);
        verify(companyRepository).save(companyCaptor.capture());
        assertEquals(99L, companyCaptor.getValue().getId());
        assertEquals(0, companyCaptor.getValue().getStatus());
        assertEquals("Club Atlético Norte", companyCaptor.getValue().getNombreComercial());
        assertEquals("1425", companyCaptor.getValue().getDomicilio().getCodigoPostal());
        assertMercadoPagoDefault(companyCaptor.getValue());
    }

    @Test
    void registerClub_rejectsConflictingDeletedRecords() {
        stubNoActiveCompany();
        Company deletedByCuit = new Company();
        deletedByCuit.setId(1L);
        deletedByCuit.setUuid("uuid-1");
        Company deletedByEmail = new Company();
        deletedByEmail.setId(2L);
        deletedByEmail.setUuid("uuid-2");
        when(companyRepository.findFirstByCuitAndStatus(VALID_CUIT, -1)).thenReturn(deletedByCuit);
        when(companyRepository.findFirstByEmailInstitucionalIgnoreCaseAndStatus("info@catnorte.com", -1))
                .thenReturn(deletedByEmail);

        CustomException ex = assertThrows(CustomException.class,
                () -> sysadminClubService.registerClub(validRequest()));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertTrue(ex.getUserMessage().contains("Datos en conflicto"));
    }

    @Test
    void registerClub_success() {
        stubNoDeletedCompany();
        when(passwordEncoder.encode("ClaveSegura123!")).thenReturn("$2a$encoded");
        when(companyRepository.save(any(Company.class))).thenAnswer(invocation -> {
            Company c = invocation.getArgument(0);
            c.setId(10L);
            c.setUuid("club-uuid-new");
            return c;
        });
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setUuid("admin-uuid-new");
            if (u.getUserCompanyRoles() == null) {
                u.setUserCompanyRoles(new ArrayList<>());
            }
            return u;
        });
        when(userCompanyRoleRepository.save(any(UserCompanyRole.class))).thenAnswer(invocation -> invocation.getArgument(0));
        stubRoleProvisioning();

        ClubRegistrationResponseDTO response = sysadminClubService.registerClub(validRequest());

        assertNotNull(response.getCompany());
        assertEquals("club-uuid-new", response.getCompany().getUuid());
        assertEquals("1425", response.getCompany().getDomicilio().getCodigoPostal());
        assertEquals("facturas@catnorte.com", response.getCompany().getEmailFacturaElectronica());
        assertTrue(response.getCompany().getAdminCreado());
        assertNotNull(response.getAdmin());
        assertEquals("admin@catnorte.com", response.getAdmin().getEmail());
        assertNull(response.getAdmin().getPassword());

        ArgumentCaptor<Company> companyCaptor = ArgumentCaptor.forClass(Company.class);
        verify(companyRepository).save(companyCaptor.capture());
        assertEquals("Club Atlético Norte", companyCaptor.getValue().getNombreComercial());
        assertEquals("facturas@catnorte.com", companyCaptor.getValue().getEmailFacturaElectronica());
        assertEquals("CABA", companyCaptor.getValue().getDomicilioFacturacion().getLocalidad());
        assertEquals(PeriodicidadCuotaEnum.BIMESTRAL, companyCaptor.getValue().getPeriodicidadCuotaSocial());
        assertTrue(companyCaptor.getValue().getAdminCreado());
        assertMercadoPagoDefault(companyCaptor.getValue());
        verify(roleRepository, org.mockito.Mockito.atLeastOnce())
                .findFirstByNameAndCompany(eq("ADMIN"), any(Company.class));

        // El alta registra el club en el CRM pero no lo suscribe: la suscripcion es un segundo
        // request (POST /sysadmin/clubs/{companyUuid}/crm/suscribir)
        assertTrue(clubCrmSincronizacionService.wasRegistrarClienteCalled());
        assertEquals(VALID_CUIT, clubCrmSincronizacionService.getLastRegistrarClienteRequest().getCuit());
        assertFalse(clubCrmSincronizacionService.wasSuscribirCalled());

        // Consulta los planes del cliente, pero el CRM no le conoce ninguno: no hay suscripcion
        // que espejar y el club queda bloqueado
        assertEquals(VALID_CUIT, planesClienteCuit);
        assertFalse(companySuscripcionCrmService.wasRegistrarDesdeCrmCalled());
        verify(companyAccesoService).bloquear(any(Company.class));
    }

    // Dado un club recien registrado al que el CRM no le conoce ningun plan, cuando registerClub,
    // entonces le bloquea el acceso: sin suscripcion no tiene cupos ni nada que lo habilite a
    // operar, asi que nadie del club deberia poder entrar hasta que lo suscriban.
    @Test
    void registerClub_sinSuscripcionEnElCrm_bloqueaElAcceso() {
        stubAltaCompleta();
        respuestaPlanesCliente.setPlanes(List.of());

        sysadminClubService.registerClub(validRequest());

        ArgumentCaptor<Company> bloqueadaCaptor = ArgumentCaptor.forClass(Company.class);
        verify(companyAccesoService).bloquear(bloqueadaCaptor.capture());
        assertEquals("club-uuid-new", bloqueadaCaptor.getValue().getUuid());
        assertFalse(companySuscripcionCrmService.wasRegistrarDesdeCrmCalled());
    }

    // Dado un club sin suscripcion porque el CRM devolvio una respuesta sin planes, cuando
    // registerClub, entonces tambien lo bloquea: se trata igual que no tener ninguno.
    @Test
    void registerClub_siElCrmDevuelvePlanesNull_bloqueaElAcceso() {
        stubAltaCompleta();
        respuestaPlanesCliente.setPlanes(null);

        sysadminClubService.registerClub(validRequest());

        verify(companyAccesoService).bloquear(any(Company.class));
        assertFalse(companySuscripcionCrmService.wasRegistrarDesdeCrmCalled());
    }

    // Dado un CUIT que ya era cliente del CRM con un plan vigente (el alta responde
    // CLIENTE_EXISTENTE), cuando registerClub, entonces espeja esa suscripcion en
    // company_suscripcion_crm: el club arranca con los cupos de ese plan sin esperar al segundo
    // request.
    @Test
    void registerClub_siElCrmYaLeConoceUnPlan_espejaLaSuscripcion() {
        stubAltaCompleta();
        CrmSuscripcionActivaDTO suscripcionVigente = new CrmSuscripcionActivaDTO();
        suscripcionVigente.setCodigoPlan("plan-estandar");
        suscripcionVigente.setUuidSuscripcion("suscripcion-uuid");
        suscripcionVigente.setEstadoSuscripcion("ACTIVA");
        respuestaPlanesCliente.setPlanes(List.of(suscripcionVigente));

        sysadminClubService.registerClub(validRequest());

        assertEquals(VALID_CUIT, planesClienteCuit);
        assertTrue(companySuscripcionCrmService.wasRegistrarDesdeCrmCalled());
        assertEquals("club-uuid-new",
                companySuscripcionCrmService.getLastRegistrarDesdeCrmCompany().getUuid());
        assertSame(suscripcionVigente,
                companySuscripcionCrmService.getLastRegistrarDesdeCrmSuscripcion());
        verify(companyAccesoService, never()).bloquear(any(Company.class));
        verify(companyAccesoService).desbloquear(any(Company.class));
    }

    // Dado un CUIT cuya suscripcion en el CRM esta ANULADA, cuando registerClub, entonces la
    // espeja igual pero le bloquea el acceso: una suscripcion de baja no habilita a operar.
    @Test
    void registerClub_siLaSuscripcionEstaAnulada_bloqueaElAcceso() {
        stubAltaCompleta();
        respuestaPlanesCliente.setPlanes(List.of(
                suscripcion(CompanySuscripcionCrm.ESTADO_ANULADA, LocalDateTime.now())));

        sysadminClubService.registerClub(validRequest());

        assertTrue(companySuscripcionCrmService.wasRegistrarDesdeCrmCalled());
        verify(companyAccesoService).bloquear(any(Company.class));
        verify(companyAccesoService, never()).desbloquear(any(Company.class));
    }

    // Dada una suscripcion PENDIENTE_PAGO que arranco hace mas de 10 dias, cuando registerClub,
    // entonces bloquea: se le paso la ventana para pagar la primera factura.
    @Test
    void registerClub_siEstaPendienteDePagoYVencioLaGracia_bloqueaElAcceso() {
        stubAltaCompleta();
        respuestaPlanesCliente.setPlanes(List.of(suscripcion(
                CompanySuscripcionCrm.ESTADO_PENDIENTE_PAGO, LocalDateTime.now().minusDays(11))));

        sysadminClubService.registerClub(validRequest());

        verify(companyAccesoService).bloquear(any(Company.class));
        verify(companyAccesoService, never()).desbloquear(any(Company.class));
    }

    // Dada una suscripcion PENDIENTE_PAGO dentro de la ventana de 10 dias, cuando registerClub,
    // entonces deja operar al club: todavia esta a tiempo de pagar.
    @Test
    void registerClub_siEstaPendienteDePagoDentroDeLaGracia_dejaAcceder() {
        stubAltaCompleta();
        respuestaPlanesCliente.setPlanes(List.of(suscripcion(
                CompanySuscripcionCrm.ESTADO_PENDIENTE_PAGO, LocalDateTime.now().minusDays(10))));

        sysadminClubService.registerClub(validRequest());

        verify(companyAccesoService).desbloquear(any(Company.class));
        verify(companyAccesoService, never()).bloquear(any(Company.class));
    }

    // Dada una suscripcion PENDIENTE_PAGO sin fecha de inicio, cuando registerClub, entonces deja
    // acceder: sin la fecha no se puede afirmar que la ventana venció, y el job de vigencia lo
    // vuelve a evaluar mañana.
    @Test
    void registerClub_siEstaPendienteDePagoSinFechaInicio_dejaAcceder() {
        stubAltaCompleta();
        respuestaPlanesCliente.setPlanes(List.of(
                suscripcion(CompanySuscripcionCrm.ESTADO_PENDIENTE_PAGO, null)));

        sysadminClubService.registerClub(validRequest());

        verify(companyAccesoService).desbloquear(any(Company.class));
        verify(companyAccesoService, never()).bloquear(any(Company.class));
    }

    // Dado un estado que el CRM podria agregar mas adelante, cuando registerClub, entonces deja
    // acceder: el catalogo de estados es abierto y uno nuevo no deberia dejar al club afuera.
    @Test
    void registerClub_siElEstadoEsDesconocido_dejaAcceder() {
        stubAltaCompleta();
        respuestaPlanesCliente.setPlanes(List.of(
                suscripcion("ESTADO_NUEVO_DEL_CRM", LocalDateTime.now().minusDays(400))));

        sysadminClubService.registerClub(validRequest());

        verify(companyAccesoService).desbloquear(any(Company.class));
        verify(companyAccesoService, never()).bloquear(any(Company.class));
    }

    // Dado un club con varias suscripciones en el CRM, cuando registerClub, entonces espeja solo
    // la primera: esta app admite un unico plan por club.
    @Test
    void registerClub_siElCrmLeConoceVariosPlanes_espejaElPrimero() {
        stubAltaCompleta();
        CrmSuscripcionActivaDTO primera = new CrmSuscripcionActivaDTO();
        primera.setCodigoPlan("plan-estandar");
        CrmSuscripcionActivaDTO segunda = new CrmSuscripcionActivaDTO();
        segunda.setCodigoPlan("plan-premium");
        respuestaPlanesCliente.setPlanes(List.of(primera, segunda));

        sysadminClubService.registerClub(validRequest());

        assertSame(primera, companySuscripcionCrmService.getLastRegistrarDesdeCrmSuscripcion());
    }

    // Dado que el CRM rechaza el registro del cliente (CUIT invalido, condicion impositiva
    // inexistente, etc.), cuando registerClub, entonces la excepcion propaga: corre dentro de la
    // transaccion del alta, asi que revierte el club recien creado en vez de dejarlo a medio
    // registrar.
    @Test
    void registerClub_siElRegistroEnElCrmFalla_propaga() {
        stubNoDeletedCompany();
        when(passwordEncoder.encode("ClaveSegura123!")).thenReturn("$2a$encoded");
        when(companyRepository.save(any(Company.class))).thenAnswer(invocation -> {
            Company c = invocation.getArgument(0);
            c.setId(10L);
            c.setUuid("club-uuid-new");
            return c;
        });
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setUuid("admin-uuid-new");
            if (u.getUserCompanyRoles() == null) {
                u.setUserCompanyRoles(new ArrayList<>());
            }
            return u;
        });
        when(userCompanyRoleRepository.save(any(UserCompanyRole.class))).thenAnswer(invocation -> invocation.getArgument(0));
        stubRoleProvisioning();
        clubCrmSincronizacionService.setExceptionToThrow(new CustomException(HttpStatus.BAD_GATEWAY,
                "Error al registrar el cliente en el CRM", "test"));

        CustomException ex = assertThrows(CustomException.class,
                () -> sysadminClubService.registerClub(validRequest()));

        assertEquals(HttpStatus.BAD_GATEWAY, ex.getStatus());
        assertFalse(clubCrmSincronizacionService.wasSuscribirCalled());
    }

    private CrmSuscripcionActivaDTO suscripcion(String estado, LocalDateTime fechaInicio) {
        CrmSuscripcionActivaDTO suscripcion = new CrmSuscripcionActivaDTO();
        suscripcion.setCodigoPlan("plan-estandar");
        suscripcion.setUuidSuscripcion("suscripcion-uuid");
        suscripcion.setEstadoSuscripcion(estado);
        suscripcion.setFechaInicio(fechaInicio);
        return suscripcion;
    }

    /** Stubs minimos para que un alta llegue hasta el final sin reventar. */
    private void stubAltaCompleta() {
        stubNoDeletedCompany();
        when(passwordEncoder.encode("ClaveSegura123!")).thenReturn("$2a$encoded");
        when(companyRepository.save(any(Company.class))).thenAnswer(invocation -> {
            Company c = invocation.getArgument(0);
            c.setId(10L);
            c.setUuid("club-uuid-new");
            return c;
        });
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setUuid("admin-uuid-new");
            if (u.getUserCompanyRoles() == null) {
                u.setUserCompanyRoles(new ArrayList<>());
            }
            return u;
        });
        when(userCompanyRoleRepository.save(any(UserCompanyRole.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        stubRoleProvisioning();
    }

    private void stubNoActiveCompany() {
        when(companyRepository.findFirstByCuitAndStatus(VALID_CUIT, 0)).thenReturn(null);
        when(companyRepository.findFirstByEmailInstitucionalIgnoreCaseAndStatus("info@catnorte.com", 0))
                .thenReturn(null);
    }

    private void stubNoDeletedCompany() {
        stubNoActiveCompany();
        when(companyRepository.findFirstByCuitAndStatus(VALID_CUIT, -1)).thenReturn(null);
        when(companyRepository.findFirstByEmailInstitucionalIgnoreCaseAndStatus("info@catnorte.com", -1))
                .thenReturn(null);
    }

    private void stubRoleProvisioning() {
        when(roleRepository.findFirstByNameAndCompany(anyString(), any(Company.class))).thenAnswer(invocation -> {
            Role role = new Role();
            role.setName(invocation.getArgument(0));
            role.setCompany(invocation.getArgument(1));
            role.setPermissions(new ArrayList<>());
            return role;
        });
        when(roleRepository.save(any(Role.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(permissionRepository.findFirstByName(anyString())).thenReturn(new Permission());
    }

    private void assertMercadoPagoDefault(Company company) {
        CompanyMercadoPago mercadoPago = company.getMercadoPago();
        assertNotNull(mercadoPago);
        assertEquals(company, mercadoPago.getCompany());
        assertEquals(Boolean.FALSE, mercadoPago.getHabilitado());
        assertNull(mercadoPago.getAccessToken());
        assertNull(mercadoPago.getPublicKey());
        assertTrue(!mercadoPago.isOperativo());
    }

    private ClubRegistrationRequestDTO validRequest() {
        return ClubProfileTestData.validRegistrationRequest();
    }
}
