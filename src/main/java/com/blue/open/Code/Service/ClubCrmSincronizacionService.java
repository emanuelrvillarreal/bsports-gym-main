package com.blue.open.Code.Service;

import com.blue.open.Code.Entity.DTO.ClubRegistrationRequestDTO;
import com.blue.open.Code.Entity.DTO.CompanyDireccionDTO;
import com.blue.open.Crm.Entity.DTO.Request.CrmClienteRequestDTO;
import com.blue.open.Crm.Entity.DTO.Request.CrmDescuentoRequestDTO;
import com.blue.open.Crm.Entity.DTO.Request.CrmSuscripcionRequestDTO;
import com.blue.open.Crm.Entity.DTO.Response.CrmClienteResponseDTO;
import com.blue.open.Crm.Entity.DTO.Response.CrmSuscripcionResponseDTO;
import com.blue.open.Crm.Service.CrmIntegracionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Da de alta en el CRM (blue-crm) el club que se acaba de registrar y lo suscribe a su plan.
 * <p>
 * Traduce los datos del formulario de alta al contrato del CRM. El registro del cliente y la
 * suscripcion al plan estan separados a proposito en {@link #registrarCliente} y
 * {@link #suscribir}, porque son dos requests distintos: el alta del club registra el cliente
 * dentro de su transaccion y la suscripcion llega despues, por el endpoint de suscripcion.
 * Existe como clase aparte porque ese mapeo es un concern propio y no le corresponde a
 * SysadminClubService, que se ocupa del alta local.
 * <p>
 * Depende de: CrmIntegracionService y, a traves de el, del servicio externo blue-crm.
 * <p>
 * No atrapa errores a proposito: quien la invoca decide que hacer con la falla del CRM.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ClubCrmSincronizacionService {

    // Largos que valida el CRM en el alta de cliente. Se recorta antes de enviar para no
    // comerse un 400 por un texto largo
    private static final int MAX_DIRECCION_FISCAL = 255;

    private final CrmIntegracionService crmIntegracionService;

    /**
     * Registra en el CRM el cliente del club que se esta dando de alta. No lo suscribe a ningun
     * plan: de eso se ocupa {@link #suscribir}.
     * <p>
     * Depende de: CrmIntegracionService.crearCliente.
     * Espera: los datos del alta del club, con los codigos de condicion impositiva y provincia
     * que exige el CRM.
     * Responde: el cliente creado, o el existente si el CUIT ya estaba en el CRM (eso no es error:
     * responde CLIENTE_EXISTENTE y se puede seguir con la suscripcion).
     */
    public CrmClienteResponseDTO registrarCliente(ClubRegistrationRequestDTO request) {
        String cuit = this.normalizarCuit(request.getCuit());

        CrmClienteRequestDTO clienteRequest = this.armarCliente(request, cuit);
        CrmClienteResponseDTO clienteResponse = this.crmIntegracionService.crearCliente(clienteRequest);

        String estadoCliente = clienteResponse != null ? clienteResponse.getEstado() : null;
        log.info("ClubCrmSincronizacionService | registrarCliente | club registrado en el CRM | cuit={} "
                + "| estadoCliente={}", cuit, estadoCliente);

        return clienteResponse;
    }

    /**
     * Suscribe a un plan al club que ya esta registrado como cliente del CRM.
     * <p>
     * Depende de: CrmIntegracionService.suscribirCliente, y de que el cliente ya exista del otro
     * lado ({@link #registrarCliente}): el CRM no suscribe un CUIT que no conoce.
     * Espera: el CUIT del club, el codigo de plan del CRM y opcionalmente el descuento (null si
     * la suscripcion va sin descuento).
     * Responde: la suscripcion creada, para persistirla en company_suscripcion_crm.
     */
    public CrmSuscripcionResponseDTO suscribir(String cuit, String codigoPlan, CrmDescuentoRequestDTO descuento) {
        cuit = this.normalizarCuit(cuit);
        codigoPlan = this.limpiar(codigoPlan);

        CrmSuscripcionRequestDTO suscripcionRequest = this.armarSuscripcion(cuit, codigoPlan, descuento);
        CrmSuscripcionResponseDTO suscripcionResponse = this.crmIntegracionService
                .suscribirCliente(suscripcionRequest);

        log.info("ClubCrmSincronizacionService | suscribir | club suscripto en el CRM | cuit={} "
                + "| codigoPlan={} | conDescuento={}", cuit, codigoPlan, descuento != null);

        return suscripcionResponse;
    }

    /** Traduce los datos fiscales del club al contrato de alta de cliente del CRM. */
    private CrmClienteRequestDTO armarCliente(ClubRegistrationRequestDTO request, String cuit) {
        String razonSocial = this.limpiar(request.getRazonSocial());
        String codigoCondicionImpositiva = this.limpiar(request.getCodigoCondicionImpositiva());
        String codigoProvincia = this.limpiar(request.getCodigoProvincia());
        String direccionFiscal = this.armarDireccionFiscal(request.getDomicilioFacturacion());
        String correoFacturacion = this.limpiar(request.getEmailFacturaElectronica());

        CrmClienteRequestDTO cliente = new CrmClienteRequestDTO();
        cliente.setRazonSocial(razonSocial);
        cliente.setCuit(cuit);
        cliente.setCodigoCondicionImpositiva(codigoCondicionImpositiva);
        cliente.setCodigoProvincia(codigoProvincia);
        cliente.setDireccionFiscal(direccionFiscal);
        cliente.setCorreoFacturacion(correoFacturacion);
        // Sin cuenta corriente el CRM no puede suscribir al cliente a un plan
        cliente.setTieneCuentaCorriente(Boolean.TRUE);
        return cliente;
    }

    /**
     * Arma el pedido de suscripcion.
     * <p>
     * Espera: el CUIT ya normalizado, el codigo del plan del CRM y el descuento, que puede ser null.
     * Responde: el pedido listo para enviar.
     */
    private CrmSuscripcionRequestDTO armarSuscripcion(String cuit, String codigoPlan,
            CrmDescuentoRequestDTO descuento) {
        List<String> codigosPlan = List.of(codigoPlan);

        CrmSuscripcionRequestDTO suscripcion = new CrmSuscripcionRequestDTO();
        suscripcion.setCuit(cuit);
        suscripcion.setCodigosPlan(codigosPlan);
        suscripcion.setDescuento(descuento);
        // Sin fecha de inicio, el CRM toma la fecha y hora actual
        return suscripcion;
    }

    /** Compone la direccion fiscal a partir del domicilio de facturacion del club. */
    private String armarDireccionFiscal(CompanyDireccionDTO domicilio) {
        if (domicilio == null) {
            return null;
        }

        String calle = this.limpiar(domicilio.getDireccion());
        String localidad = this.limpiar(domicilio.getLocalidad());
        String codigoPostal = this.limpiar(domicilio.getCodigoPostal());
        String direccionFiscal = calle + ", " + localidad + " (" + codigoPostal + ")";

        return this.recortar(direccionFiscal, MAX_DIRECCION_FISCAL);
    }

    /**
     * Deja el CUIT en los 11 digitos que exige el CRM.
     * <p>
     * El club lo admite con guiones (CuitValidatorService los tolera), pero el CRM valida
     * ^[0-9]{11}$ y rechaza cualquier otro formato.
     */
    private String normalizarCuit(String cuit) {
        if (cuit == null) {
            return null;
        }
        return cuit.replaceAll("[^0-9]", "");
    }

    private String limpiar(String valor) {
        if (valor == null) {
            return "";
        }
        return valor.trim();
    }

    private String recortar(String valor, int max) {
        if (valor == null || valor.length() <= max) {
            return valor;
        }
        return valor.substring(0, max);
    }
}
