package com.blue.open.Code.WebSocketConfig;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean("AsyncTaskExecutor")
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setQueueCapacity(150);
        executor.setMaxPoolSize(4);
        executor.setThreadNamePrefix("Async-Thread");
        executor.initialize();
        return executor;
    }

    /**
     * Pool exclusivo para las importaciones masivas: son tareas largas (miles de
     * filas con escrituras en base) y no deben ocupar los hilos de
     * {@code AsyncTaskExecutor}, compartidos con webhooks y eventos.
     */
    @Bean("BulkImportTaskExecutor")
    public Executor getBulkImportExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("BulkImport-Thread");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }

    /**
     * Pool exclusivo para eventos de AccesoClub (SSE): maneja notificaciones
     * de nuevos accesos a los clientes suscritos sin competir con otros eventos.
     */
    @Bean("AccesoClubEventExecutor")
    public Executor getAccesoClubEventExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("AccesoClub-Event");
        executor.initialize();
        return executor;
    }

    /**
     * Pool grande y exclusivo para operaciones de Mercado Pago: transacciones,
     * callbacks y reconciliación que pueden ser largas y no deben afectar
     * otros procesos asincronicos del sistema.
     */
    @Bean("MercadoPagoExecutor")
    public Executor getMercadoPagoExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(8);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("MercadoPago-Thread");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(120);
        executor.initialize();
        return executor;
    }
}
