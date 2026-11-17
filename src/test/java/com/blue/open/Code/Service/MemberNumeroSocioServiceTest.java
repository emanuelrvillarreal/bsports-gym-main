package com.blue.open.Code.Service;

import com.blue.open.Code.Repository.MemberRepository;
import com.blue.open.Core.Exceptions.CustomException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;

import java.sql.SQLIntegrityConstraintViolationException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemberNumeroSocioServiceTest {

    private static final long CLUB = 7L;

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private MemberNumeroSocioService service;

    @Test
    void sinNumeroPedidoAsignaElSiguiente() {
        when(memberRepository.findMaxNumeroSocioByCompanyId(CLUB)).thenReturn(41L);
        assertEquals("42", service.resolver(CLUB, null, null));
    }

    @Test
    void clubSinSociosEmpiezaEnUno() {
        when(memberRepository.findMaxNumeroSocioByCompanyId(CLUB)).thenReturn(null);
        assertEquals("1", service.generateNext(CLUB));
    }

    @Test
    void numeroLibreSeRespeta() {
        when(memberRepository.existsByCompany_IdAndNumeroSocio(CLUB, "500")).thenReturn(false);
        assertEquals("500", service.resolver(CLUB, 500, null));
    }

    @Test
    void numeroOcupadoEnAltaDevuelveConflicto() {
        when(memberRepository.existsByCompany_IdAndNumeroSocio(CLUB, "12")).thenReturn(true);
        CustomException ex = assertThrows(CustomException.class, () -> service.resolver(CLUB, 12, null));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
    }

    @Test
    void alEditarSeExcluyeAlMismoSocio() {
        when(memberRepository.existsByCompany_IdAndNumeroSocioAndIdNot(CLUB, "12", 3L)).thenReturn(false);
        assertEquals("12", service.resolver(CLUB, 12, 3L));
        verify(memberRepository).existsByCompany_IdAndNumeroSocioAndIdNot(CLUB, "12", 3L);
    }

    @Test
    void alEditarNoPuedeTomarElNumeroDeOtro() {
        when(memberRepository.existsByCompany_IdAndNumeroSocioAndIdNot(CLUB, "12", 3L)).thenReturn(true);
        CustomException ex = assertThrows(CustomException.class, () -> service.resolver(CLUB, 12, 3L));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
    }

    @Test
    void numeroCeroOMenorSeRechaza() {
        CustomException ex = assertThrows(CustomException.class, () -> service.resolver(CLUB, 0, null));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    void choqueDeClaveUnicaSeTraduceAConflicto() {
        DataIntegrityViolationException ex = new DataIntegrityViolationException("dup",
                new SQLIntegrityConstraintViolationException("Duplicate entry '7-12' for key 'uk_members_numero_socio'"));
        RuntimeException traducida = service.traducir(ex, "12", CLUB);
        assertEquals(HttpStatus.CONFLICT, ((CustomException) traducida).getStatus());
    }

    @Test
    void otraViolacionNoSeDisfraza() {
        DataIntegrityViolationException ex = new DataIntegrityViolationException("otra",
                new SQLIntegrityConstraintViolationException("Duplicate entry for key 'uk_users_email'"));
        assertSame(ex, service.traducir(ex, "12", CLUB));
    }
}
