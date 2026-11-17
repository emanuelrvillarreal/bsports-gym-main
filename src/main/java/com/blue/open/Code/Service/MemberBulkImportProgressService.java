package com.blue.open.Code.Service;

import com.blue.open.Code.Entity.DTO.MemberBulkImportProgressDTO;
import com.blue.open.Core.Exceptions.CustomException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Registro en memoria de las importaciones masivas en curso y canal SSE para
 * publicar el avance (filas procesadas sobre filas totales) fila a fila.
 */
@Service
public class MemberBulkImportProgressService {

    private static final String EVENT_PROGRESS = "progress";
    private static final String EVENT_COMPLETED = "completed";
    private static final String EVENT_ERROR = "error";

    private static final long EMITTER_TIMEOUT_MS = 30 * 60 * 1000L;
    private static final long RETENTION_MS = 15 * 60 * 1000L;

    private final Map<String, ImportJob> jobs = new ConcurrentHashMap<>();

    /**
     * Registra una importación y devuelve el id con el que el cliente se suscribe
     * al progreso.
     */
    public String iniciar(int filasTotales) {
        purgarFinalizados();
        String importId = UUID.randomUUID().toString();
        jobs.put(importId, new ImportJob(filasTotales));
        return importId;
    }

    /**
     * Abre el stream SSE de una importación. Envía de inmediato el avance actual
     * para que un cliente que se conecta tarde no pierda el estado.
     */
    public SseEmitter suscribir(String importId) {
        purgarFinalizados();
        ImportJob job = jobs.get(importId);
        if (job == null) {
            throw new CustomException(HttpStatus.NOT_FOUND, "Importación no encontrada",
                    "MemberBulkImportProgressService | importId: " + importId);
        }

        SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT_MS);
        emitter.onCompletion(() -> job.emitters.remove(emitter));
        emitter.onTimeout(() -> {
            job.emitters.remove(emitter);
            emitter.complete();
        });
        emitter.onError(ex -> job.emitters.remove(emitter));

        synchronized (job) {
            String evento = job.finalizado ? job.eventoFinal : EVENT_PROGRESS;
            if (enviar(emitter, evento, job.progreso())) {
                if (job.finalizado) {
                    emitter.complete();
                } else {
                    job.emitters.add(emitter);
                }
            }
        }
        return emitter;
    }

    /** Suma una fila procesada y publica el nuevo avance. */
    public void avanzar(String importId) {
        ImportJob job = jobs.get(importId);
        if (job == null) {
            return;
        }
        synchronized (job) {
            job.filasProcesadas++;
            publicar(job, EVENT_PROGRESS, job.progreso());
        }
    }

    /** Suma una fila procesada con error y publica el nuevo avance. */
    public void avanzarConError(String importId) {
        ImportJob job = jobs.get(importId);
        if (job == null) {
            return;
        }
        synchronized (job) {
            job.filasProcesadas++;
            job.errorCount++;
            publicar(job, EVENT_PROGRESS, job.progreso());
        }
    }

    /** Incrementa el contador de advertencias. */
    public void incrementarAdvertencias(String importId) {
        ImportJob job = jobs.get(importId);
        if (job == null) {
            return;
        }
        synchronized (job) {
            job.warningCount++;
        }
    }

    /** Cierra la importación con éxito y libera los streams suscriptos. */
    public void finalizar(String importId) {
        ImportJob job = jobs.get(importId);
        if (job == null) {
            return;
        }
        synchronized (job) {
            cerrar(job, EVENT_COMPLETED, null);
        }
    }

    /** Cierra la importación con error y libera los streams suscriptos. */
    public void fallar(String importId, String errorMessage) {
        ImportJob job = jobs.get(importId);
        if (job == null) {
            return;
        }
        synchronized (job) {
            cerrar(job, EVENT_ERROR, errorMessage);
        }
    }

    /** Avance actual de una importación, o null si el id no existe. */
    public MemberBulkImportProgressDTO consultar(String importId) {
        ImportJob job = jobs.get(importId);
        if (job == null) {
            return null;
        }
        synchronized (job) {
            return job.progreso();
        }
    }

    private void cerrar(ImportJob job, String evento, String errorMessage) {
        job.finalizado = true;
        job.eventoFinal = evento;
        job.errorMessage = errorMessage;
        job.finishedAt = System.currentTimeMillis();
        publicar(job, evento, job.progreso());
        for (SseEmitter emitter : job.emitters) {
            try {
                emitter.complete();
            } catch (Exception ignored) {
                // El cliente ya cerró la conexión.
            }
        }
        job.emitters.clear();
    }

    private void publicar(ImportJob job, String evento, MemberBulkImportProgressDTO progreso) {
        for (SseEmitter emitter : job.emitters) {
            if (!enviar(emitter, evento, progreso)) {
                job.emitters.remove(emitter);
            }
        }
    }

    private boolean enviar(SseEmitter emitter, String evento, MemberBulkImportProgressDTO progreso) {
        try {
            emitter.send(SseEmitter.event()
                    .name(evento)
                    .data(progreso, MediaType.APPLICATION_JSON));
            return true;
        } catch (Exception ex) {
            try {
                emitter.completeWithError(ex);
            } catch (Exception ignored) {
                // El emitter ya estaba cerrado.
            }
            return false;
        }
    }

    private void purgarFinalizados() {
        long limite = System.currentTimeMillis() - RETENTION_MS;
        jobs.entrySet().removeIf(entry -> entry.getValue().finalizado
                && entry.getValue().finishedAt < limite);
    }

    private static class ImportJob {

        private final int filasTotales;
        private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();
        private int filasProcesadas;
        private int errorCount;
        private int warningCount;
        private boolean finalizado;
        private String eventoFinal;
        private String errorMessage;
        private long finishedAt;

        private ImportJob(int filasTotales) {
            this.filasTotales = filasTotales;
        }

        private MemberBulkImportProgressDTO progreso() {
            MemberBulkImportProgressDTO dto = new MemberBulkImportProgressDTO();
            dto.setFilasProcesadas(filasProcesadas);
            dto.setFilasTotales(filasTotales);
            dto.setPorcentajeCompletado(filasTotales <= 0 ? 100 : (filasProcesadas * 100) / filasTotales);
            dto.setErrorCount(errorCount);
            dto.setWarningCount(warningCount);
            dto.setErrorMessage(errorMessage);
            return dto;
        }
    }
}
