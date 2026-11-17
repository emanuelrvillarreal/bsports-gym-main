package com.blue.open.Crm.Entity.DTO;

import com.blue.open.Crm.Entity.Enums.CrmLogCanalEnum;
import com.blue.open.Crm.Entity.Enums.CrmLogDireccionEnum;
import lombok.Builder;
import lombok.Getter;

/**
 * Datos de una interaccion con el CRM (blue-crm) a registrar en el log.
 * <p>
 * Se arma con el builder desde los dos puntos de enganche —CrmLoggingInterceptor para las
 * llamadas salientes y CrmWebhookController para los webhooks recibidos— y lo consume
 * CrmIntegracionLogService, que se encarga de truncar los payloads y persistir.
 * <p>
 * Existe para no tener un metodo de registro con doce parametros sueltos, siguiendo el mismo
 * criterio que MercadoPagoTrazaRegistro.
 */
@Getter
@Builder
public class CrmIntegracionLogRegistro {

    private CrmLogDireccionEnum direccion;

    private CrmLogCanalEnum canal;

    // Endpoint involucrado, ej: integraciones/obtener-planes
    private String operacion;

    // Tipo del evento del CRM. Solo aplica a webhooks y ACKs
    private String tipoEvento;

    private String eventId;

    private String metodoHttp;

    private String url;

    private Integer statusCode;

    private boolean exitoso;

    private String requestPayload;

    private String responsePayload;

    private String errorPayload;

    private Long duracionMs;
}
