package com.blue.open.Crm.Entity.Db;

import com.blue.open.Core.Entity.Db.AbstractEntity;
import com.blue.open.Core.Exceptions.CustomException;
import com.blue.open.Crm.Entity.Enums.CrmWebhookEstadoEnum;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Types;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Registro de un webhook recibido del CRM (blue-crm), para garantizar idempotencia.
 * <p>
 * Cada evento del CRM se guarda una sola vez, identificado por su eventId. Si el CRM reintenta
 * un evento ya procesado, no se vuelve a ejecutar la logica: se le responde el ACK guardado.
 * <p>
 * No se relaciona con ninguna otra entidad: es una bitacora independiente que consulta
 * CrmWebhookEventoService antes de dejar que AbstractCrmWebhookProcesador procese un evento.
 * <p>
 * La entidad es la duena de su maquina de estados. El estado, el motivo del error y el numero
 * de factura no tienen setter publico: la unica forma de cambiarlos es a traves de los metodos
 * marcarProcesado, marcarError y marcarParaReproceso.
 */
@Getter
@NoArgsConstructor
@Entity
@Table(name = "crm_webhook_eventos")
public class CrmWebhookEvento extends AbstractEntity {

    // Identificador del evento en el CRM. Es la clave de la idempotencia, por eso es unico
    @Column(name = "event_id", nullable = false, length = 64, unique = true)
    private String eventId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(Types.VARCHAR)
    @Column(name = "estado", nullable = false, length = 20)
    private CrmWebhookEstadoEnum estado;

    // Tipo del evento tal cual lo mando el CRM. Es texto y no un enum a proposito: si el CRM
    // emite un tipo que todavia no modelamos, igual hay que poder registrarlo
    @Column(name = "tipo", nullable = false, length = 40)
    private String tipo;

    // Numero de factura del evento. Solo aplica a PAGO_IMPUTADO, que lo necesita en el ACK
    @Column(name = "numero_factura", length = 100)
    private String numeroFactura;

    // Detalle del fallo: stack trace completo, o el motivo informado por el procesador
    @Column(name = "motivo_error", columnDefinition = "TEXT")
    private String motivoError;

    // Cuerpo crudo del webhook, para poder reproducir el problema si el evento falla
    @Column(name = "webhook_raw", columnDefinition = "TEXT")
    private String webhookRaw;

    // ! ---------------------------------------------------------------
    // ! Construccion
    // ! ---------------------------------------------------------------

    /**
     * Crea el registro de un evento recien recibido, siempre en estado PENDIENTE.
     * <p>
     * Recibe valores sueltos y no el DTO del webhook para que la entidad no dependa de la capa
     * de transporte.
     * <p>
     * Espera: el identificador del evento en el CRM, su tipo y el cuerpo crudo recibido.
     * Responde: la entidad lista para persistir.
     */
    public static CrmWebhookEvento crear(String eventId, String tipo, String webhookRaw) {
        CrmWebhookEvento evento = new CrmWebhookEvento();
        evento.eventId = eventId;
        evento.tipo = tipo;
        evento.webhookRaw = webhookRaw;
        evento.estado = CrmWebhookEstadoEnum.PENDIENTE;
        return evento;
    }

    // ! ---------------------------------------------------------------
    // ! Transiciones de estado
    // ! ---------------------------------------------------------------

    /**
     * Marca el evento como procesado con exito. Es el estado terminal: a partir de aca, si el
     * CRM reintenta el evento, se le responde el ACK guardado sin ejecutar nada.
     * <p>
     * Limpia el motivo de error porque el evento pudo venir de un intento fallido y dejar el
     * error viejo confundiria al leer la tabla.
     * <p>
     * Espera: el numero de factura si el evento lo necesita para el ACK (PAGO_IMPUTADO), o null.
     */
    public void marcarProcesado(String numeroFactura) {
        this.estado = CrmWebhookEstadoEnum.PROCESADO;
        this.numeroFactura = numeroFactura;
        this.motivoError = null;
    }

    /**
     * Marca el evento como fallido guardando la excepcion completa.
     * <p>
     * Espera: la excepcion que corto el procesamiento.
     */
    public void marcarError(Throwable excepcion) {
        this.marcarError(describirExcepcion(excepcion));
    }

    /**
     * Marca el evento como fallido guardando un motivo en texto. Se usa cuando el procesamiento
     * no lanzo excepcion pero igual devolvio un resultado con error, como hacen los metodos por
     * defecto de los tipos de evento sin implementar.
     * <p>
     * Espera: la descripcion del fallo.
     */
    public void marcarError(String detalle) {
        this.estado = CrmWebhookEstadoEnum.ERROR;
        this.motivoError = detalle;
    }

    /**
     * Vuelve a dejar el evento en PENDIENTE para ejecutarlo de nuevo.
     * <p>
     * Cubre los dos casos de reproceso: un evento que quedo en ERROR y que el CRM reintenta, y
     * un PENDIENTE que quedo colgado. Al persistirse refresca date_updated, que es lo que
     * despues consulta pendienteVencido.
     */
    public void marcarParaReproceso() {
        this.estado = CrmWebhookEstadoEnum.PENDIENTE;
        this.motivoError = null;
    }

    // ! ---------------------------------------------------------------
    // ! Preguntas de dominio
    // ! ---------------------------------------------------------------

    /** Indica si el evento ya se proceso con exito y no hay que volver a ejecutarlo. */
    public boolean estaProcesado() {
        return this.estado == CrmWebhookEstadoEnum.PROCESADO;
    }

    /** Indica si el ultimo intento de procesar el evento fallo. */
    public boolean estaEnError() {
        return this.estado == CrmWebhookEstadoEnum.ERROR;
    }

    /** Indica si el evento esta en proceso. */
    public boolean estaPendiente() {
        return this.estado == CrmWebhookEstadoEnum.PENDIENTE;
    }

    /**
     * Indica si el evento quedo colgado: sigue PENDIENTE desde hace mas tiempo del tolerado,
     * lo que sugiere que el procesamiento se corto por la mitad (por ejemplo, un reinicio de
     * la aplicacion) y conviene volver a ejecutarlo.
     * <p>
     * Espera: los minutos que se toleran antes de dar por colgado un evento.
     * Responde: true si esta PENDIENTE y su ultima actualizacion supera esa tolerancia.
     */
    public boolean pendienteVencido(int minutosTolerancia) {
        if (!this.estaPendiente() || this.dateUpdated == null) {
            return false;
        }
        LocalDateTime ultimaActualizacion = this.dateUpdated.toLocalDateTime();
        return Duration.between(ultimaActualizacion, LocalDateTime.now()).toMinutes() >= minutosTolerancia;
    }

    // ! ---------------------------------------------------------------
    // ! Infraestructura interna
    // ! ---------------------------------------------------------------

    /**
     * Convierte una excepcion en el texto que se guarda como motivo del error.
     * <p>
     * CustomException sobrescribe fillInStackTrace devolviendo null, asi que no tiene stack
     * trace: para ese caso se guardan sus mensajes, que es donde esta la informacion util. Para
     * el resto de las excepciones se guarda el stack trace completo.
     */
    private static String describirExcepcion(Throwable excepcion) {
        if (excepcion == null) {
            return "Error sin detalle";
        }

        if (excepcion instanceof CustomException customException) {
            return customException.getUserMessage() + " | " + customException.getDeveloperMessage();
        }

        StringWriter salida = new StringWriter();
        excepcion.printStackTrace(new PrintWriter(salida));
        return salida.toString();
    }
}
