package com.blue.open.Code.Service;

import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.CompanySuscripcionCrm;
import com.blue.open.Crm.Entity.DTO.Response.CrmPlanesClienteResponseDTO;
import com.blue.open.Crm.Entity.DTO.Response.CrmSuscripcionActivaDTO;
import com.blue.open.Crm.Service.CrmIntegracionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Resuelve con que suscripcion arranca un club recien dado de alta y si puede operar, a partir
 * de lo que el CRM le conoce al cliente.
 * <p>
 * Lo comparten el alta manual (SysadminClubService.registerClub) y el alta por webhook
 * ALTA_CLIENTE (CompanyService.registrarAltaCliente), para que los dos apliquen la misma regla
 * de acceso.
 * <p>
 * La consulta al CRM y la persistencia estan separadas a proposito: el webhook consulta fuera de
 * transaccion y persiste despues, mientras que el alta manual hace las dos cosas dentro de la
 * transaccion de registerClub.
 */
@Service
public class ClubSuscripcionInicialService {

    // Ventana que se le da a una suscripcion PENDIENTE_PAGO para que se pague la primera factura.
    // Pasada, el club deja de poder operar
    private static final long DIAS_GRACIA_PENDIENTE_PAGO = 10;

    @Autowired
    private CrmIntegracionService crmIntegracionService;

    @Autowired
    private CompanySuscripcionCrmService companySuscripcionCrmService;

    @Autowired
    private CompanyAccesoService companyAccesoService;

    /** La suscripcion que el CRM le conoce hoy al cliente del CUIT, o null si no tiene ninguna. */
    public CrmSuscripcionActivaDTO buscarSuscripcionEnCrm(String cuit) {
        String cuitNormalizado = normalizarCuit(cuit);
        CrmPlanesClienteResponseDTO planes = crmIntegracionService.obtenerPlanesCliente(cuitNormalizado);
        if (planes == null || planes.getPlanes() == null) {
            return null;
        }

        // Esta app admite un unico plan por club: se toma la primera suscripcion, mismo criterio
        // que SuscripcionVerificacionService y CompanySuscripcionCrmService
        return planes.getPlanes().stream().findFirst().orElse(null);
    }

    /**
     * Espeja en company_suscripcion_crm la suscripcion que informa el CRM y le resuelve el acceso
     * al club segun su estado.
     */
    @Transactional
    public void aplicarSuscripcion(Company company, CrmSuscripcionActivaDTO suscripcion) {
        companySuscripcionCrmService.registrarDesdeCrm(company, suscripcion);

        if (habilitaAcceso(suscripcion)) {
            companyAccesoService.desbloquear(company);
            return;
        }
        companyAccesoService.bloquear(company);
    }

    /**
     * Si la suscripcion que informa el CRM alcanza para dejar operar al club.
     * <p>
     * Corta en dos casos: la suscripcion esta ANULADA, o esta PENDIENTE_PAGO y ya se le paso la
     * ventana para pagar la primera factura. Cualquier otro estado habilita — el catalogo de
     * estados del CRM es abierto y un estado nuevo no deberia dejar al club afuera, mismo criterio
     * que SuscripcionVerificacionService.
     */
    private boolean habilitaAcceso(CrmSuscripcionActivaDTO suscripcion) {
        String estado = suscripcion.getEstadoSuscripcion();

        if (CompanySuscripcionCrm.ESTADO_ANULADA.equalsIgnoreCase(estado)) {
            return false;
        }

        if (CompanySuscripcionCrm.ESTADO_PENDIENTE_PAGO.equalsIgnoreCase(estado)) {
            return !vencioLaGraciaDePago(suscripcion.getFechaInicio());
        }

        return true;
    }

    /**
     * Si una suscripcion PENDIENTE_PAGO ya arranco hace mas de DIAS_GRACIA_PENDIENTE_PAGO dias.
     * <p>
     * Sin fecha de inicio no se puede afirmar que la ventana vencio, asi que se deja pasar: el job
     * de vigencia vuelve a evaluar al club todos los dias.
     */
    private boolean vencioLaGraciaDePago(LocalDateTime fechaInicio) {
        if (fechaInicio == null) {
            return false;
        }
        return fechaInicio.toLocalDate().plusDays(DIAS_GRACIA_PENDIENTE_PAGO).isBefore(LocalDate.now());
    }

    /** El CRM identifica al cliente por CUIT y exige 11 digitos; el club lo admite con guiones. */
    private String normalizarCuit(String cuit) {
        if (cuit == null) {
            return null;
        }
        return cuit.replaceAll("[^0-9]", "");
    }
}
