package com.blue.open.Code.Service.Recepcionista;

import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.Recepcionista;
import com.blue.open.Code.Entity.Enums.RecepcionistaEstadoEnum;
import com.blue.open.Code.Repository.RecepcionistaRepository;
import com.blue.open.Code.Service.support.StubAuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecepcionistaBajaServiceTest {

    @Mock
    private RecepcionistaRepository recepcionistaRepository;

    private StubAuthService stubAuthService;
    private RecepcionistaBajaService service;

    @BeforeEach
    void setUp() {
        stubAuthService = new StubAuthService();
        service = new RecepcionistaBajaService();
        ReflectionTestUtils.setField(service, "authService", stubAuthService);
        ReflectionTestUtils.setField(service, "recepcionistaRepository", recepcionistaRepository);

        Company company = new Company();
        company.setId(1L);
        stubAuthService.setCompany(company);
    }

    @Test
    void bajaLogica_marcaEliminado() {
        Recepcionista recepcionista = new Recepcionista();
        recepcionista.setId(5L);
        recepcionista.setUuid("rec-uuid");
        recepcionista.setStatus(0);
        recepcionista.setEstado(RecepcionistaEstadoEnum.ACTIVO);

        when(recepcionistaRepository.findByUuidAndCompanyId("rec-uuid", 1L)).thenReturn(Optional.of(recepcionista));

        service.bajaLogica("rec-uuid");

        assertEquals(-1, recepcionista.getStatus());
        assertEquals(RecepcionistaEstadoEnum.INACTIVO, recepcionista.getEstado());
        verify(recepcionistaRepository).save(recepcionista);
    }

    @Test
    void bajaLogica_idempotenteSiYaEliminado() {
        Recepcionista recepcionista = new Recepcionista();
        recepcionista.setId(5L);
        recepcionista.setStatus(-1);

        when(recepcionistaRepository.findByUuidAndCompanyId("rec-uuid", 1L)).thenReturn(Optional.of(recepcionista));

        service.bajaLogica("rec-uuid");

        verify(recepcionistaRepository, never()).save(recepcionista);
    }
}
