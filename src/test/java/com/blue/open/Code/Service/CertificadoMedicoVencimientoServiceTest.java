package com.blue.open.Code.Service;

import com.blue.open.Code.Entity.Db.CertificadoMedico;
import com.blue.open.Code.Entity.Enums.CertificadoMedicoEstadoEnum;
import com.blue.open.Code.Repository.CertificadoMedicoRepository;
import com.blue.open.Core.Entity.DTO.DeleteArchivoRequest;
import com.blue.open.Core.Service.SM3Service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CertificadoMedicoVencimientoServiceTest {

    @Mock
    private CertificadoMedicoRepository certificadoMedicoRepository;

    @Mock
    private SM3Service sm3Service;

    private CertificadoMedicoVencimientoService service;

    @BeforeEach
    void setUp() {
        service = new CertificadoMedicoVencimientoService();
        ReflectionTestUtils.setField(service, "certificadoMedicoRepository", certificadoMedicoRepository);
        ReflectionTestUtils.setField(service, "sm3Service", sm3Service);
    }

    private CertificadoMedico certificado(String urlArchivo) {
        CertificadoMedico certificado = new CertificadoMedico();
        certificado.setEstado(CertificadoMedicoEstadoEnum.CONFIRMADO);
        certificado.setUrlArchivo(urlArchivo);
        return certificado;
    }

    // Dado más de un lote pendiente (BATCH_SIZE), cuando marcarVencidos, entonces recorre
    // todas las páginas hasta que la query deja de devolver resultados (los certificados
    // recién marcados VENCIDO dejan de matchear el filtro estado = CONFIRMADO).
    @Test
    void marcarVencidos_recorreVariosLotesHastaVaciar() {
        LocalDate fechaCorte = LocalDate.of(2026, 1, 1);

        CertificadoMedico c1 = certificado("url1");
        CertificadoMedico c2 = certificado("url2");
        CertificadoMedico c3 = certificado("url3");

        when(certificadoMedicoRepository.findByEstadoAndFechaExpiracionLessThanEqual(
                eq(CertificadoMedicoEstadoEnum.CONFIRMADO), eq(fechaCorte), any(Pageable.class)))
                .thenReturn(List.of(c1, c2))
                .thenReturn(List.of(c3))
                .thenReturn(List.of());
        when(certificadoMedicoRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        int totalMarcados = service.marcarVencidos(fechaCorte);

        assertEquals(3, totalMarcados);
        assertEquals(CertificadoMedicoEstadoEnum.VENCIDO, c1.getEstado());
        assertEquals(CertificadoMedicoEstadoEnum.VENCIDO, c2.getEstado());
        assertEquals(CertificadoMedicoEstadoEnum.VENCIDO, c3.getEstado());

        verify(certificadoMedicoRepository, times(3)).findByEstadoAndFechaExpiracionLessThanEqual(
                eq(CertificadoMedicoEstadoEnum.CONFIRMADO), eq(fechaCorte), any(Pageable.class));
        verify(sm3Service, times(3)).delete(any(DeleteArchivoRequest.class));
    }

    // Dado que no hay certificados vencidos pendientes, cuando marcarVencidos, entonces
    // no hace ningún update ni llama a SM3, y devuelve 0 sin loopear.
    @Test
    void marcarVencidos_sinCertificadosPendientes_noHaceNada() {
        LocalDate fechaCorte = LocalDate.of(2026, 1, 1);

        when(certificadoMedicoRepository.findByEstadoAndFechaExpiracionLessThanEqual(
                eq(CertificadoMedicoEstadoEnum.CONFIRMADO), eq(fechaCorte), any(Pageable.class)))
                .thenReturn(List.of());

        int totalMarcados = service.marcarVencidos(fechaCorte);

        assertEquals(0, totalMarcados);
        verify(certificadoMedicoRepository, times(1)).findByEstadoAndFechaExpiracionLessThanEqual(
                eq(CertificadoMedicoEstadoEnum.CONFIRMADO), eq(fechaCorte), any(Pageable.class));
        verify(sm3Service, times(0)).delete(any(DeleteArchivoRequest.class));
    }

    // Certificados con urlArchivo nula o vacía no deben disparar una eliminación en SM3.
    @Test
    void marcarVencidos_urlArchivoVacia_noEliminaEnSm3() {
        LocalDate fechaCorte = LocalDate.of(2026, 1, 1);

        CertificadoMedico sinUrl = certificado(null);
        CertificadoMedico conUrl = certificado("url-valida");

        when(certificadoMedicoRepository.findByEstadoAndFechaExpiracionLessThanEqual(
                eq(CertificadoMedicoEstadoEnum.CONFIRMADO), eq(fechaCorte), any(Pageable.class)))
                .thenReturn(List.of(sinUrl, conUrl))
                .thenReturn(List.of());
        when(certificadoMedicoRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        int totalMarcados = service.marcarVencidos(fechaCorte);

        assertEquals(2, totalMarcados);
        verify(sm3Service, times(1)).delete(any(DeleteArchivoRequest.class));
    }
}
