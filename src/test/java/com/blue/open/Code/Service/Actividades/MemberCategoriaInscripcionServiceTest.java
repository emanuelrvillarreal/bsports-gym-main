package com.blue.open.Code.Service.Actividades;

import com.blue.open.Code.Entity.Db.Actividad;
import com.blue.open.Code.Entity.Db.Categoria;
import com.blue.open.Code.Entity.Db.Member;
import com.blue.open.Code.Repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemberCategoriaInscripcionServiceTest {

    @Mock
    private MemberRepository memberRepository;

    private MemberCategoriaInscripcionService service;

    private Member member;
    private Actividad actividad;
    private Categoria categoriaActividad;
    private Categoria otraCategoria;

    @BeforeEach
    void setUp() {
        service = new MemberCategoriaInscripcionService();
        ReflectionTestUtils.setField(service, "memberRepository", memberRepository);

        categoriaActividad = new Categoria();
        categoriaActividad.setId(10L);
        categoriaActividad.setUuid("cat-actividad");

        otraCategoria = new Categoria();
        otraCategoria.setId(20L);
        otraCategoria.setUuid("cat-otra");

        actividad = new Actividad();
        actividad.setId(1L);
        actividad.getCategorias().add(categoriaActividad);

        member = new Member();
        member.setId(100L);
    }

    @Test
    void asignar_conCategoria_asignaYPersiste() {
        when(memberRepository.save(member)).thenReturn(member);

        service.asignar(member, categoriaActividad);

        verify(memberRepository).save(member);
        assertEquals(categoriaActividad, member.getCategoria());
    }

    @Test
    void asignar_sinCategoria_noPersiste() {
        service.asignar(member, null);

        verify(memberRepository, never()).save(member);
    }

    @Test
    void desasignarSiCorresponde_categoriasCoinciden_limpiaYPersiste() {
        member.setCategoria(categoriaActividad);
        when(memberRepository.save(member)).thenReturn(member);

        service.desasignarSiCorresponde(member, actividad);

        assertNull(member.getCategoria());
        verify(memberRepository).save(member);
    }

    @Test
    void desasignarSiCorresponde_categoriasDistintas_noModifica() {
        member.setCategoria(otraCategoria);

        service.desasignarSiCorresponde(member, actividad);

        verify(memberRepository, never()).save(member);
        assertEquals(otraCategoria, member.getCategoria());
    }

    @Test
    void desasignarSiCorresponde_actividadSinCategoria_noModifica() {
        actividad.getCategorias().clear();
        member.setCategoria(categoriaActividad);

        service.desasignarSiCorresponde(member, actividad);

        verify(memberRepository, never()).save(member);
        assertEquals(categoriaActividad, member.getCategoria());
    }

    @Test
    void limpiar_conCategoria_limpiaYPersiste() {
        member.setCategoria(categoriaActividad);
        when(memberRepository.save(member)).thenReturn(member);

        service.limpiar(member);

        assertNull(member.getCategoria());
        verify(memberRepository).save(member);
    }

    @Test
    void limpiar_sinCategoria_noPersiste() {
        service.limpiar(member);

        verify(memberRepository, never()).save(member);
    }
}
