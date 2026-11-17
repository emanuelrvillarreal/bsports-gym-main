package com.blue.open.Code.Service;

import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.CompanySuscripcionCrm;
import com.blue.open.Code.Entity.Db.PlanCrm;
import com.blue.open.Code.Repository.CompanySuscripcionCrmRepository;
import com.blue.open.Code.Repository.MemberRepository;
import com.blue.open.Core.Exceptions.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemberCupoServiceTest {

    private static final int STATUS_ACTIVE = 0;

    @Mock
    private CompanySuscripcionCrmRepository companySuscripcionCrmRepository;

    @Mock
    private MemberRepository memberRepository;

    private MemberCupoService service;
    private Company company;

    @BeforeEach
    void setUp() {
        CrmPlanLimiteResolver limiteResolver = new CrmPlanLimiteResolver();
        ReflectionTestUtils.setField(limiteResolver, "companySuscripcionCrmRepository", companySuscripcionCrmRepository);

        service = new MemberCupoService();
        ReflectionTestUtils.setField(service, "crmPlanLimiteResolver", limiteResolver);
        ReflectionTestUtils.setField(service, "memberRepository", memberRepository);

        company = new Company();
        company.setId(10L);
        company.setUuid("club-uuid");
    }

    @Test
    void validarCupoDisponible_hayLugar_noLanza() {
        stubPlan("bsports-socios", 10);
        when(memberRepository.countByCompany_IdAndStatus(10L, MemberStatusConstants.STATUS_ACTIVE)).thenReturn(5L);

        assertDoesNotThrow(() -> service.validarCupoDisponible(company, 1));
    }

    @Test
    void validarCupoDisponible_justoEnElLimite_noLanza() {
        stubPlan("bsports-socios", 10);
        when(memberRepository.countByCompany_IdAndStatus(10L, MemberStatusConstants.STATUS_ACTIVE)).thenReturn(9L);

        assertDoesNotThrow(() -> service.validarCupoDisponible(company, 1));
    }

    @Test
    void validarCupoDisponible_superaElLimite_lanza() {
        stubPlan("bsports-socios", 10);
        when(memberRepository.countByCompany_IdAndStatus(10L, MemberStatusConstants.STATUS_ACTIVE)).thenReturn(10L);

        CustomException ex = assertThrows(CustomException.class,
                () -> service.validarCupoDisponible(company, 1));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    void validarCupoDisponible_bulkSuperaElLimite_lanza() {
        stubPlan("bsports-socios", 10);
        when(memberRepository.countByCompany_IdAndStatus(10L, MemberStatusConstants.STATUS_ACTIVE)).thenReturn(8L);

        // 8 actuales + 5 nuevos = 13 > 10
        CustomException ex = assertThrows(CustomException.class,
                () -> service.validarCupoDisponible(company, 5));
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
    void validarCupoDisponible_planSinProductoSocios_lanza() {
        stubPlanConProductos("[{\"codigo\":\"otro-producto\",\"nombre\":\"Otro\",\"unidades\":5}]");

        CustomException ex = assertThrows(CustomException.class,
                () -> service.validarCupoDisponible(company, 1));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    void validarCupoDisponible_productosJsonInvalido_lanza() {
        stubPlanConProductos("no-es-json-valido");

        CustomException ex = assertThrows(CustomException.class,
                () -> service.validarCupoDisponible(company, 1));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    void validarCupoDisponible_productosJsonVacio_lanza() {
        stubPlanConProductos(null);

        CustomException ex = assertThrows(CustomException.class,
                () -> service.validarCupoDisponible(company, 1));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    void validarCupoDisponible_matchPorCodigoIgnoraCaseYEspacios() {
        stubPlanConProductos("[{\"codigo\":\" BSPORTS-SOCIOS \",\"nombre\":\"Socios\",\"unidades\":3}]");
        when(memberRepository.countByCompany_IdAndStatus(10L, MemberStatusConstants.STATUS_ACTIVE)).thenReturn(2L);

        assertDoesNotThrow(() -> service.validarCupoDisponible(company, 1));
    }

    private void stubPlan(String codigoProducto, int unidades) {
        stubPlanConProductos("[{\"codigo\":\"" + codigoProducto + "\",\"nombre\":\"Socios\",\"unidades\":" + unidades + "}]");
    }

    private void stubPlanConProductos(String productosJson) {
        PlanCrm plan = PlanCrm.crear("plan-test");
        plan.actualizar(null, null, null, null, null, null, productosJson);
        CompanySuscripcionCrm suscripcion = CompanySuscripcionCrm.crear(company, plan, null, null, null, null);
        when(companySuscripcionCrmRepository.findFirstByCompanyIdWithPlan(10L, STATUS_ACTIVE))
                .thenReturn(Optional.of(suscripcion));
    }
}
