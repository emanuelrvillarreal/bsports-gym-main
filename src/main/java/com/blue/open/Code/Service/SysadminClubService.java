package com.blue.open.Code.Service;

import com.blue.open.Code.Entity.DTO.ClubAdminRequestDTO;
import com.blue.open.Code.Entity.DTO.ClubRegistrationRequestDTO;
import com.blue.open.Code.Entity.DTO.ClubRegistrationResponseDTO;
import com.blue.open.Code.Entity.DTO.CompanyDTO;
import com.blue.open.Code.Entity.DTO.UserDTO;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.CompanyBloqueoAcceso;
import com.blue.open.Code.Entity.Db.CompanyMercadoPago;
import com.blue.open.Code.Entity.Db.User;
import com.blue.open.Code.Event.ClubRegistradoEvent;
import com.blue.open.Code.Mapper.CompanyMapper;
import com.blue.open.Code.Mapper.UserMapper;
import com.blue.open.Code.Repository.CompanyRepository;
import com.blue.open.Code.Service.CuotaSocialRango.CuotaSocialRangoDefaultService;
import com.blue.open.Core.Exceptions.CustomException;
import com.blue.open.Crm.Entity.DTO.Response.CrmClienteResponseDTO;
import com.blue.open.Crm.Entity.DTO.Response.CrmCondicionImpositivaResponseDTO;
import com.blue.open.Crm.Entity.DTO.Response.CrmPlanResponseDTO;
import com.blue.open.Crm.Entity.DTO.Response.CrmProvinciaResponseDTO;
import com.blue.open.Crm.Entity.DTO.Response.CrmSuscripcionActivaDTO;
import com.blue.open.Crm.Service.CrmIntegracionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SysadminClubService {

    private static final int STATUS_ACTIVE = 0;
    private static final int STATUS_DELETED = -1;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private CompanyMapper companyMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ClubRoleProvisioningService clubRoleProvisioningService;

    @Autowired
    private ClubCrmSincronizacionService clubCrmSincronizacionService;

    @Autowired
    private ClubSuscripcionInicialService clubSuscripcionInicialService;

    @Autowired
    private CompanyAccesoService companyAccesoService;

    @Autowired
    private CrmIntegracionService crmIntegracionService;

    @Autowired
    private ClubAdminService clubAdminService;

    @Autowired
    private CuotaSocialRangoDefaultService cuotaSocialRangoDefaultService;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    /** Catálogo de planes del CRM, para completar codigoPlan en el alta/suscripción de un club. */
    public List<CrmPlanResponseDTO> obtenerPlanesCrm() {
        return crmIntegracionService.obtenerPlanes();
    }

    /** Catálogo de provincias del CRM, para completar codigoProvincia en el alta de un club. */
    public List<CrmProvinciaResponseDTO> obtenerProvinciasCrm() {
        return crmIntegracionService.obtenerProvincias();
    }

    /**
     * Catálogo de condiciones impositivas del CRM, para completar codigoCondicionImpositiva en
     * el alta de un club.
     */
    public List<CrmCondicionImpositivaResponseDTO> obtenerCondicionesImpositivasCrm() {
        return crmIntegracionService.obtenerCondicionesImpositivas();
    }

    @Transactional
    public void syncRolesForAllActiveCompanies() {
        clubRoleProvisioningService.syncAllActiveCompanies();
    }

    /**
     * Da de alta el club: lo persiste localmente con su admin y lo registra como cliente del CRM.
     * <p>
     * Todo corre en una transaccion: si el CRM rechaza el club (CUIT invalido, condicion
     * impositiva o provincia inexistentes, etc.) la excepcion propaga y no queda nada persistido.
     * <p>
     * No lo suscribe a ningun plan: el alta y la suscripcion son dos requests separados, y el
     * segundo es POST /sysadmin/clubs/{companyUuid}/crm/suscribir (SysadminSuscripcionService).
     * Hasta que el front haga ese segundo request el club queda registrado y sin plan, o sea sin
     * cupos de socios: una suscripcion es una llamada HTTP al CRM que esta transaccion no podria
     * revertir, asi que no tiene por que compartirla.
     * <p>
     * Lo unico que si hace, ya terminado el registro, es preguntarle al CRM si el club tiene
     * planes: espeja el que tenga y le resuelve el acceso segun el estado de esa suscripcion. Ver
     * sincronizarSuscripcionDelCrm.
     */
    @Transactional
    public ClubRegistrationResponseDTO registerClub(ClubRegistrationRequestDTO request) {
        Company existingCompanyByCuit = companyRepository.findFirstByCuitAndStatus(request.getCuit().trim(), STATUS_ACTIVE);
        if (existingCompanyByCuit != null) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "CUIT ya registrado",
                    "Ya existe un club activo registrado con el CUIT: " + request.getCuit().trim());
        }

        Company existingByMailCompany = companyRepository.findFirstByEmailInstitucionalIgnoreCaseAndStatus(request.getEmailInstitucional(), STATUS_ACTIVE);
        if (existingByMailCompany != null) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Email ya registrado",
                    "Ya existe un club activo registrado con el email: " + request.getEmailInstitucional().trim());
        }

        Company company = saveCompany(request);
        clubRoleProvisioningService.provisionRoles(company);
        cuotaSocialRangoDefaultService.asegurarDefault(company);

        ClubAdminRequestDTO adminRequest = request.getAdmin();
        User admin = clubAdminService.crearAdmin(company, adminRequest.getEmail(),
                adminRequest.getNombre().trim(), adminRequest.getApellido().trim(), adminRequest.getPassword());

        // Registro del cliente en el CRM. Corre dentro de la transaccion a proposito: si el CRM
        // lo rechaza, la excepcion propaga y revierte el alta completa del club
        CrmClienteResponseDTO clienteCrm = clubCrmSincronizacionService.registrarCliente(request);
        company = guardarUuidCliente(company, clienteCrm);

        sincronizarSuscripcionDelCrm(company);

        // Se procesa despues del commit: si el alta se revierte, no hay facturas que asociar
        eventPublisher.publishEvent(
                new ClubRegistradoEvent(company.getId(), company.getUuidCliente(), company.getCuit()));

        ClubRegistrationResponseDTO response = new ClubRegistrationResponseDTO();
        response.setCompany(companyMapper.modelToDto(company));
        UserDTO adminDto = userMapper.modelToDto(admin);
        adminDto.setPassword(null);
        response.setAdmin(adminDto);
        return response;
    }

    /**
     * Ya terminado el registro, resuelve con que suscripcion arranca el club y si puede operar.
     * <p>
     * Si el CRM le conoce un plan al CUIT — el caso de uno que ya era cliente, donde el alta
     * responde CLIENTE_EXISTENTE — lo espeja en company_suscripcion_crm para que el club arranque
     * con los cupos de ese plan.
     * <p>
     * Si no le conoce ninguno, que es lo normal en un alta, le bloquea el acceso: sin suscripcion
     * no hay cupos ni plan que lo habilite a operar, asi que nadie del club deberia poder entrar
     * hasta que se lo suscriba con el segundo request.
     */
    private void sincronizarSuscripcionDelCrm(Company company) {
        CrmSuscripcionActivaDTO suscripcion = clubSuscripcionInicialService.buscarSuscripcionEnCrm(company.getCuit());
        if (suscripcion == null) {
            companyAccesoService.bloquear(company);
            return;
        }

        clubSuscripcionInicialService.aplicarSuscripcion(company, suscripcion);
    }

    private Company saveCompany(ClubRegistrationRequestDTO request) {
        String cuit = request.getCuit().trim();
        String emailInstitucional = request.getEmailInstitucional().trim();

        Company company = resolveCompanyForSave(cuit, emailInstitucional);

        CompanyDTO companyDTO = companyMapper.profileFieldsToDto(request);
        companyDTO.setAdminCreado(Boolean.TRUE);
        if (company.getId() != null && company.getStatus() != null) {
            companyDTO.setStatus(company.getStatus());
        }

        company = companyMapper.dtoToModel(companyDTO, company);
        company.setStatus(STATUS_ACTIVE);
        ensureMercadoPagoDefault(company);
        ensureBloqueoAccesoDefault(company);
        return companyRepository.save(company);
    }

    /**
     * Guarda en el club el uuid con el que el CRM lo identifica, tanto si lo acaba de crear como
     * si ya existia (CLIENTE_EXISTENTE). Es la clave con la que llegan sus webhooks.
     */
    private Company guardarUuidCliente(Company company, CrmClienteResponseDTO clienteCrm) {
        if (clienteCrm == null || clienteCrm.getUuidCliente() == null || clienteCrm.getUuidCliente().isBlank()) {
            return company;
        }
        company.setUuidCliente(clienteCrm.getUuidCliente().trim());
        return companyRepository.save(company);
    }

    private void ensureMercadoPagoDefault(Company company) {
        if (company.getMercadoPago() != null) {
            return;
        }
        company.setMercadoPago(CompanyMercadoPago.crearDeshabilitadoVacias(company));
    }

    private void ensureBloqueoAccesoDefault(Company company) {
        if (company.getBloqueoAcceso() != null) {
            return;
        }
        company.setBloqueoAcceso(CompanyBloqueoAcceso.crearDesbloqueada(company));
    }

    /**
     * Reutiliza un club dado de baja (status -1) si coincide CUIT o email institucional.
     * Si hay dos eliminados distintos para esos datos, no se puede decidir cuál reactivar.
     */
    private Company resolveCompanyForSave(String cuit, String emailInstitucional) {
        Company deletedByCuit = companyRepository.findFirstByCuitAndStatus(cuit, STATUS_DELETED);
        Company deletedByEmail = companyRepository.findFirstByEmailInstitucionalIgnoreCaseAndStatus(
                emailInstitucional, STATUS_DELETED);

        if (deletedByCuit != null && deletedByEmail != null && !sameCompany(deletedByCuit, deletedByEmail)) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Datos en conflicto",
                    "Existen clubes eliminados distintos para el CUIT y el email institucional indicados.");
        }

        if (deletedByCuit != null) {
            return deletedByCuit;
        }
        if (deletedByEmail != null) {
            return deletedByEmail;
        }
        return new Company();
    }

    private boolean sameCompany(Company a, Company b) {
        if (a.getId() != null && b.getId() != null) {
            return a.getId().equals(b.getId());
        }
        return a.getUuid() != null && a.getUuid().equals(b.getUuid());
    }

}
