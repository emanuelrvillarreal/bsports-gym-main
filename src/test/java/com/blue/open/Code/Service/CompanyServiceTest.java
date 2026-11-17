package com.blue.open.Code.Service;

import com.blue.open.Code.Entity.DTO.ClubProfileUpdateRequestDTO;
import com.blue.open.Code.Entity.DTO.CompanyDTO;
import com.blue.open.Code.Entity.DTO.CompanyPaginateRequestDTO;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.CompanySuscripcionCrm;
import com.blue.open.Code.Entity.Db.Factura;
import com.blue.open.Code.Entity.Db.PlanCrm;
import com.blue.open.Code.Entity.Enums.FacturaEstadoEnum;
import com.blue.open.Code.Mapper.CompanyMapper;
import com.blue.open.Code.Repository.CompanyRepository;
import com.blue.open.Code.Repository.CompanySuscripcionCrmRepository;
import com.blue.open.Code.Repository.FacturaRepository;
import com.blue.open.Core.Exceptions.CustomException;
import com.blue.open.Core.Service.Auth.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompanyServiceTest {

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private CompanySuscripcionCrmRepository companySuscripcionCrmRepository;

    @Mock
    private FacturaRepository facturaRepository;

    private StubAuthService stubAuthService;
    private CompanyService companyService;
    private CompanyMapper companyMapper;

    @BeforeEach
    void setUp() {
        companyMapper = new CompanyMapper();
        companyService = new CompanyService();
        stubAuthService = new StubAuthService();
        ReflectionTestUtils.setField(companyService, "companyRepository", companyRepository);
        ReflectionTestUtils.setField(companyService, "authService", stubAuthService);
        ReflectionTestUtils.setField(companyService, "cuitValidatorService", new CuitValidatorService());
        ReflectionTestUtils.setField(companyService, "companyMapper", companyMapper);
        ReflectionTestUtils.setField(companyService, "mapper", companyMapper);
        ReflectionTestUtils.setField(companyService, "companySuscripcionCrmRepository", companySuscripcionCrmRepository);
        ReflectionTestUtils.setField(companyService, "facturaRepository", facturaRepository);
    }

    @Test
    void updateClubProfile_updatesFieldsAndPreservesLogoUrl() {
        Company company = currentCompany();
        stubAuthService.setCurrentCompany(company);
        when(companyRepository.findFirstByCuitAndStatus(CuitValidatorServiceTest.VALID_CUIT_FOR_TESTS, 0))
                .thenReturn(company);
        when(companyRepository.findFirstByEmailInstitucionalIgnoreCaseAndStatus("info@catnorte.com", 0))
                .thenReturn(company);
        when(companyRepository.save(any(Company.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CompanyDTO result = companyService.updateClubProfile(validUpdateRequest());

        assertNotNull(result);
        assertEquals("Club Atlético Norte", result.getNombreComercial());
        assertEquals("https://cdn.example/logo.png", result.getLogoUrl());
        assertEquals("1425", result.getDomicilio().getCodigoPostal());
        assertEquals("club-atletico-norte", result.getPath());

        ArgumentCaptor<Company> captor = ArgumentCaptor.forClass(Company.class);
        verify(companyRepository).save(captor.capture());
        assertEquals("Club Atlético Norte", captor.getValue().getNombreComercial());
        assertEquals(CuitValidatorServiceTest.VALID_CUIT_FOR_TESTS, captor.getValue().getCuit());
        assertEquals("1425", captor.getValue().getDomicilio().getCodigoPostal());
        assertEquals("facturas@catnorte.com", captor.getValue().getEmailFacturaElectronica());
        assertEquals("https://cdn.example/logo.png", captor.getValue().getLogoUrl());
        assertEquals(true, captor.getValue().getAdminCreado());
        assertEquals("club-atletico-norte", captor.getValue().getPath());
    }

    @Test
    void updateClubProfile_changesPathToNewValue() {
        Company company = currentCompany();
        company.setPath("path-anterior");
        stubAuthService.setCurrentCompany(company);
        when(companyRepository.findFirstByCuitAndStatus(CuitValidatorServiceTest.VALID_CUIT_FOR_TESTS, 0))
                .thenReturn(company);
        when(companyRepository.findFirstByEmailInstitucionalIgnoreCaseAndStatus("info@catnorte.com", 0))
                .thenReturn(company);
        when(companyRepository.save(any(Company.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ClubProfileUpdateRequestDTO request = validUpdateRequest();
        request.setPath("path-nuevo");

        CompanyDTO result = companyService.updateClubProfile(request);

        assertNotEquals("path-anterior", result.getPath());
        assertEquals("path-nuevo", result.getPath());

        ArgumentCaptor<Company> captor = ArgumentCaptor.forClass(Company.class);
        verify(companyRepository).save(captor.capture());
        assertNotEquals("path-anterior", captor.getValue().getPath());
        assertEquals("path-nuevo", captor.getValue().getPath());
    }

    @Test
    void updateClubProfile_rejectsDuplicateCuitFromAnotherClub() {
        Company company = currentCompany();
        Company other = new Company();
        other.setId(99L);
        other.setUuid("other-uuid");
        stubAuthService.setCurrentCompany(company);
        when(companyRepository.findFirstByCuitAndStatus(CuitValidatorServiceTest.VALID_CUIT_FOR_TESTS, 0))
                .thenReturn(other);

        CustomException ex = assertThrows(CustomException.class,
                () -> companyService.updateClubProfile(validUpdateRequest()));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("CUIT ya registrado", ex.getUserMessage());
    }

    @Test
    void updateClubProfile_rejectsDuplicateEmailFromAnotherClub() {
        Company company = currentCompany();
        Company other = new Company();
        other.setId(99L);
        other.setUuid("other-uuid");
        stubAuthService.setCurrentCompany(company);
        when(companyRepository.findFirstByCuitAndStatus(CuitValidatorServiceTest.VALID_CUIT_FOR_TESTS, 0))
                .thenReturn(null);
        when(companyRepository.findFirstByEmailInstitucionalIgnoreCaseAndStatus("info@catnorte.com", 0))
                .thenReturn(other);

        CustomException ex = assertThrows(CustomException.class,
                () -> companyService.updateClubProfile(validUpdateRequest()));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("Email ya registrado", ex.getUserMessage());
    }

    // Dado un club con suscripcion vigente al plan del CRM y con facturas, cuando paginar,
    // entonces el DTO trae la suscripcion (sin uuidFacturaInicialCrm, que no se expone) y los
    // datos de la ultima factura por fechaGeneracion.
    @Test
    void paginar_incluyeSuscripcionCrmYUltimaFactura() {
        Company company = currentCompany();
        Page<Company> companyPage = new PageImpl<>(List.of(company), PageRequest.of(0, 10), 1);
        when(companyRepository.paginate(null, PageRequest.of(0, 10))).thenReturn(companyPage);

        PlanCrm plan = PlanCrm.crear("plan-estandar");
        plan.actualizar("Plan Estándar", "desc", BigDecimal.TEN, true, BigDecimal.ONE, "mensual", null);
        CompanySuscripcionCrm suscripcion = CompanySuscripcionCrm.crear(company, plan,
                "uuid-suscripcion", "uuid-factura-inicial", "ACTIVA", LocalDateTime.of(2026, 7, 15, 0, 0));
        when(companySuscripcionCrmRepository.findByCompanyIdInWithPlan(List.of(1L), 0))
                .thenReturn(List.of(suscripcion));

        Factura ultimaFactura = Factura.crear(company, null, "20123456789", "uuid-factura-crm", List.of("plan-estandar"), List.of(), null);
        when(facturaRepository.findUltimaFacturaIdPorCompanyIds(List.of(1L), 0)).thenReturn(List.of(100L));
        when(facturaRepository.findByIdInWithCompany(List.of(100L))).thenReturn(List.of(ultimaFactura));

        Page<CompanyDTO> result = companyService.paginar(new CompanyPaginateRequestDTO());

        assertEquals(1, result.getContent().size());
        CompanyDTO dto = result.getContent().get(0);
        assertNotNull(dto.getSuscripcionCrm());
        assertEquals("plan-estandar", dto.getSuscripcionCrm().getCodigoPlan());
        assertEquals("Plan Estándar", dto.getSuscripcionCrm().getNombrePlan());
        assertEquals("uuid-suscripcion", dto.getSuscripcionCrm().getUuidSuscripcionCrm());
        assertEquals("ACTIVA", dto.getSuscripcionCrm().getEstado());
        assertEquals(LocalDate.of(2026, 7, 15), dto.getSuscripcionCrm().getFechaProximaFacturacion());

        assertEquals(FacturaEstadoEnum.PENDIENTE, dto.getUltimaFacturaEstado());
        assertEquals(ultimaFactura.getFechaGeneracion(), dto.getUltimaFacturaFechaGeneracion());
        assertEquals(ultimaFactura.getFechaVencimiento(), dto.getUltimaFacturaFechaVencimiento());
    }

    // Dado un club sin suscripcion registrada y sin facturas, cuando paginar, entonces los 4
    // campos nuevos quedan null en vez de romper.
    @Test
    void paginar_sinSuscripcionNiFactura_dejaCamposNull() {
        Company company = currentCompany();
        Page<Company> companyPage = new PageImpl<>(List.of(company), PageRequest.of(0, 10), 1);
        when(companyRepository.paginate(null, PageRequest.of(0, 10))).thenReturn(companyPage);
        when(companySuscripcionCrmRepository.findByCompanyIdInWithPlan(List.of(1L), 0)).thenReturn(List.of());
        when(facturaRepository.findUltimaFacturaIdPorCompanyIds(List.of(1L), 0)).thenReturn(List.of());

        Page<CompanyDTO> result = companyService.paginar(new CompanyPaginateRequestDTO());

        CompanyDTO dto = result.getContent().get(0);
        assertNull(dto.getSuscripcionCrm());
        assertNull(dto.getUltimaFacturaEstado());
        assertNull(dto.getUltimaFacturaFechaGeneracion());
        assertNull(dto.getUltimaFacturaFechaVencimiento());
    }

    private Company currentCompany() {
        Company company = new Company();
        company.setId(1L);
        company.setUuid("club-uuid");
        company.setLogoUrl("https://cdn.example/logo.png");
        company.setAdminCreado(true);
        company.setStatus(0);
        return company;
    }

    private ClubProfileUpdateRequestDTO validUpdateRequest() {
        return ClubProfileTestData.validUpdateRequest();
    }

    private static class StubAuthService extends AuthService {

        private Company currentCompany;

        void setCurrentCompany(Company currentCompany) {
            this.currentCompany = currentCompany;
        }

        @Override
        public Company getCurrentCompanyOrThrow() {
            if (currentCompany == null) {
                throw new CustomException(HttpStatus.BAD_REQUEST, "Company requerida", "Sin company de prueba");
            }
            return currentCompany;
        }
    }
}
