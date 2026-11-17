package com.blue.open.Code.Service.Categorias;

import com.blue.open.Code.Entity.Db.Categoria;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Event.CategoriaBajaEvent;
import com.blue.open.Code.Repository.CategoriaRepository;
import com.blue.open.Code.Service.support.StubAuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoriaBajaServiceTest {

    @Mock
    private CategoriaRepository categoriaRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private StubAuthService stubAuthService;
    private CategoriaBajaService service;

    @BeforeEach
    void setUp() {
        stubAuthService = new StubAuthService();
        service = new CategoriaBajaService();
        ReflectionTestUtils.setField(service, "authService", stubAuthService);
        ReflectionTestUtils.setField(service, "categoriaRepository", categoriaRepository);
        ReflectionTestUtils.setField(service, "eventPublisher", eventPublisher);

        Company company = new Company();
        company.setId(1L);
        stubAuthService.setCompany(company);
    }

    @Test
    void bajaLogica_marcaStatusMenosUno() {
        Categoria categoria = new Categoria();
        categoria.setId(5L);
        categoria.setUuid("cat-uuid");
        categoria.setStatus(0);

        when(categoriaRepository.findByUuidAndCompanyIdAndStatusActive("cat-uuid", 1L))
                .thenReturn(Optional.of(categoria));
        when(categoriaRepository.save(categoria)).thenReturn(categoria);

        service.bajaLogica("cat-uuid");

        assertEquals(-1, categoria.getStatus());
        verify(categoriaRepository).save(categoria);

        ArgumentCaptor<CategoriaBajaEvent> eventCaptor = ArgumentCaptor.forClass(CategoriaBajaEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertEquals(1L, eventCaptor.getValue().getCompanyId());
        assertEquals(5L, eventCaptor.getValue().getCategoriaId());
        assertEquals("cat-uuid", eventCaptor.getValue().getCategoriaUuid());
    }
}
