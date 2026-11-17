package com.blue.open.Code.Service;

import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.CompanySuscripcionCrm;
import com.blue.open.Code.Entity.Db.PlanCrm;
import com.blue.open.Code.Repository.CompanySuscripcionCrmRepository;
import com.blue.open.Code.Repository.PlanCrmRepository;
import com.blue.open.Core.Exceptions.CustomException;
import com.blue.open.Crm.Entity.DTO.Response.CrmSuscripcionActivaDTO;
import com.blue.open.Crm.Entity.DTO.Response.CrmSuscripcionCreadaDTO;
import com.blue.open.Crm.Entity.DTO.Response.CrmSuscripcionResponseDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Persiste la suscripcion del club a su plan del CRM (blue-crm).
 * <p>
 * registrar parte de la respuesta de ClubCrmSincronizacionService.suscribir: lo invoca
 * SysadminSuscripcionService cuando se suscribe o cambia de plan a mano.
 * <p>
 * registrarDesdeCrm parte de lo que informa GET integraciones/cliente/planes: lo invoca
 * ClubSuscripcionInicialService en el alta del club, tanto manual (SysadminClubService.registerClub)
 * como por webhook ALTA_CLIENTE (CompanyService.registrarAltaCliente), cuando el CRM ya le conoce
 * un plan al cliente.
 * <p>
 * Despues del alta, SuscripcionVerificacionService llama a refrescar todos los dias para mantener la
 * fila espejada con el CRM. Es el unico escritor de la tabla.
 */
@Service
@Slf4j
public class CompanySuscripcionCrmService {

    private static final int STATUS_ACTIVE = 0;

    @Autowired
    private CompanySuscripcionCrmRepository companySuscripcionCrmRepository;

    @Autowired
    private PlanCrmRepository planCrmRepository;

    /**
     * Persiste la suscripcion que acaba de devolver el CRM. Crea la fila si el club todavia no
     * tenia, o la sobrescribe si ya existia (caso de un club que se resuscribe o cambia de plan).
     */
    @Transactional
    public void registrar(Company company, CrmSuscripcionResponseDTO suscripcionResponse) {
        CrmSuscripcionCreadaDTO suscripcionCreada = this.extraerSuscripcionCreada(suscripcionResponse);
        PlanCrm plan = this.resolverPlan(suscripcionCreada.getCodigoPlan());

        CompanySuscripcionCrm registro = this.companySuscripcionCrmRepository
                .findFirstByCompanyIdAndStatus(company.getId(), STATUS_ACTIVE)
                .orElseGet(() -> CompanySuscripcionCrm.crear(company, plan,
                        suscripcionCreada.getUuidSuscripcion(), suscripcionCreada.getUuidFacturaInicial(),
                        suscripcionCreada.getEstado(), suscripcionCreada.getFechaProximaFacturacion()));

        registro.actualizar(plan, suscripcionCreada.getUuidSuscripcion(), suscripcionCreada.getUuidFacturaInicial(),
                suscripcionCreada.getEstado(), suscripcionCreada.getFechaProximaFacturacion());
        this.companySuscripcionCrmRepository.save(registro);
    }

    /**
     * Registra la suscripcion que el CRM ya le conocia al club, tal como la informa
     * GET integraciones/cliente/planes.
     * <p>
     * Crea la fila si el club todavia no tenia, o la sobrescribe si ya existia. Esa es la
     * diferencia con refrescar, que solo sincroniza filas que ya estan: este se usa en el alta de
     * un club cuyo cliente ya tiene un plan en el CRM, para que arranque con los cupos de ese
     * plan. En el alta manual es el caso de un CUIT que ya era cliente; en el alta por webhook
     * ALTA_CLIENTE es el caso normal, porque el CRM avisa recien cuando se pago la suscripcion.
     * <p>
     * No guarda uuidFacturaInicialCrm: ese endpoint no lo devuelve. Si la fila ya existia, se
     * conserva el que tenia.
     */
    @Transactional
    public void registrarDesdeCrm(Company company, CrmSuscripcionActivaDTO suscripcion) {
        PlanCrm plan = this.resolverPlan(suscripcion.getCodigoPlan());

        CompanySuscripcionCrm registro = this.companySuscripcionCrmRepository
                .findFirstByCompanyIdAndStatus(company.getId(), STATUS_ACTIVE)
                .orElseGet(() -> CompanySuscripcionCrm.crear(company, plan,
                        suscripcion.getUuidSuscripcion(), null,
                        suscripcion.getEstadoSuscripcion(), suscripcion.getFechaProximaFacturacion()));

        registro.refrescarDesdeCrm(plan, suscripcion.getUuidSuscripcion(),
                suscripcion.getEstadoSuscripcion(), suscripcion.getFechaProximaFacturacion());
        this.companySuscripcionCrmRepository.save(registro);
    }

    /**
     * Marca como anulada la suscripcion local despues de darla de baja en el CRM.
     * <p>
     * No borra la fila ni la da de baja logica: queda como historial del ultimo plan que tuvo el
     * club, con estado ANULADA. Si el club no tenia fila, no hay nada que anular.
     */
    @Transactional
    public void anular(Company company) {
        this.companySuscripcionCrmRepository
                .findFirstByCompanyIdAndStatus(company.getId(), STATUS_ACTIVE)
                .ifPresent(registro -> {
                    registro.anular();
                    this.companySuscripcionCrmRepository.save(registro);
                });
    }

    /**
     * Refresca la suscripcion local con lo que el CRM informa hoy, incluida una suscripcion
     * ANULADA: el CRM es la fuente de verdad y de esta fila salen despues los cupos del plan.
     * <p>
     * No crea la fila si falta: solo sincroniza la que ya existe. Tampoco refresca si el plan
     * todavia no esta en el catalogo local, para no dejar la suscripcion apuntando a un plan
     * incompleto (un PlanCrm sin productosJson bloquearia el alta de socios, porque el chequeo de
     * cupo es fail-closed). CrmCatalogoSyncScheduler lo sincroniza y el proximo refresh lo toma.
     */
    @Transactional
    public void refrescar(Company company, CrmSuscripcionActivaDTO suscripcion) {
        String codigoPlan = suscripcion.getCodigoPlan();
        if (codigoPlan == null || codigoPlan.isBlank()) {
            log.warn("CompanySuscripcionCrmService | refrescar | el CRM informo una suscripcion sin "
                    + "codigo de plan, no se refresca | companyId={}", company.getId());
            return;
        }

        PlanCrm plan = this.planCrmRepository.findFirstByCodigoAndStatus(codigoPlan, STATUS_ACTIVE)
                .orElse(null);
        if (plan == null) {
            log.warn("CompanySuscripcionCrmService | refrescar | el plan no esta sincronizado en "
                    + "plan_crm, no se refresca | companyId={} | codigoPlan={}", company.getId(), codigoPlan);
            return;
        }

        this.companySuscripcionCrmRepository
                .findFirstByCompanyIdAndStatus(company.getId(), STATUS_ACTIVE)
                .ifPresent(registro -> {
                    registro.refrescarDesdeCrm(plan, suscripcion.getUuidSuscripcion(),
                            suscripcion.getEstadoSuscripcion(), suscripcion.getFechaProximaFacturacion());
                    this.companySuscripcionCrmRepository.save(registro);
                });
    }

    /**
     * Busca el plan en el catalogo local. No lo crea: un PlanCrm incompleto (sin precio ni
     * productosJson) dejaria al club sin poder dar de alta socios, porque el chequeo de cupo es
     * fail-closed. Si falta, corta: el catalogo se sincroniza solo (cron diario o webhook
     * PLAN_CAMBIO) o a mano con POST /sysadmin/jobs/catalogo-sync.
     */
    private PlanCrm resolverPlan(String codigoPlan) {
        return this.planCrmRepository.findFirstByCodigoAndStatus(codigoPlan, STATUS_ACTIVE)
                .orElseThrow(() -> new CustomException(HttpStatus.CONFLICT,
                        "El plan del gimnasio no está sincronizado en el catálogo local. "
                                + "Sincronizá el catálogo del CRM y reintentá.",
                        "CompanySuscripcionCrmService | resolverPlan | codigoPlan no encontrado en "
                                + "plan_crm | codigoPlan: " + codigoPlan));
    }

    private CrmSuscripcionCreadaDTO extraerSuscripcionCreada(CrmSuscripcionResponseDTO suscripcionResponse) {
        List<CrmSuscripcionCreadaDTO> suscripciones = suscripcionResponse != null
                ? suscripcionResponse.getSuscripciones()
                : null;
        if (suscripciones == null || suscripciones.isEmpty()) {
            throw new CustomException(HttpStatus.BAD_GATEWAY,
                    "El CRM no devolvió la suscripción del gimnasio",
                    "CompanySuscripcionCrmService | registrar | respuesta del CRM sin suscripciones");
        }
        // Esta app admite un unico plan por club
        return suscripciones.get(0);
    }
}
