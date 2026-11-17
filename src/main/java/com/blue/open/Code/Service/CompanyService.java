package com.blue.open.Code.Service;

import com.blue.open.Code.Auth.RoleEnum;
import com.blue.open.Code.Entity.DTO.ClubProfileUpdateRequestDTO;
import com.blue.open.Code.Entity.DTO.CompanyDTO;
import com.blue.open.Code.Entity.DTO.CompanyPaginateRequestDTO;
import com.blue.open.Code.Entity.DTO.CompanyPublicDTO;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.CompanySuscripcionCrm;
import com.blue.open.Code.Entity.Db.Factura;
import com.blue.open.Code.Entity.Db.User;
import com.blue.open.Code.Event.ClubRegistradoEvent;
import com.blue.open.Code.Mapper.CompanyMapper;
import com.blue.open.Code.Repository.CompanyRepository;
import com.blue.open.Code.Repository.CompanySuscripcionCrmRepository;
import com.blue.open.Code.Repository.FacturaRepository;
import com.blue.open.Code.Service.CuotaSocialRango.CuotaSocialRangoDefaultService;
import com.blue.open.Core.Entity.DTO.DeleteArchivoRequest;
import com.blue.open.Core.Auth.BaseRoleEnum;
import com.blue.open.Core.Exceptions.CustomException;
import com.blue.open.Core.Service.BaseCompanyService;
import com.blue.open.Core.Service.SM3Service;
import com.blue.open.Crm.Entity.DTO.Response.CrmSuscripcionActivaDTO;
import com.blue.open.Crm.Entity.DTO.Webhook.CrmAltaClienteWebhookDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Service
public class CompanyService extends BaseCompanyService<CompanyDTO> {

    private static final int STATUS_ACTIVE = 0;
    private static final int MAX_PAGE_SIZE = 100;

    @Autowired
    private CuitValidatorService cuitValidatorService;

    @Autowired
    private SM3Service sm3Service;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private CompanyMapper companyMapper;

    @Autowired
    private CompanySuscripcionCrmRepository companySuscripcionCrmRepository;

    @Autowired
    private FacturaRepository facturaRepository;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private ClubRoleProvisioningService clubRoleProvisioningService;

    @Autowired
    private ClubSuscripcionInicialService clubSuscripcionInicialService;

    @Autowired
    private ClubAdminService clubAdminService;

    @Autowired
    private CuotaSocialRangoDefaultService cuotaSocialRangoDefaultService;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Transactional(readOnly = true)
    public CompanyPublicDTO findByPath(String path) {
        if (path == null || path.isBlank()) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Path requerido", "El path no puede estar vacío.");
        }
        Company company = companyRepository.findByPath(path.trim());
        if (company == null) {
            throw new CustomException(HttpStatus.NOT_FOUND, "Gimnasio no encontrado",
                    "No existe un club con el path: " + path.trim());
        }
        return companyMapper.modelToPublicDto(company);
    }

    @Transactional(readOnly = true)
    public CompanyDTO getCurrentCompany(User user) {
        Company company = authService.getCurrentCompanyOrThrow();
        CompanyDTO dto = mapper.modelToDto(company);
        if (esSoloSocioFamiliarOProfesor(user)) {
            return soloColoresYLogo(dto);
        }
        return dto;
    }

    private boolean esSoloSocioFamiliarOProfesor(User user) {
        return authService.hasAnyRoleOrPermission(user,
                List.of(RoleEnum.SOCIO_TITULAR, RoleEnum.FAMILIAR, RoleEnum.PROFESOR))
                && !authService.hasAnyRoleOrPermission(user, List.of(BaseRoleEnum.ADMIN, BaseRoleEnum.SYSADMIN));
    }

    private CompanyDTO soloColoresYLogo(CompanyDTO dto) {
        CompanyDTO limitado = new CompanyDTO();
        limitado.setLogoUrl(dto.getLogoUrl());
        limitado.setColorPrimario(dto.getColorPrimario());
        limitado.setColorSecundario(dto.getColorSecundario());
        return limitado;
    }

    @Transactional(readOnly = true)
    public Page<CompanyDTO> paginar(CompanyPaginateRequestDTO request) {
        CompanyPaginateRequestDTO req = request != null ? request : new CompanyPaginateRequestDTO();
        int page = Math.max(req.getPage(), 1);
        int size = req.getSize() <= 0 ? 10 : Math.min(req.getSize(), MAX_PAGE_SIZE);
        String search = (req.getSearch() == null || req.getSearch().isBlank())
                ? null
                : "%" + req.getSearch().trim().toLowerCase() + "%";

        Pageable pageable = PageRequest.of(page - 1, size);
        Page<Company> companyPage = companyRepository.paginate(search, pageable);

        List<Long> ids = companyPage.getContent().stream().map(Company::getId).collect(Collectors.toList());

        Map<Long, CompanySuscripcionCrm> suscripcionesPorCompanyId = suscripcionesPorCompanyId(ids);
        Map<Long, Factura> ultimaFacturaPorCompanyId = ultimaFacturaPorCompanyId(ids);

        List<CompanyDTO> dtos = companyPage.getContent().stream()
                .map(c -> {
                    CompanyDTO dto = mapper.modelToDto(c);
                    dto.setSuscripcionCrm(companyMapper.suscripcionCrmToDto(suscripcionesPorCompanyId.get(c.getId())));
                    Factura ultimaFactura = ultimaFacturaPorCompanyId.get(c.getId());
                    if (ultimaFactura != null) {
                        dto.setUltimaFacturaEstado(ultimaFactura.getEstado());
                        dto.setUltimaFacturaFechaGeneracion(ultimaFactura.getFechaGeneracion());
                        dto.setUltimaFacturaFechaVencimiento(ultimaFactura.getFechaVencimiento());
                    }
                    return dto;
                })
                .collect(Collectors.toList());

        return new PageImpl<>(dtos, pageable, companyPage.getTotalElements());
    }

    private Map<Long, CompanySuscripcionCrm> suscripcionesPorCompanyId(List<Long> companyIds) {
        if (companyIds.isEmpty()) {
            return Map.of();
        }
        return companySuscripcionCrmRepository.findByCompanyIdInWithPlan(companyIds, STATUS_ACTIVE).stream()
                .collect(Collectors.toMap(csc -> csc.getCompany().getId(), Function.identity()));
    }

    /**
     * Resuelve la ultima factura de cada club en dos queries: un GROUP BY que da el id mas
     * reciente por company, y una segunda query (con company ya inicializada via JOIN FETCH) que
     * trae esas entidades completas. Evita el N+1 de pedirla club por club en el paginado.
     */
    private Map<Long, Factura> ultimaFacturaPorCompanyId(List<Long> companyIds) {
        if (companyIds.isEmpty()) {
            return Map.of();
        }
        List<Long> ultimaFacturaIds = facturaRepository.findUltimaFacturaIdPorCompanyIds(companyIds, STATUS_ACTIVE);
        if (ultimaFacturaIds.isEmpty()) {
            return Map.of();
        }
        return facturaRepository.findByIdInWithCompany(ultimaFacturaIds).stream()
                .collect(Collectors.toMap(factura -> factura.getCompany().getId(), Function.identity()));
    }

    public CompanyDTO uploadLogo(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Archivo requerido",
                    "Debe enviar un archivo de imagen para el logo.");
        }

        Company company = authService.getCurrentCompanyOrThrow();
        deleteLogoFromStorage(company.getLogoUrl());

        String logoUrl = sm3Service.upload(file);
        company.setLogoUrl(logoUrl);
        company = companyRepository.save(company);
        return mapper.modelToDto(company);
    }

    public CompanyDTO deleteLogo() {
        Company company = authService.getCurrentCompanyOrThrow();
        deleteLogoFromStorage(company.getLogoUrl());
        company.setLogoUrl(null);
        company = companyRepository.save(company);
        return mapper.modelToDto(company);
    }

    public CompanyDTO updateClubProfile(ClubProfileUpdateRequestDTO request) {
        Company company = authService.getCurrentCompanyOrThrow();
        String cuit = request.getCuit().trim();

        Company existingByCuit = companyRepository.findFirstByCuitAndStatus(cuit, STATUS_ACTIVE);
        if (existingByCuit != null && !sameCompany(existingByCuit, company)) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "CUIT ya registrado",
                    "Ya existe un club activo registrado con el CUIT: " + cuit);
        }

        String logoUrl = company.getLogoUrl();

        CompanyDTO companyDTO = companyMapper.profileFieldsToDto(request);
        companyDTO.setStatus(company.getStatus());
        companyDTO.setAdminCreado(company.getAdminCreado());
        company = mapper.dtoToModel(companyDTO, company);
        company.setLogoUrl(logoUrl);

        return mapper.modelToDto(companyRepository.save(company));
    }

    /**
     * Da de alta el club del cliente que informa el CRM en ALTA_CLIENTE y lo deja listo para
     * operar: club parcial, admin, suscripcion espejada desde el CRM y facturas previas asociadas.
     * <p>
     * Cada paso corre en su propia transaccion (TransactionTemplate), para que una falla posterior
     * no revierta el club ya creado:
     * <ol>
     *     <li>Crea el club parcial (solo uuidCliente y datos fiscales, onboarding pendiente) con sus
     *     roles. Si falla, lanza y no sigue.</li>
     *     <li>Crea el admin (email = correo de facturacion, contraseña = CUIT). Si falla, se loguea
     *     y se sigue: el club queda con adminCreado = false.</li>
     *     <li>Consulta en el CRM la suscripcion del cliente, la espeja, le resuelve el acceso y
     *     publica ClubRegistradoEvent. Si algo falla, se loguea y se corta sin lanzar: un
     *     reintento del webhook chocaria con el club ya existente.</li>
     * </ol>
     * Sin transaccion propia (NOT_SUPPORTED): la clase es transaccional por BaseCompanyService y
     * la consulta al CRM no tiene que quedar dentro de una transaccion abierta.
     * <p>
     * Espera: los datos del webhook ALTA_CLIENTE.
     * Responde: el club creado. Lanza si ya existe un club con ese uuidCliente o CUIT, o si falta
     * algun dato obligatorio.
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public Company registrarAltaCliente(CrmAltaClienteWebhookDTO data) {
        Company company = transactionTemplate.execute(status -> crearClubDeAltaCliente(data));

        crearAdminDeAltaCliente(company, data);
        sincronizarSuscripcionDeAltaCliente(company);
        return company;
    }

    private Company crearClubDeAltaCliente(CrmAltaClienteWebhookDTO data) {
        validarClienteNoRegistrado(data.getUuidCliente(), data.getCuit());

        Company company = Company.crearDesdeAltaCliente(data.getUuidCliente(), data.getRazonSocial(),
                data.getCuit(), data.getCorreoFacturacion());
        String pathDisponible = resolverPathDisponible(company.getPath());
        company.setPath(pathDisponible);
        company = companyRepository.save(company);
        clubRoleProvisioningService.provisionRoles(company);
        cuotaSocialRangoDefaultService.asegurarDefault(company);
        return company;
    }

    /** Crea el admin del club en su propia transaccion. Si falla, el club queda sin admin. */
    private void crearAdminDeAltaCliente(Company company, CrmAltaClienteWebhookDTO data) {
        try {
            clubAdminService.crearAdminDeAltaCliente(company.getId(), data.getCorreoFacturacion(),
                    data.getRazonSocial(), data.getCuit());
        } catch (Exception e) {
            Logger.getLogger(CompanyService.class.getName()).log(Level.SEVERE,
                    "CompanyService | crearAdminDeAltaCliente | club creado sin admin | companyId="
                            + company.getId() + " | email=" + data.getCorreoFacturacion(), e);
        }
    }

    /**
     * Espeja la suscripcion que el CRM le conoce al club y le asocia las facturas previas. La
     * consulta al CRM corre fuera de transaccion. Si el CRM no informa ninguna suscripcion o algo
     * falla, se loguea y se corta: el club ya quedo creado.
     */
    private void sincronizarSuscripcionDeAltaCliente(Company company) {
        try {
            CrmSuscripcionActivaDTO suscripcion = clubSuscripcionInicialService
                    .buscarSuscripcionEnCrm(company.getCuit());
            if (suscripcion == null) {
                Logger.getLogger(CompanyService.class.getName()).warning(
                        "CompanyService | sincronizarSuscripcionDeAltaCliente | el CRM no informo ninguna "
                                + "suscripcion, el club queda sin suscripcion | companyId=" + company.getId());
                return;
            }

            transactionTemplate.executeWithoutResult(
                    status -> completarAltaCliente(company.getId(), suscripcion));
        } catch (Exception e) {
            Logger.getLogger(CompanyService.class.getName()).log(Level.SEVERE,
                    "CompanyService | sincronizarSuscripcionDeAltaCliente | club creado sin suscripcion "
                            + "| companyId=" + company.getId(), e);
        }
    }

    /**
     * Espeja la suscripcion, le resuelve el acceso al club y publica ClubRegistradoEvent. Corre
     * dentro del TransactionTemplate: el listener escucha AFTER_COMMIT y, sin transaccion activa,
     * Spring no lo ejecuta. Despues del commit le asocia al club las facturas previas.
     */
    private void completarAltaCliente(Long companyId, CrmSuscripcionActivaDTO suscripcion) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Gimnasio no encontrado",
                        "CompanyService | completarAltaCliente | companyId: " + companyId));

        clubSuscripcionInicialService.aplicarSuscripcion(company, suscripcion);

        eventPublisher.publishEvent(
                new ClubRegistradoEvent(company.getId(), company.getUuidCliente(), company.getCuit()));
    }

    private void validarClienteNoRegistrado(String uuidCliente, String cuit) {
        if (!companyRepository.existsByUuidClienteOrCuit(uuidCliente, cuit)) {
            return;
        }
        throw new CustomException(HttpStatus.CONFLICT, "El cliente ya tiene un gimnasio registrado",
                "CompanyService | registrarAltaCliente | club existente | uuidCliente: " + uuidCliente
                        + " | cuit: " + cuit);
    }

    /**
     * Devuelve el path base si esta libre; si no, el primero libre de path-2, path-3, …
     */
    private String resolverPathDisponible(String pathBase) {
        if (!companyRepository.existsByPath(pathBase)) {
            return pathBase;
        }

        int sufijo = 2;
        String candidato = pathBase + "-" + sufijo;
        while (companyRepository.existsByPath(candidato)) {
            sufijo++;
            candidato = pathBase + "-" + sufijo;
        }
        return candidato;
    }

    private boolean sameCompany(Company a, Company b) {
        if (a.getId() != null && b.getId() != null) {
            return a.getId().equals(b.getId());
        }
        return a.getUuid() != null && a.getUuid().equals(b.getUuid());
    }

    private void deleteLogoFromStorage(String logoUrl) {
        if (logoUrl == null || logoUrl.isBlank()) {
            return;
        }
        DeleteArchivoRequest request = new DeleteArchivoRequest();
        request.setUrl(logoUrl);
        try {
            sm3Service.delete(request);
        } catch (Exception e) {
            Logger.getLogger(CompanyService.class.getName()).log(Level.SEVERE, "Error al eliminar el logo del storage", e);
        }
    }

    @Override
    protected Company hookSaveMapperDtoToModel(CompanyDTO dto, Company model) {
        validateClubFields(dto);
        model = super.hookSaveMapperDtoToModel(dto, model);
        if (model.getAdminCreado() == null) {
            model.setAdminCreado(Boolean.TRUE);
        }
        return model;
    }

    private void validateClubFields(CompanyDTO dto) {
        if (dto.getCuit() != null && !dto.getCuit().isBlank()
                && !cuitValidatorService.isValid(dto.getCuit().trim())) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "CUIT inválido",
                    "El CUIT ingresado no es válido según su dígito verificador.");
        }
        validatePlazoPositivo(dto.getDiasVencimientoCuota(), "Días vencimiento de cuota");
        validatePlazoPositivo(dto.getDiasVencimientoMoroso(), "Días vencimiento/moroso");
        validatePlazoPositivo(dto.getDiasInhabilitacion(), "Días inhabilitación");
    }

    private void validatePlazoPositivo(Integer value, String nombreCampo) {
        if (value == null) {
            return;
        }
        if (value <= 0) {
            throw new CustomException(HttpStatus.BAD_REQUEST, nombreCampo + " inválido",
                    nombreCampo + " debe ser un entero positivo.");
        }
    }
}
