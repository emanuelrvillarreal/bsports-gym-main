package com.blue.open.Code.Service.Actividades;

import com.blue.open.Code.Entity.Db.Categoria;
import com.blue.open.Code.Repository.CategoriaRepository;
import com.blue.open.Core.Exceptions.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActividadCategoriaResolverTest {

    @Mock
    private CategoriaRepository categoriaRepository;

    private ActividadCategoriaResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new ActividadCategoriaResolver();
        ReflectionTestUtils.setField(resolver, "categoriaRepository", categoriaRepository);
    }

    @Test
    void resolve_uuidsValidos_retornaCategoriasEnOrdenDelRequest() {
        Categoria sub23 = buildCategoria(10L, "cat-sub23");
        Categoria sub25 = buildCategoria(11L, "cat-sub25");

        when(categoriaRepository.findByUuidInAndCompanyIdAndStatusActive(Set.of("cat-sub25", "cat-sub23"), 1L))
                .thenReturn(List.of(sub23, sub25));

        Set<Categoria> result = resolver.resolve(1L, List.of("cat-sub25", " cat-sub23 ", "cat-sub25"));

        assertEquals(List.of(sub25, sub23), List.copyOf(result));
    }

    @Test
    void resolve_algunUuidInexistente_lanzaNotFound() {
        Categoria sub23 = buildCategoria(10L, "cat-sub23");

        when(categoriaRepository.findByUuidInAndCompanyIdAndStatusActive(Set.of("cat-sub23", "missing"), 1L))
                .thenReturn(List.of(sub23));

        CustomException ex = assertThrows(CustomException.class,
                () -> resolver.resolve(1L, List.of("cat-sub23", "missing")));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    @Test
    void resolve_listaNullOVacia_retornaVacioSinConsultar() {
        assertTrue(resolver.resolve(1L, null).isEmpty());
        assertTrue(resolver.resolve(1L, List.of()).isEmpty());
        verify(categoriaRepository, never()).findByUuidInAndCompanyIdAndStatusActive(anyCollection(), anyLong());
    }

    private Categoria buildCategoria(Long id, String uuid) {
        Categoria categoria = new Categoria();
        categoria.setId(id);
        categoria.setUuid(uuid);
        return categoria;
    }
}
