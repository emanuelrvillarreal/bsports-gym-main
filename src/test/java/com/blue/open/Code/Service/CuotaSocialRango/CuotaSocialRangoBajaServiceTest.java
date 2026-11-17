package com.blue.open.Code.Service.CuotaSocialRango;

import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.CuotaSocialRango;
import com.blue.open.Code.Event.CuotaSocialRangoBajaEvent;
import com.blue.open.Code.Repository.CuotaSocialRangoRepository;
import com.blue.open.Code.Service.support.StubAuthService;
import com.blue.open.Core.Exceptions.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CuotaSocialRangoBajaServiceTest {

    private static final Long COMPANY_ID = 1L;

    @Mock
    private CuotaSocialRangoRepository cuotaSocialRangoRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private CuotaSocialRangoBajaService service;
    private Company company;

    @BeforeEach
    void setUp() {
        StubAuthService stubAuthService = new StubAuthService();
        company = new Company();
        company.setId(COMPANY_ID);
        stubAuthService.setCompany(company);

        service = new CuotaSocialRangoBajaService();
        ReflectionTestUtils.setField(service, "authService", stubAuthService);
        ReflectionTestUtils.setField(service, "cuotaSocialRangoRepository", cuotaSocialRangoRepository);
        ReflectionTestUtils.setField(service, "eventPublisher", eventPublisher);
    }

    @Test
    void bajaLogica_rangoNormal_loMarcaEliminadoYPublicaElEvento() {
        CuotaSocialRango rango = new CuotaSocialRango();
        rango.setId(5L);
        rango.setUuid("rango-uuid");
        rango.setEsDefault(false);
        when(cuotaSocialRangoRepository.findByUuidAndCompanyIdAndStatusActive("rango-uuid", COMPANY_ID))
                .thenReturn(Optional.of(rango));

        service.bajaLogica("rango-uuid");

        assertEquals(-1, rango.getStatus());
        verify(cuotaSocialRangoRepository).save(rango);

        ArgumentCaptor<CuotaSocialRangoBajaEvent> captor = ArgumentCaptor.forClass(CuotaSocialRangoBajaEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertEquals(COMPANY_ID, captor.getValue().getCompanyId());
        assertEquals(5L, captor.getValue().getCuotaSocialRangoId());
        assertEquals("rango-uuid", captor.getValue().getCuotaSocialRangoUuid());
    }

    @Test
    void bajaLogica_rangoDefault_rechaza() {
        CuotaSocialRango rango = new CuotaSocialRango();
        rango.setId(5L);
        rango.setUuid("rango-default-uuid");
        rango.setEsDefault(true);
        when(cuotaSocialRangoRepository.findByUuidAndCompanyIdAndStatusActive("rango-default-uuid", COMPANY_ID))
                .thenReturn(Optional.of(rango));

        CustomException ex = assertThrows(CustomException.class, () -> service.bajaLogica("rango-default-uuid"));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        verify(cuotaSocialRangoRepository, never()).save(rango);
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void bajaLogica_noExiste_404() {
        when(cuotaSocialRangoRepository.findByUuidAndCompanyIdAndStatusActive("missing", COMPANY_ID))
                .thenReturn(Optional.empty());

        CustomException ex = assertThrows(CustomException.class, () -> service.bajaLogica("missing"));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }
}
