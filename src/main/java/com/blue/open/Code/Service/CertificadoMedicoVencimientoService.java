package com.blue.open.Code.Service;

import com.blue.open.Code.Entity.Db.CertificadoMedico;
import com.blue.open.Code.Entity.Enums.CertificadoMedicoEstadoEnum;
import com.blue.open.Code.Repository.CertificadoMedicoRepository;
import com.blue.open.Core.Entity.DTO.DeleteArchivoRequest;
import com.blue.open.Core.Service.SM3Service;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@Slf4j
public class CertificadoMedicoVencimientoService {

    private static final int BATCH_SIZE = 50;

    @Autowired
    private CertificadoMedicoRepository certificadoMedicoRepository;

    @Autowired
    private SM3Service sm3Service;

    @Transactional
    public int marcarVencidos(LocalDate fechaCorte) {
        int totalMarcados = 0;
        Pageable pageable = PageRequest.of(0, BATCH_SIZE);

        List<CertificadoMedico> lote = certificadoMedicoRepository.findByEstadoAndFechaExpiracionLessThanEqual(
                CertificadoMedicoEstadoEnum.CONFIRMADO, fechaCorte, pageable);

        while (!lote.isEmpty()) {
            List<String> urls = lote.stream()
                    .map(CertificadoMedico::getUrlArchivo)
                    .filter(url -> url != null && !url.isBlank())
                    .toList();

            for (CertificadoMedico certificado : lote) {
                certificado.setEstado(CertificadoMedicoEstadoEnum.VENCIDO);
            }
            certificadoMedicoRepository.saveAll(lote);
            totalMarcados += lote.size();

            eliminarArchivosEnParalelo(urls);

            lote = certificadoMedicoRepository.findByEstadoAndFechaExpiracionLessThanEqual(
                    CertificadoMedicoEstadoEnum.CONFIRMADO, fechaCorte, pageable);
        }

        return totalMarcados;
    }

    private void eliminarArchivosEnParalelo(List<String> urls) {
        urls.parallelStream().forEach(url -> {
            try {
                DeleteArchivoRequest request = new DeleteArchivoRequest();
                request.setUrl(url);
                sm3Service.delete(request);
            } catch (Exception e) {
                log.warn("Error eliminando archivo de certificado médico vencido: {}", url, e);
            }
        });
    }
}
