package com.blue.open.Crm.Service;

import com.blue.open.Crm.Entity.Enums.CrmTipoEventoEnum;
import com.fasterxml.jackson.databind.JsonNode;

import com.blue.open.Code.Service.CompanyService;
import com.blue.open.Code.Service.CrmCatalogoSyncService;
import com.blue.open.Code.Service.FacturaService;
import com.blue.open.Crm.Entity.DTO.Response.CrmPlanResponseDTO;
import com.blue.open.Crm.Entity.DTO.Response.CrmProductoResponseDTO;
import com.blue.open.Crm.Entity.DTO.Webhook.CrmAltaClienteWebhookDTO;
import com.blue.open.Crm.Entity.DTO.Webhook.CrmDeudaGeneradaWebhookDTO;
import com.blue.open.Crm.Entity.DTO.Webhook.CrmPagoImputadoWebhookDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Implementacion de los webhooks del CRM (blue-crm) para bsports-club.
 * <p>
 * Este es el unico archivo que hay que tocar para implementar un evento: alcanza con
 * sobrescribir el metodo del tipo que corresponda. Los que no se sobrescriben heredan el
 * comportamiento por defecto de AbstractCrmWebhookProcesador, que deja un log de advertencia y
 * responde un ACK con error para que el CRM reintente.
 * <p>
 * Metodos disponibles para sobrescribir:
 * onPagoImputado, onDeudaGenerada, onPlanCambio, onProductoCambio y onAltaCliente.
 * <p>
 * La convencion para informar el resultado es simple: si el metodo retorna, el evento se da por
 * procesado; si algo falla, hay que lanzar una excepcion y el CRM lo reintenta. No hay que
 * armar ni devolver el ACK: de eso se encarga AbstractCrmWebhookProcesador, que ya tiene el
 * identificador del evento y, para PAGO_IMPUTADO, el numero de factura que llego en el webhook.
 * <p>
 * Ejemplo:
 *
 * <pre>
 * &#64;Override
 * protected void onPagoImputado(CrmPagoImputadoWebhookDTO data, String eventId) {
 *     // ... logica de negocio del pago ...
 *     // si algo falla, lanzar una excepcion: el CRM reintenta el evento
 * }
 * </pre>
 *
 * Atencion con la idempotencia: un evento ya procesado con exito no se vuelve a ejecutar, pero
 * uno que termino en error si se reprocesa en el proximo reintento del CRM. La logica que se
 * implemente tiene que tolerar ejecutarse de nuevo despues de haber fallado a la mitad.
 */
@Slf4j
@Service
public class CrmWebhookProcesador extends AbstractCrmWebhookProcesador {

    @Autowired
    private FacturaService facturaService;

    @Autowired
    private CrmCatalogoSyncService crmCatalogoSyncService;

    @Autowired
    private CompanyService companyService;

    /**
     * Da de alta el club del cliente que informa el CRM: club, admin, suscripcion y facturas
     * previas. Todo el flujo lo resuelve CompanyService.registrarAltaCliente.
     * <p>
     * Si ya existe un club con ese uuidCliente o CUIT, lanza y el ACK sale con error. Si lo que
     * falla es el admin o la suscripcion, el club queda creado igual: se loguea y el evento se
     * confirma.
     */
    @Override
    protected void onAltaCliente(CrmAltaClienteWebhookDTO data, String eventId) {
        this.companyService.registrarAltaCliente(data);
    }

    /**
     * Marca como pagada la factura a la que el CRM imputo un pago.
     * <p>
     * Si el club o la factura no existen, lanza y el CRM reintenta el evento. Eso cubre el caso
     * de que el pago llegue antes de haberse procesado la deuda que lo origino.
     */
    @Override
    protected void onPagoImputado(CrmPagoImputadoWebhookDTO data, String eventId) {
        this.facturaService.registrarPagoImputado(data);
    }

    /**
     * Registra como factura del club la deuda que informa el CRM.
     * <p>
     * Si la factura ya estaba registrada, FacturaService la omite sin lanzar y el evento se
     * confirma igual. Si el club del CUIT no existe, lanza y el CRM reintenta el evento.
     */
    @Override
    protected void onDeudaGenerada(CrmDeudaGeneradaWebhookDTO data, String eventId) {
        this.facturaService.registrarDeudaGenerada(data);
    }

    /**
     * Si el CRM deshizo la deuda (no reconoce el evento despues de reintentar el ACK), se descarta la factura
     * que se habia registrado, para que el club no vea un aviso de una factura que no existe.
     */
    @Override
    protected void onEventoInexistenteEnCrm(CrmTipoEventoEnum tipo, JsonNode data, String eventId) {
        if (tipo != CrmTipoEventoEnum.DEUDA_GENERADA || data == null || !data.hasNonNull("uuidFactura")) {
            super.onEventoInexistenteEnCrm(tipo, data, eventId);
            return;
        }
        this.facturaService.descartarFacturaInexistenteEnCrm(data.get("uuidFactura").asText(), eventId);
    }

    /**
     * Actualiza plan_crm con los datos que el CRM informa para el plan modificado.
     * <p>
     * Mismo upsert por codigo que usa CrmCatalogoSyncScheduler en la sincronizacion diaria: no
     * hace falta esperar a esa corrida para que el precio/nombre queden al dia.
     */
    @Override
    protected void onPlanCambio(CrmPlanResponseDTO data, String eventId) {
        this.crmCatalogoSyncService.actualizarPlan(data);
    }

    /**
     * Actualiza productos_crm con los datos que el CRM informa para el producto modificado.
     * <p>
     * Mismo upsert por codigo que usa CrmCatalogoSyncScheduler en la sincronizacion diaria.
     */
    @Override
    protected void onProductoCambio(CrmProductoResponseDTO data, String eventId) {
        this.crmCatalogoSyncService.actualizarProducto(data);
    }
}
