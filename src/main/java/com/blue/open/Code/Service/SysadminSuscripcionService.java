package com.blue.open.Code.Service;

import com.blue.open.Code.Entity.DTO.SysadminCambioPlanRequestDTO;
import com.blue.open.Code.Entity.DTO.SysadminSuscripcionRequestDTO;
import com.blue.open.Code.Entity.DTO.SysadminSuscripcionResponseDTO;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.CompanySuscripcionCrm;
import com.blue.open.Code.Entity.Db.PlanCrm;
import com.blue.open.Code.Repository.CompanyRepository;
import com.blue.open.Code.Repository.CompanySuscripcionCrmRepository;
import com.blue.open.Core.Exceptions.CustomException;
import com.blue.open.Crm.Entity.DTO.Request.CrmDesuscripcionRequestDTO;
import com.blue.open.Crm.Entity.DTO.Response.CrmCancelarSuscripcionesResponseDTO;
import com.blue.open.Crm.Entity.DTO.Response.CrmPlanesClienteResponseDTO;
import com.blue.open.Crm.Entity.DTO.Response.CrmSuscripcionActivaDTO;
import com.blue.open.Crm.Entity.DTO.Response.CrmSuscripcionResponseDTO;
import com.blue.open.Crm.Service.CrmIntegracionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Gestion de la suscripcion de un club al CRM (blue-crm) desde el panel de sysadmin: suscribir,
 * cambiar de plan y desuscribir.
 * <p>
 * Los tres flujos terminan actualizando company_suscripcion_crm, que es de donde salen los cupos
 * de socios y administradores del plan.
 * <p>
 * No son transaccionales de punta a punta a proposito: cada paso es una llamada HTTP al CRM y una
 * transaccion local no puede revertirlas. El orden esta elegido para que, si algo se corta a la
 * mitad, el estado local nunca quede diciendo que el club tiene un plan que en el CRM no tiene.
 */
@Service
@Slf4j
public class SysadminSuscripcionService {

    private static final int STATUS_ACTIVE = 0;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private CompanySuscripcionCrmRepository companySuscripcionCrmRepository;

    @Autowired
    private CompanySuscripcionCrmService companySuscripcionCrmService;

    @Autowired
    private ClubCrmSincronizacionService clubCrmSincronizacionService;

    @Autowired
    private CrmIntegracionService crmIntegracionService;

    @Autowired
    private CompanyAccesoService companyAccesoService;

    /**
     * Suscribe a un plan un club que ya es cliente del CRM.
     * <p>
     * Anula las suscripciones que pudiera tener sueltas y lo suscribe al plan pedido. No da de
     * alta el cliente: para suscribirlo el club ya tiene que existir del otro lado.
     * <p>
     * Si todo sale bien y el club estaba bloqueado, le devuelve el acceso en el momento. Es el
     * caso del club recien dado de alta, que nace bloqueado por no tener plan: esperar al job de
     * vigencia (03:00) lo dejaria sin poder operar hasta la madrugada siguiente.
     */
    public SysadminSuscripcionResponseDTO suscribir(String companyUuid, SysadminSuscripcionRequestDTO request) {
        Company company = this.resolverCompany(companyUuid);
        String cuit = this.resolverCuit(company);
        // Antes de desuscribir: un descuento invalido no puede dejar al club sin su plan vigente
        this.crmIntegracionService.validarDescuento(request.getDescuento());

        this.desuscribirDeSusPlanesVigentes(cuit, company);

        CrmSuscripcionResponseDTO suscripcion = this.clubCrmSincronizacionService
                .suscribir(cuit, request.getCodigoPlan(), request.getDescuento());
        this.companySuscripcionCrmService.registrar(company, suscripcion);

        if (this.companyAccesoService.estaBloqueada(company)) {
            this.companyAccesoService.desbloquear(company);
        }

        log.info("SysadminSuscripcionService | suscribir | club suscripto | companyId={} | codigoPlan={}",
                company.getId(), request.getCodigoPlan());
        return this.toResponse(company);
    }

    /**
     * Cambia el plan de un club que ya es cliente del CRM.
     * <p>
     * Cancela todas sus suscripciones (lo que ademas compensa la deuda pendiente de esos planes en
     * el CRM) y lo suscribe al plan nuevo. No vuelve a dar de alta el cliente: para cambiar de plan
     * el club ya tiene que existir del otro lado.
     */
    public SysadminSuscripcionResponseDTO cambiarPlan(String companyUuid, SysadminCambioPlanRequestDTO request) {
        Company company = this.resolverCompany(companyUuid);
        String cuit = this.resolverCuit(company);
        // Antes de cancelar: un descuento invalido no puede dejar al club sin su plan vigente
        this.crmIntegracionService.validarDescuento(request.getDescuento());

        CrmPlanesClienteResponseDTO planes = this.crmIntegracionService.obtenerPlanesCliente(cuit);

        List<String> codigosVigentes = planes.getPlanes().stream()
                .filter(suscripcion -> !CompanySuscripcionCrm.ESTADO_ANULADA
                        .equalsIgnoreCase(suscripcion.getEstadoSuscripcion()))
                .map(CrmSuscripcionActivaDTO::getCodigoPlan)
                .filter(codigo -> codigo != null && !codigo.isBlank())
                .distinct()
                .toList();

        if (!codigosVigentes.isEmpty()) {
            this.crmIntegracionService.cancelarSuscripcionesCliente(cuit);
        }

        CrmSuscripcionResponseDTO suscripcion = this.clubCrmSincronizacionService
                .suscribir(cuit, request.getCodigoPlan(), request.getDescuento());
        this.companySuscripcionCrmService.registrar(company, suscripcion);

        log.info("SysadminSuscripcionService | cambiarPlan | plan cambiado | companyId={} | codigoPlan={}",
                company.getId(), request.getCodigoPlan());
        return this.toResponse(company);
    }

    /**
     * Da de baja al club en el CRM: cancela todas sus suscripciones y compensa la deuda pendiente.
     * <p>
     * Ademas del CRM, marca la suscripcion local como ANULADA y le corta el acceso a la plataforma
     * en el momento: sin suscripcion vigente el club no deberia poder operar, y esperar al job de
     * vigencia (03:30) le dejaria acceso libre hasta la madrugada.
     */
    public CrmCancelarSuscripcionesResponseDTO desuscribir(String companyUuid) {
        Company company = this.resolverCompany(companyUuid);
        String cuit = this.resolverCuit(company);

        CrmCancelarSuscripcionesResponseDTO respuesta = this.crmIntegracionService
                .cancelarSuscripcionesCliente(cuit);

        this.companySuscripcionCrmService.anular(company);
        this.companyAccesoService.bloquear(company);

        log.info("SysadminSuscripcionService | desuscribir | club dado de baja en el CRM | companyId={} "
                + "| suscripcionesAnuladas={} | facturasCompensadas={}",
                company.getId(), respuesta.getSuscripcionesAnuladas(), respuesta.getFacturasCompensadas());
        return respuesta;
    }

    /**
     * Anula las suscripciones que el club tenga vigentes antes de suscribirlo de nuevo.
     * <p>
     * Se filtran las ya ANULADA porque el CRM responde 404 si ninguno de los codigos informados
     * tiene una suscripcion activa, y eso cortaria el alta de un club que solo tenia bajas viejas.
     */
    private void desuscribirDeSusPlanesVigentes(String cuit, Company company) {
        CrmPlanesClienteResponseDTO planes = this.crmIntegracionService.obtenerPlanesCliente(cuit);

        List<String> codigosVigentes = planes.getPlanes().stream()
                .filter(suscripcion -> !CompanySuscripcionCrm.ESTADO_ANULADA
                        .equalsIgnoreCase(suscripcion.getEstadoSuscripcion()))
                .map(CrmSuscripcionActivaDTO::getCodigoPlan)
                .filter(codigo -> codigo != null && !codigo.isBlank())
                .distinct()
                .toList();

        if (codigosVigentes.isEmpty()) {
            return;
        }

        CrmDesuscripcionRequestDTO desuscripcion = new CrmDesuscripcionRequestDTO();
        desuscripcion.setCuit(cuit);
        desuscripcion.setCodigosPlan(codigosVigentes);
        this.crmIntegracionService.desuscribirCliente(desuscripcion);

        log.info("SysadminSuscripcionService | desuscribirDeSusPlanesVigentes | companyId={} | codigos={}",
                company.getId(), codigosVigentes);
    }

    private Company resolverCompany(String companyUuid) {
        if (companyUuid == null || companyUuid.isBlank()) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Gimnasio requerido",
                    "SysadminSuscripcionService | companyUuid vacío");
        }
        Company company = this.companyRepository.findFirstByUuidWithStatusIsActive(companyUuid);
        if (company == null) {
            throw new CustomException(HttpStatus.NOT_FOUND, "Gimnasio no encontrado",
                    "SysadminSuscripcionService | companyUuid: " + companyUuid);
        }
        return company;
    }

    /** El CRM identifica al cliente por CUIT: sin el no hay nada que hacer del otro lado. */
    private String resolverCuit(Company company) {
        String cuit = company.getCuit() != null ? company.getCuit().replaceAll("[^0-9]", "") : null;
        if (cuit == null || cuit.isBlank()) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "El gimnasio no tiene CUIT cargado",
                    "SysadminSuscripcionService | companyId: " + company.getId());
        }
        return cuit;
    }

    @Transactional(readOnly = true)
    public SysadminSuscripcionResponseDTO toResponse(Company company) {
        CompanySuscripcionCrm registro = this.companySuscripcionCrmRepository
                .findFirstByCompanyIdWithPlan(company.getId(), STATUS_ACTIVE)
                .orElse(null);

        SysadminSuscripcionResponseDTO dto = new SysadminSuscripcionResponseDTO();
        dto.setCompanyUuid(company.getUuid());
        if (registro == null) {
            return dto;
        }

        PlanCrm plan = registro.getPlan();
        if (plan != null) {
            dto.setCodigoPlan(plan.getCodigo());
            dto.setNombrePlan(plan.getNombre());
        }
        dto.setUuidSuscripcionCrm(registro.getUuidSuscripcionCrm());
        dto.setEstado(registro.getEstado());
        dto.setFechaProximaFacturacion(registro.getFechaProximaFacturacion());
        return dto;
    }
}
