package com.blue.open.Code.Service.Categorias;

import com.blue.open.Code.Repository.ActividadRepository;
import com.blue.open.Code.Repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoriaBajaDesasignacionServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private ActividadRepository actividadRepository;

    private CategoriaBajaDesasignacionService service;

    @BeforeEach
    void setUp() {
        service = new CategoriaBajaDesasignacionService();
        ReflectionTestUtils.setField(service, "memberRepository", memberRepository);
        ReflectionTestUtils.setField(service, "actividadRepository", actividadRepository);
    }

    @Test
    void desasignarPorBaja_limpiaSociosYActividades() {
        when(memberRepository.clearCategoriaByCategoriaIdAndCompanyId(5L, 1L)).thenReturn(2);
        when(actividadRepository.clearCategoriaByCategoriaIdAndCompanyId(5L, 1L)).thenReturn(3);

        service.desasignarPorBaja(1L, 5L, "cat-uuid");

        verify(memberRepository).clearCategoriaByCategoriaIdAndCompanyId(5L, 1L);
        verify(actividadRepository).clearCategoriaByCategoriaIdAndCompanyId(5L, 1L);
    }
}
