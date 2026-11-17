package com.blue.open.Code.Service;

import com.blue.open.Code.Config.FrontUrlProperties;
import com.blue.open.Code.Entity.DTO.FacturaPagoRequestDTO;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.Factura;
import com.blue.open.Code.Entity.Db.Movimiento;
import com.blue.open.Code.Entity.Db.PlanCrm;
import com.blue.open.Code.Entity.Db.ProductoCrm;
import com.blue.open.Code.Entity.Db.User;
import com.blue.open.Code.Entity.Enums.FacturaEstadoEnum;
import com.blue.open.Code.Entity.Enums.MovimientoEstadoEnum;
import com.blue.open.Code.Entity.Enums.MovimientoTipoEnum;
import com.blue.open.Code.Repository.CompanyRepository;
import com.blue.open.Code.Repository.FacturaRepository;
import com.blue.open.Code.Repository.MovimientoRepository;
import com.blue.open.Code.Repository.PlanCrmRepository;
import com.blue.open.Code.Repository.ProductoCrmRepository;
import com.blue.open.Core.Entity.Db.UserCompanyRole;
import com.blue.open.Core.Exceptions.CustomException;
import com.blue.open.Core.Repository.UserCompanyRoleRepository;
import com.blue.open.Crm.Entity.DTO.Request.PagoIntegracionRequestDTO;
import com.blue.open.Crm.Entity.DTO.Response.MercadoPagoPreferenceResponseDTO;
import com.blue.open.Crm.Entity.DTO.Webhook.CrmDeudaGeneradaWebhookDTO;
import com.blue.open.Crm.Entity.DTO.Webhook.CrmPagoImputadoWebhookDTO;
import com.blue.open.Crm.Service.CrmIntegracionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Registra como facturas del club las deudas que informa el CRM (blue-crm).
 * <p>
 * Depende de: FacturaRepository y CompanyRepository. No llama a ningun servicio externo: lo
 * invoca CrmWebhookProcesador cuando llega un evento DEUDA_GENERADA.
 * <p>
 * Solo orquesta: busca, delega en los metodos de la entidad y persiste. Las validaciones de los
 * datos de la factura viven en Factura.crear.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class FacturaService {

    private static final int STATUS_ACTIVE = 0;

    private final FacturaRepository facturaRepository;

    private final CompanyRepository companyRepository;

    private final CompanyAccesoService companyAccesoService;

    private final CrmIntegracionService crmIntegracionService;

    private final FrontUrlProperties frontUrlProperties;

    private final PlanCrmRepository planCrmRepository;

    private final ProductoCrmRepository productoCrmRepository;

    private final MovimientoRepository movimientoRepository;

    private final UserCompanyRoleRepository userCompanyRoleRepository;

    /**
     * Crea la factura correspondiente a una deuda generada en el CRM.
     * <p>
     * Si la factura ya estaba registrada no hace nada y no lanza: el CRM reintenta el mismo
     * evento y no tiene que resultar en una factura duplicada ni en un error.
     * <p>
     * Si el club del CUIT informado todavia no existe, la factura se registra igual sin club y
     * se le asocia cuando se lo da de alta (asociarFacturasSinClub).
     * <p>
     * Espera: los datos del webhook DEUDA_GENERADA.
     * Responde: nada.
     */
    @Transactional
    public void registrarDeudaGenerada(CrmDeudaGeneradaWebhookDTO data) {
        String uuidFacturaCrm = data.getUuidFactura();

        Optional<Factura> registrada = this.facturaRepository.findFirstByUuidFacturaCrm(uuidFacturaCrm);
        if (registrada.isPresent()) {
            log.info("FacturaService | registrarDeudaGenerada | la factura ya estaba registrada, se omite "
                    + "| uuidFacturaCrm={}", uuidFacturaCrm);
            return;
        }

        Company company = this.buscarCompanyActiva(data.getUuidCliente(), data.getCuit());
        if (company == null) {
            log.warn("FacturaService | registrarDeudaGenerada | no existe el club del cliente, la factura se "
                    + "registra sin club | uuidFacturaCrm={} | uuidCliente={} | cuit={}",
                    uuidFacturaCrm, data.getUuidCliente(), data.getCuit());
        }

        String cuit = this.resolverCuit(data.getCuit(), company);
        BigDecimal importe = this.resolverImporte(data);
        Factura factura = Factura.crear(company, data.getUuidCliente(), cuit, uuidFacturaCrm,
                data.getCodigosPlanes(), data.getCodigosProductos(), importe);
        this.facturaRepository.save(factura);

        log.info("FacturaService | registrarDeudaGenerada | factura creada | uuidFacturaCrm={} "
                + "| companyId={} | codigoPlan={} | vencimiento={} | importe={}",
                uuidFacturaCrm, company != null ? company.getId() : null, factura.getCodigoPlan(),
                factura.getFechaVencimiento(), factura.getImporte());
    }

    /**
     * Descarta (baja logica) una factura que el CRM informo y despues deshizo: el CRM ya no reconoce el
     * evento que la creo. Solo se toca si sigue PENDIENTE; si ya se pago o vencio, se deja como esta.
     */
    @Transactional
    public void descartarFacturaInexistenteEnCrm(String uuidFacturaCrm, String eventId) {
        this.facturaRepository.findFirstByUuidFacturaCrm(uuidFacturaCrm).ifPresent(factura -> {
            if (factura.getEstado() != FacturaEstadoEnum.PENDIENTE) {
                log.warn("FacturaService | descartarFacturaInexistenteEnCrm | la factura no esta pendiente, "
                        + "no se descarta | uuidFacturaCrm={} | estado={} | eventId={}",
                        uuidFacturaCrm, factura.getEstado(), eventId);
                return;
            }
            factura.setStatus(-1);
            this.facturaRepository.save(factura);
            log.info("FacturaService | descartarFacturaInexistenteEnCrm | factura descartada | uuidFacturaCrm={} "
                    + "| eventId={}", uuidFacturaCrm, eventId);
        });
    }

    /**
     * Asocia al club recien dado de alta las facturas que el CRM informo para su cliente antes de
     * que existiera, buscandolas por uuidCliente o por CUIT.
     * <p>
     * No toca el acceso del club: si alguna de esas facturas ya esta vencida, lo bloquea el job
     * de verificacion en su proxima corrida.
     * <p>
     * Espera: el id del club, su uuidCliente (null si no lo tiene) y el CUIT con el que se
     * registro (con o sin guiones).
     * Responde: nada. Si el club no existe o no esta activo, no asocia nada.
     */
    @Transactional
    public void asociarFacturasSinClub(Long companyId, String uuidCliente, String cuit) {
        String cuitNormalizado = Factura.normalizarCuit(cuit);
        if (cuitNormalizado == null && (uuidCliente == null || uuidCliente.isBlank())) {
            return;
        }

        Company company = this.companyRepository.findById(companyId).orElse(null);
        if (company == null || company.getStatus() == null || company.getStatus() != STATUS_ACTIVE) {
            log.warn("FacturaService | asociarFacturasSinClub | club inexistente o inactivo, no se asocian "
                    + "facturas | companyId={}", companyId);
            return;
        }

        List<Factura> facturas = this.facturaRepository.buscarSinClubPorClienteOCuit(
                uuidCliente, cuitNormalizado, STATUS_ACTIVE);
        if (facturas.isEmpty()) return;

        List<Factura> asociadas = facturas
                .stream()
                .filter(f -> f.asociarCompany(company))
                .collect(Collectors.toList());

        if (asociadas.size() != facturas.size()) {
            log.warn("FacturaService | asociarFacturasSinClub | algunas facturas no se pudieron asociar "
                    + "| companyId={} | encontradas={} | asociadas={}", companyId, facturas.size(), asociadas.size());
        }

        if (asociadas.isEmpty()) return;

        this.facturaRepository.saveAll(asociadas);

        log.info("FacturaService | asociarFacturasSinClub | facturas asociadas | companyId={} | cantidad={}",
                companyId, asociadas.size());
    }

    /**
     * Busca el club activo del cliente: primero por uuidCliente y, si no viene o no hay club con
     * ese uuid, por CUIT. Responde null si no encuentra ninguno.
     */
    private Company buscarCompanyActiva(String uuidCliente, String cuit) {
        if (uuidCliente != null && !uuidCliente.isBlank()) {
            Company porUuidCliente = this.companyRepository.findFirstByUuidClienteAndStatus(uuidCliente, STATUS_ACTIVE);
            if (porUuidCliente != null) {
                return porUuidCliente;
            }
        }
        if (cuit == null || cuit.isBlank()) {
            return null;
        }
        return this.companyRepository.findFirstByCuitAndStatus(cuit, STATUS_ACTIVE);
    }

    /**
     * Igual que buscarCompanyActiva pero sin filtrar por status: el pago tiene que encontrar al
     * club aunque este dado de baja.
     */
    private Company buscarCompany(String uuidCliente, String cuit) {
        if (uuidCliente != null && !uuidCliente.isBlank()) {
            Company porUuidCliente = this.companyRepository.findFirstByUuidCliente(uuidCliente);
            if (porUuidCliente != null) {
                return porUuidCliente;
            }
        }
        if (cuit == null || cuit.isBlank()) {
            return null;
        }
        return this.companyRepository.findFirstByCuit(cuit);
    }

    /**
     * CUIT con el que se guarda la factura: el del webhook si vino (eventos anteriores al cambio
     * del CRM) y, si no, el del club. Null si no hay ninguno de los dos.
     */
    private String resolverCuit(String cuitWebhook, Company company) {
        if (cuitWebhook != null && !cuitWebhook.isBlank()) {
            return cuitWebhook;
        }
        if (company == null) {
            return null;
        }
        return company.getCuit();
    }

    /**
     * Resuelve el importe de la factura.
     * <p>
     * Usa el importeTotal que informa el CRM, que ya incluye el descuento de la suscripcion. Solo
     * si no viene (eventos emitidos antes de que el CRM lo informara) lo calcula a partir de los
     * precios de los planes y productos sincronizados, que no contemplan descuentos.
     */
    private BigDecimal resolverImporte(CrmDeudaGeneradaWebhookDTO data) {
        if (data.getImporteTotal() != null) {
            return data.getImporteTotal();
        }
        log.warn("FacturaService | resolverImporte | el CRM no informo importeTotal, se calcula por codigos "
                + "| uuidFacturaCrm={}", data.getUuidFactura());
        return this.calcularImportePorCodigos(data.getCodigosPlanes(), data.getCodigosProductos());
    }

    /**
     * Suma el precio del plan (si hay uno) mas el precio de cada producto que origino la deuda,
     * con el IVA ya aplicado cuando corresponde (PlanCrm.precioFinal/ProductoCrm.precioFinal).
     * Los codigos que todavia no estan sincronizados en plan_crm/productos_crm se ignoran (con
     * un warning) en vez de hacer fallar el calculo entero.
     * <p>
     * Responde: la suma encontrada, o null si no se pudo resolver el precio de ningun codigo.
     */
    private BigDecimal calcularImportePorCodigos(List<String> codigosPlanes, List<String> codigosProductos) {
        BigDecimal total = BigDecimal.ZERO;
        boolean encontroAlgo = false;

        String codigoPlan = Factura.primerCodigoPlan(codigosPlanes);
        if (codigoPlan != null) {
            BigDecimal precioPlan = this.planCrmRepository.findFirstByCodigoAndStatus(codigoPlan, STATUS_ACTIVE)
                    .map(PlanCrm::precioFinal)
                    .orElse(null);
            if (precioPlan != null) {
                total = total.add(precioPlan);
                encontroAlgo = true;
            } else {
                log.warn("FacturaService | calcularImportePorCodigos | plan no sincronizado en plan_crm todavia "
                        + "| codigoPlan={}", codigoPlan);
            }
        }

        if (codigosProductos != null) {
            for (String codigoProducto : codigosProductos) {
                if (codigoProducto == null || codigoProducto.isBlank()) {
                    continue;
                }
                BigDecimal precioProducto = this.productoCrmRepository
                        .findFirstByCodigoAndStatus(codigoProducto.trim(), STATUS_ACTIVE)
                        .map(ProductoCrm::precioFinal)
                        .orElse(null);
                if (precioProducto != null) {
                    total = total.add(precioProducto);
                    encontroAlgo = true;
                } else {
                    log.warn("FacturaService | calcularImportePorCodigos | producto no sincronizado en "
                            + "productos_crm todavia | codigoProducto={}", codigoProducto);
                }
            }
        }

        return encontroAlgo ? total : null;
    }

    /**
     * Marca como pagada la factura a la que el CRM imputo un pago.
     * <p>
     * Espera: los datos del webhook PAGO_IMPUTADO.
     * Responde: nada. Si no aparece el club o la factura, lanza: el ACK sale con error y el CRM
     * reintenta. Eso resuelve solo el caso de que el pago llegue antes que la deuda que lo
     * origino, porque para el proximo reintento la factura ya va a estar registrada.
     */
    @Transactional
    public void registrarPagoImputado(CrmPagoImputadoWebhookDTO data) {
        String uuidFacturaCrm = data.getUuidFactura();

        // El club se busca sin filtrar por status a proposito: mas adelante va a ser el que
        // defina si tiene acceso, y para eso hace falta encontrarlo aunque este dado de baja
        Company company = this.buscarCompany(data.getUuidCliente(), data.getCuit());

        Factura factura = this.facturaRepository.findFirstByUuidFacturaCrm(uuidFacturaCrm)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND,
                        "No se encontró la factura del pago informado por el CRM",
                        "FacturaService | registrarPagoImputado | uuidFacturaCrm: " + uuidFacturaCrm));

        factura.marcarPagada();
        this.facturaRepository.save(factura);

        if (company == null) {
            log.warn("FacturaService | registrarPagoImputado | club inexistente, no se registra movimiento "
                    + "| uuidFacturaCrm={} | uuidCliente={} | cuit={}",
                    uuidFacturaCrm, data.getUuidCliente(), data.getCuit());
            return;
        }
        
        this.registrarMovimientoSaliente(company, factura);

        // Desbloqueo inmediato: si esta era la ultima factura vencida del club, no hace falta
        // esperar al job de vencimiento para restablecerle el acceso
        boolean quedanVencidas = this.facturaRepository.existsByCompanyAndEstadoAndStatus(
                company, FacturaEstadoEnum.VENCIDA, STATUS_ACTIVE);
        if (!quedanVencidas) {
            this.companyAccesoService.desbloquear(company);
        }

        log.info("FacturaService | registrarPagoImputado | factura marcada como pagada "
                + "| uuidFacturaCrm={} | companyId={}", uuidFacturaCrm, company.getId());
    }

    /**
     * Registra en la tesoreria del club un egreso por el importe de la factura pagada.
     * <p>
     * No bloquea el contrato principal del webhook: si no hay importe (no se pudo resolver el
     * precio al crear la factura) o el club no tiene ADMIN al que atribuirle el movimiento
     * (Movimiento.registradoPor es obligatorio y este flujo no tiene un usuario logueado), se
     * omite con un warning en vez de hacer fallar el pago.
     */
    private void registrarMovimientoSaliente(Company company, Factura factura) {
        if (factura.getImporte() == null) {
            log.warn("FacturaService | registrarMovimientoSaliente | factura sin importe, no se genera "
                    + "movimiento | uuidFacturaCrm={}", factura.getUuidFacturaCrm());
            return;
        }
        User admin = this.resolverAdmin(company);
        if (admin == null) {
            log.warn("FacturaService | registrarMovimientoSaliente | club sin ADMIN, no se genera movimiento "
                    + "| companyId={}", company.getId());
            return;
        }

        Movimiento movimiento = new Movimiento();
        movimiento.setCompany(company);
        movimiento.setTipo(MovimientoTipoEnum.EGRESO);
        movimiento.setEstado(MovimientoEstadoEnum.ACTIVO);
        movimiento.setMonto(factura.getImporte());
        movimiento.setFecha(LocalDate.now());
        movimiento.setConcepto("Pago de suscripción — factura " + factura.getUuid());
        movimiento.setRegistradoPor(admin);
        this.movimientoRepository.save(movimiento);
    }

    private User resolverAdmin(Company company) {
        return this.userCompanyRoleRepository.findByCompanyAndRoleName(company, "ADMIN").stream()
                .findFirst()
                .map(UserCompanyRole::getUser)
                .orElse(null);
    }

    /**
     * Crea en el CRM el pago de una o mas facturas del club y devuelve la preferencia de
     * Mercado Pago tal cual la responde el CRM.
     * <p>
     * Espera: la company que resolvio el controller y los uuids locales de las facturas a pagar.
     * Responde: la respuesta del CRM sin transformar. Lanza si alguna factura no existe o no es
     * del club (la busqueda ya filtra por company, asi que ninguna de otro club aparece).
     */
    public MercadoPagoPreferenceResponseDTO crearPago(Company company, FacturaPagoRequestDTO request) {
        if (request.getFacturasUuid() == null || request.getFacturasUuid().isEmpty()) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Debe indicar al menos una factura",
                    "FacturaService | crearPago | facturasUuid vacio");
        }

        List<Factura> facturas = this.facturaRepository.findByUuidInAndCompanyAndStatus(
                request.getFacturasUuid(), company, STATUS_ACTIVE);
        if (facturas.size() != request.getFacturasUuid().size()) {
            throw new CustomException(HttpStatus.NOT_FOUND, "No se encontraron todas las facturas indicadas",
                    "FacturaService | crearPago | companyId: " + company.getId()
                            + " | solicitadas: " + request.getFacturasUuid().size()
                            + " | encontradas: " + facturas.size());
        }

        String baseUrl = this.trimTrailingSlash(this.frontUrlProperties.getUrl());

        PagoIntegracionRequestDTO pagoRequest = new PagoIntegracionRequestDTO();
        pagoRequest.setFacturasUuid(facturas.stream().map(Factura::getUuidFacturaCrm).toList());
        pagoRequest.setCuit(company.getCuit());
        pagoRequest.setReferencia(request.getReferencia());
        pagoRequest.setFechaPago(request.getFechaPago());
        pagoRequest.setUrlExito(baseUrl + "/pago-factura/exito");
        pagoRequest.setUrlError(baseUrl + "/pago-factura/fallo");
        pagoRequest.setUrlPendiente(baseUrl + "/pago-factura/pendiente");

        return this.crmIntegracionService.crearPago(pagoRequest);
    }

    // Mismo criterio que MercadoPagoPreferenceBuilder: evita el doble slash si front.url ya
    // termina en "/"
    private String trimTrailingSlash(String url) {
        if (url == null || url.isBlank()) {
            return "";
        }
        String trimmed = url.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }
}
