package com.blue.open.Code.Service.Categorias;

import com.blue.open.Code.Entity.DTO.CategoriaAltaCommand;
import com.blue.open.Code.Entity.DTO.CategoriaDTO;
import com.blue.open.Code.Entity.Db.Categoria;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Mapper.CategoriaMapper;
import com.blue.open.Code.Repository.CategoriaRepository;
import com.blue.open.Code.Service.support.StubAuthService;
import com.blue.open.Core.Exceptions.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoriaAltaServiceTest {

    @Mock
    private CategoriaRepository categoriaRepository;

    private StubAuthService stubAuthService;
    private CategoriaMapper categoriaMapper;
    private CategoriaAltaService service;

    @BeforeEach
    void setUp() {
        stubAuthService = new StubAuthService();
        categoriaMapper = new CategoriaMapper();
        service = new CategoriaAltaService();
        ReflectionTestUtils.setField(service, "authService", stubAuthService);
        ReflectionTestUtils.setField(service, "categoriaRepository", categoriaRepository);
        ReflectionTestUtils.setField(service, "categoriaMapper", categoriaMapper);

        Company company = new Company();
        company.setId(1L);
        stubAuthService.setCompany(company);
    }

    @Test
    void alta_persisteCategoriaConRangoEdad() {
        CategoriaAltaCommand command = new CategoriaAltaCommand();
        command.setNombre(" Sub-23 ");
        command.setDescripcion("  Categoría juvenil  ");
        command.setEdadMinima(23);
        command.setEdadMaxima(25);

        when(categoriaRepository.existsNombreActivoEnCompany(1L, "Sub-23", null)).thenReturn(false);
        when(categoriaRepository.save(any(Categoria.class))).thenAnswer(inv -> {
            Categoria c = inv.getArgument(0);
            c.setId(10L);
            c.setUuid("cat-uuid");
            return c;
        });

        CategoriaDTO dto = service.alta(command);

        ArgumentCaptor<Categoria> captor = ArgumentCaptor.forClass(Categoria.class);
        verify(categoriaRepository).save(captor.capture());
        Categoria saved = captor.getValue();
        assertEquals("Sub-23", saved.getNombre());
        assertEquals("Categoría juvenil", saved.getDescripcion());
        assertEquals(23, saved.getEdadMinima());
        assertEquals(25, saved.getEdadMaxima());
        assertEquals("cat-uuid", dto.getCategoriaUuid());
        assertEquals(23, dto.getEdadMinima());
    }

    @Test
    void alta_nombreDuplicado_lanzaConflict() {
        CategoriaAltaCommand command = new CategoriaAltaCommand();
        command.setNombre("Sub-23");
        command.setEdadMinima(23);
        command.setEdadMaxima(25);

        when(categoriaRepository.existsNombreActivoEnCompany(1L, "Sub-23", null)).thenReturn(true);

        CustomException ex = assertThrows(CustomException.class, () -> service.alta(command));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        verify(categoriaRepository, never()).save(any());
    }
}
