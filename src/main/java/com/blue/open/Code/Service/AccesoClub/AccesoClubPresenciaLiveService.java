package com.blue.open.Code.Service.AccesoClub;

import com.blue.open.Code.Entity.DTO.AccesoClubPresenciaUpdateDTO;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;

@Service
@Slf4j
public class AccesoClubPresenciaLiveService {

    private static final String EVENT_UPDATE = "update";
    private static final long EMITTER_TIMEOUT_MS = 30 * 60 * 1000L;

    private final Map<Long, List<SseEmitter>> subscriptions = new ConcurrentHashMap<>();

    @Autowired
    @Qualifier("AccesoClubEventExecutor")
    private Executor executor;

    public SseEmitter suscribir(Long companyId) {
        SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT_MS);
        List<SseEmitter> emitters = subscriptions.computeIfAbsent(companyId, k -> new CopyOnWriteArrayList<>());

        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> {
            emitters.remove(emitter);
            try {
                emitter.complete();
            } catch (Exception ignored) {
            }
        });
        emitter.onError(ex -> emitters.remove(emitter));

        emitters.add(emitter);
        return emitter;
    }

    public void notificar(Long companyId, AccesoClubPresenciaUpdateDTO data) {
        log.info("AccesoClubPresenciaLiveService.notificar fue llamado para company {}", companyId);
        List<SseEmitter> emitters = subscriptions.get(companyId);
        if (emitters == null || emitters.isEmpty()) {
            log.info("AccesoClubPresenciaLiveService.notificar: No habia emitters para company {}", companyId);
            return;
        }

        for (SseEmitter emitter : emitters) {
            CompletableFuture.runAsync(() -> {
                if (!enviar(emitter, EVENT_UPDATE, data)) {
                    emitters.remove(emitter);
                }
            }, executor);
        }
    }

    @Scheduled(fixedRate = 10000)
    public void enviarHeartbeat() {
        for (List<SseEmitter> emitters : subscriptions.values()) {
            for (SseEmitter emitter : emitters) {
                CompletableFuture.runAsync(() -> {
                    try {
                        emitter.send(SseEmitter.event().comment("heartbeat"));
                    } catch (Exception ex) {
                        emitters.remove(emitter);
                    }
                }, executor);
            }
        }
    }

    private boolean enviar(SseEmitter emitter, String evento, AccesoClubPresenciaUpdateDTO data) {
        try {
            emitter.send(SseEmitter.event()
                    .name(evento)
                    .data(data, MediaType.APPLICATION_JSON));
            return true;
        } catch (Exception ex) {
            try {
                emitter.completeWithError(ex);
            } catch (Exception ignored) {
            }
            return false;
        }
    }
}
