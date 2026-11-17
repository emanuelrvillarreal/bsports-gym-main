package com.blue.open.Code.Service;

import com.blue.open.Code.Entity.DTO.SuscripcionVerificacionResumenDTO;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.CompanySuscripcionCrm;
import com.blue.open.Code.Entity.Db.Factura;
import com.blue.open.Code.Entity.Enums.FacturaEstadoEnum;
import com.blue.open.Code.Repository.CompanyRepository;
import com.blue.open.Code.Repository.CompanySuscripcionCrmRepository;
import com.blue.open.Code.Repository.FacturaRepository;
import com.blue.open.Crm.Entity.DTO.Response.CrmPlanesClienteResponseDTO;
import com.blue.open.Crm.Entity.DTO.Response.CrmSuscripcionActivaDTO;
import com.blue.open.Crm.Service.CrmIntegracionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Verifica el estado de facturacion y de suscripcion de cada club, y ajusta su acceso a la
 * plataforma en consecuencia.
 * <p>
 * Unifica lo que antes hacian dos jobs separados (vencimiento de facturas a las 03:00 y vigencia de
 * suscripcion a las 03:30). Estaban separados y compartian el mismo flag CompanyBloqueoAcceso, asi
 * que se pisaban: el de facturas desbloqueaba a cualquier club sin factura VENCIDA, incluidos los
 * que el de vigencia habia bloqueado por tener la suscripcion anulada. Con una unica decision de
 * acceso por club eso deja de ser posible.
 * <p>
 * Dos fases por corrida:
 * 1. Marca como VENCIDA las facturas PENDIENTE que pasaron su fecha de vencimiento.
 * 2. Por cada club gestionado por el CRM, decide el acceso con todos los motivos juntos.
 * <p>
 * Ademas mantiene espejada company_suscripcion_crm con lo que informa el CRM: ya trae la suscripcion
 * completa para decidir el acceso, y de esa fila salen los cupos de socios y administradores.
 * <p>
 * Lo dispara SuscripcionVerificacionScheduler.
 */
@Service
@Slf4j
public class SuscripcionVerificacionService {

    private static final int STATUS_ACTIVE = 0;

    private static final int BATCH_SIZE = 300;

    // Margen sobre fechaProximaFacturacion para absorber el propio delay de facturacion del CRM
    private static final long DIAS_GRACIA = 2;

    private static final String ESTADO_ANULADA = CompanySuscripcionCrm.ESTADO_ANULADA;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private FacturaRepository facturaRepository;

    @Autowired
    private CrmIntegracionService crmIntegracionService;

    @Autowired
    private CompanyAccesoService companyAccesoService;

    @Autowired
    private CompanySuscripcionCrmRepository companySuscripcionCrmRepository;

    @Autowired
    private CompanySuscripcionCrmService companySuscripcionCrmService;

    /**
     * Corre las dos fases y devuelve el resumen de lo que hizo.
     * <p>
     * No es transaccional a este nivel: cada club implica una llamada HTTP sincrona al CRM, y tanto
     * el marcado de facturas como cada bloqueo/desbloqueo ya son transaccionales por si solos. Si
     * algo falla para un club puntual, se loguea y se sigue con el resto.
     */
    public SuscripcionVerificacionResumenDTO verificarTodas(LocalDate hoy) {
        SuscripcionVerificacionResumenDTO resumen = new SuscripcionVerificacionResumenDTO();
        resumen.setFacturasVencidas(this.marcarFacturasVencidas(hoy));

        for (Company company : this.companyRepository.findByStatus(STATUS_ACTIVE)) {
            try {
                this.resolverAcceso(company, hoy, resumen);
            } catch (Exception e) {
                resumen.sumarError();
                log.error("SuscripcionVerificacionService | verificarTodas | companyId={} | error: {}",
                        company.getId(), e.getMessage(), e);
            }
        }

        log.info("SuscripcionVerificacionService | verificarTodas | facturasVencidas={} | evaluados={} "
                + "| bloqueados={} | desbloqueados={} | erroresCrm={}",
                resumen.getFacturasVencidas(), resumen.getClubesEvaluados(), resumen.getClubesBloqueados(),
                resumen.getClubesDesbloqueados(), resumen.getErroresCrm());
        return resumen;
    }

    /**
     * Sin @Transactional a proposito: llamado desde verificarTodas, la anotacion no tendria efecto
     * (la auto-invocacion no pasa por el proxy de Spring). No hace falta: saveAll ya es
     * transaccional por si mismo, asi que cada lote commitea solo, que ademas evita sostener una
     * transaccion larga mientras se recorre toda la tabla.
     */
    private int marcarFacturasVencidas(LocalDate fechaCorte) {
        int total = 0;
        Pageable pageable = PageRequest.of(0, BATCH_SIZE);

        List<Factura> lote = this.facturaRepository.findByEstadoAndFechaVencimientoLessThanAndStatus(
                FacturaEstadoEnum.PENDIENTE, fechaCorte, STATUS_ACTIVE, pageable);

        while (!lote.isEmpty()) {
            lote.forEach(Factura::marcarVencida);
            this.facturaRepository.saveAll(lote);
            total += lote.size();

            lote = this.facturaRepository.findByEstadoAndFechaVencimientoLessThanAndStatus(
                    FacturaEstadoEnum.PENDIENTE, fechaCorte, STATUS_ACTIVE, pageable);
        }
        return total;
    }

    /**
     * Unica decision de acceso del club, con los dos motivos de bloqueo evaluados en orden.
     * <p>
     * Bloquear gana: el motivo local (deuda vencida) se evalua primero y corta, asi que no hace
     * falta consultar al CRM para un club que ya tiene por que estar bloqueado. Para desbloquear,
     * en cambio, hay que confirmar las dos condiciones — sin deuda vencida y con la suscripcion
     * vigente —, de modo que una falla del CRM nunca termina en un desbloqueo indebido.
     */
    private void resolverAcceso(Company company, LocalDate hoy, SuscripcionVerificacionResumenDTO resumen) {
        if (company.getCuit() == null || company.getCuit().isBlank()) {
            return;
        }
        if (!this.esGestionadoPorCrm(company)) {
            return;
        }
        resumen.sumarEvaluado();

        // 1) Motivo local: deuda vencida. No necesita al CRM
        if (this.facturaRepository.existsByCompanyAndEstadoAndStatus(
                company, FacturaEstadoEnum.VENCIDA, STATUS_ACTIVE)) {
            this.bloquear(company, resumen);
            return;
        }

        // 2) Sin deuda vencida: hay que confirmar contra el CRM que la suscripcion siga vigente
        CrmPlanesClienteResponseDTO planes = this.crmIntegracionService.obtenerPlanesCliente(company.getCuit());
        // Esta app admite un unico plan por club: se toma la primera suscripcion, mismo criterio
        // que CompanySuscripcionCrmService al darla de alta
        CrmSuscripcionActivaDTO suscripcion = planes.getPlanes().stream().findFirst().orElse(null);
        if (suscripcion == null) {
            // El CRM no le conoce ninguna suscripcion a este sistema externo: no esta cubierto
            this.bloquear(company, resumen);
            return;
        }

        this.refrescarSuscripcion(company, suscripcion);

        // Se excluye por ANULADA en vez de incluir por ACTIVA/PENDIENTE_PAGO porque el catalogo
        // de estados del CRM es abierto: un estado nuevo no debe romper esta regla
        if (ESTADO_ANULADA.equalsIgnoreCase(suscripcion.getEstadoSuscripcion())) {
            this.bloquear(company, resumen);
            return;
        }

        // Sin fechaProximaFacturacion se considera cubierta; con ella, cubre hasta esa fecha mas
        // los dias de gracia
        LocalDateTime proximaFacturacion = suscripcion.getFechaProximaFacturacion();
        if (proximaFacturacion != null
                && hoy.isAfter(proximaFacturacion.toLocalDate().plusDays(DIAS_GRACIA))) {
            this.bloquear(company, resumen);
            return;
        }

        this.desbloquear(company, resumen);
    }

    /**
     * Un club esta gestionado por el CRM si ya tiene facturacion o si tiene registrada su
     * suscripcion. Lo segundo cubre a los recien dados de alta, que todavia no recibieron ninguna
     * DEUDA_GENERADA pero cuyo plan igual puede cambiar o anularse en el CRM.
     */
    private boolean esGestionadoPorCrm(Company company) {
        return this.facturaRepository.existsByCompanyAndStatus(company, STATUS_ACTIVE)
                || this.companySuscripcionCrmRepository
                        .findFirstByCompanyIdAndStatus(company.getId(), STATUS_ACTIVE).isPresent();
    }

    /**
     * Espeja en company_suscripcion_crm lo que el CRM informa hoy, incluida una suscripcion
     * ANULADA. Aislado en su propio try/catch: el acceso es lo critico y no puede quedar sin
     * resolverse porque haya fallado el refresco de un dato que solo alimenta los cupos.
     */
    private void refrescarSuscripcion(Company company, CrmSuscripcionActivaDTO suscripcion) {
        try {
            this.companySuscripcionCrmService.refrescar(company, suscripcion);
        } catch (Exception e) {
            log.error("SuscripcionVerificacionService | refrescarSuscripcion | companyId={} | error: {}",
                    company.getId(), e.getMessage(), e);
        }
    }

    private void bloquear(Company company, SuscripcionVerificacionResumenDTO resumen) {
        this.companyAccesoService.bloquear(company);
        resumen.sumarBloqueado();
    }

    private void desbloquear(Company company, SuscripcionVerificacionResumenDTO resumen) {
        this.companyAccesoService.desbloquear(company);
        resumen.sumarDesbloqueado();
    }
}
