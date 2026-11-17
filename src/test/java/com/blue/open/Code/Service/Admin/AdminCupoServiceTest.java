package com.blue.open.Code.Service.Admin;

import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.CompanySuscripcionCrm;
import com.blue.open.Code.Entity.Db.PlanCrm;
import com.blue.open.Code.Repository.CompanySuscripcionCrmRepository;
import com.blue.open.Code.Service.CrmPlanLimiteResolver;
import com.blue.open.Core.Entity.Db.UserCompanyRole;
import com.blue.open.Core.Exceptions.CustomException;
import com.blue.open.Core.Repository.UserCompanyRoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminCupoServiceTest {

    private static final int STATUS_ACTIVE = 0;

    @Mock
    private CompanySuscripcionCrmRepository companySuscripcionCrmRepository;

    @Mock
    private UserCompanyRoleRepository userCompanyRoleRepository;

    private AdminCupoService service;
    private Company company;

    @BeforeEach
    void setUp() {
        CrmPlanLimiteResolver limiteResolver = new CrmPlanLimiteResolver();
        ReflectionTestUtils.setField(limiteResolver, "companySuscripcionCrmRepository", companySuscripcionCrmRepository);

        service = new AdminCupoService();
        ReflectionTestUtils.setField(service, "crmPlanLimiteResolver", limiteResolver);
        ReflectionTestUtils.setField(service, "userCompanyRoleRepository", userCompanyRoleRepository);

        company = new Company();
        company.setId(10L);
        company.setUuid("club-uuid");
    }

    @Test
    void validarCupoDisponible_hayLugar_noLanza() {
        stubPlan("bsports-admin", 3);
        lenient().when(userCompanyRoleRepository.findByCompanyAndRoleName(any(), any()))
                .thenReturn(List.of(new UserCompanyRole()));

        assertDoesNotThrow(() -> service.validarCupoDisponible(company, 1));
    }

    @Test
    void validarCupoDisponible_superaElLimite_lanza() {
        stubPlan("bsports-admin", 1);
        when(userCompanyRoleRepository.findByCompanyAndRoleName(company, "ADMIN"))
                .thenReturn(List.of(new UserCompanyRole()));

        CustomException ex = assertThrows(CustomException.class,
                () -> service.validarCupoDisponible(company, 1));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    void validarCupoDisponible_sinCompanySuscripcionCrm_lanza() {
        when(companySuscripcionCrmRepository.findFirstByCompanyIdWithPlan(10L, STATUS_ACTIVE))
                .thenReturn(Optional.empty());

        CustomException ex = assertThrows(CustomException.class,
                () -> service.validarCupoDisponible(company, 1));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    void validarCupoDisponible_planSinProductoAdmin_lanza() {
        stubPlanConProductos("[{\"codigo\":\"bsports-socios\",\"nombre\":\"Socios\",\"unidades\":5}]");

        CustomException ex = assertThrows(CustomException.class,
                () -> service.validarCupoDisponible(company, 1));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    private void stubPlan(String codigoProducto, int unidades) {
        stubPlanConProductos("[{\"codigo\":\"" + codigoProducto + "\",\"nombre\":\"Admins\",\"unidades\":" + unidades + "}]");
    }

    private void stubPlanConProductos(String productosJson) {
        PlanCrm plan = PlanCrm.crear("plan-test");
        plan.actualizar(null, null, null, null, null, null, productosJson);
        CompanySuscripcionCrm suscripcion = CompanySuscripcionCrm.crear(company, plan, null, null, null, null);
        when(companySuscripcionCrmRepository.findFirstByCompanyIdWithPlan(10L, STATUS_ACTIVE))
                .thenReturn(Optional.of(suscripcion));
    }
}
