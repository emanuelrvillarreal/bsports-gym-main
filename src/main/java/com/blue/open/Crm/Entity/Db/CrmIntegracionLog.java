package com.blue.open.Crm.Entity.Db;

import com.blue.open.Core.Entity.Db.AbstractEntity;
import com.blue.open.Crm.Entity.Enums.CrmLogCanalEnum;
import com.blue.open.Crm.Entity.Enums.CrmLogDireccionEnum;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;

import java.sql.Types;

/**
 * Registro de una interaccion con el CRM (blue-crm): un webhook recibido, un ACK enviado con lo
 * que respondio el CRM, o una llamada del cliente.
 * <p>
 * Es una bitacora append-only para auditoria y diagnostico: nunca se modifica una fila. No se
 * relaciona con ninguna otra entidad, ni siquiera con crm_webhook_eventos, porque tiene que
 * poder registrar entregas que nunca llegan a convertirse en evento (token invalido, JSON
 * ilegible). La correlacion se hace por eventId.
 * <p>
 * No confundir con crm_webhook_eventos, que guarda una fila mutable por evento para resolver la
 * idempotencia. Esta tabla guarda N filas inmutables, una por interaccion.
 */
@Getter
@Setter
@Entity
@Table(name = "crm_integracion_logs")
public class CrmIntegracionLog extends AbstractEntity {

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(Types.VARCHAR)
    @Column(name = "direccion", nullable = false, length = 10)
    private CrmLogDireccionEnum direccion;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(Types.VARCHAR)
    @Column(name = "canal", nullable = false, length = 10)
    private CrmLogCanalEnum canal;

    // Endpoint involucrado, ej: integraciones/obtener-planes
    @Column(name = "operacion", length = 120)
    private String operacion;

    // Tipo del evento del CRM. Solo aplica a webhooks y ACKs
    @Column(name = "tipo_evento", length = 40)
    private String tipoEvento;

    // Identificador del evento en el CRM, para correlacionar con crm_webhook_eventos
    @Column(name = "event_id", length = 64)
    private String eventId;

    @Column(name = "metodo_http", length = 10)
    private String metodoHttp;

    @Column(name = "url", length = 500)
    private String url;

    @Column(name = "status_code")
    private Integer statusCode;

    @Column(name = "exitoso", nullable = false, columnDefinition = "BIT")
    private boolean exitoso;

    // Cuerpo enviado. Truncado segun crm.integracion.logs.max-payload-chars
    @Lob
    @Column(name = "request_payload", columnDefinition = "LONGTEXT")
    private String requestPayload;

    // Cuerpo recibido. Truncado segun crm.integracion.logs.max-payload-chars
    @Lob
    @Column(name = "response_payload", columnDefinition = "LONGTEXT")
    private String responsePayload;

    // Detalle del fallo cuando la interaccion no fue exitosa
    @Lob
    @Column(name = "error_payload", columnDefinition = "LONGTEXT")
    private String errorPayload;

    @Column(name = "duracion_ms")
    private Long duracionMs;
}
