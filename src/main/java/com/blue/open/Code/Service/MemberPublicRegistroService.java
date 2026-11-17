package com.blue.open.Code.Service;

import com.blue.open.Code.Entity.DTO.MemberDTO;
import com.blue.open.Code.Entity.DTO.MemberPublicRegistroCommand;
import com.blue.open.Code.Entity.DTO.MemberTitularAltaCommand;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Repository.CompanyRepository;
import com.blue.open.Core.Exceptions.CustomException;
import com.blue.open.Core.Service.Auth.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
public class MemberPublicRegistroService {

    @Autowired
    private AuthService authService;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private MemberTitularService memberTitularService;

    @Autowired
    private CompanyAccesoService companyAccesoService;

    @Transactional
    public MemberDTO registroTitular(String companyUuid, MemberPublicRegistroCommand command) {
        Company company = resolveCompanyOrThrow(companyUuid);
        validarAccesoNoBloqueado(company);
        MemberTitularAltaCommand altaCommand = toAltaCommand(command);
        return memberTitularService.registrarTitularPublicoEnClub(company, altaCommand);
    }

    /**
     * Corta el alta si el club tiene el acceso bloqueado, antes de crear el usuario y el socio.
     * <p>
     * Este endpoint es publico (sin JWT), asi que AccesoBloqueadoInterceptor no lo alcanza: no
     * puede resolver la company sin usuario autenticado y deja pasar el request. Se replica aca el
     * mismo criterio — el flag de CompanyBloqueoAcceso, que cubre tanto la factura vencida como la
     * suscripcion anulada — y el mismo formato de error, para que el front no tenga que distinguir
     * dos respuestas distintas para el mismo motivo.
     */
    private void validarAccesoNoBloqueado(Company company) {
        if (!companyAccesoService.estaBloqueada(company)) {
            return;
        }
        CustomException ex = new CustomException(HttpStatus.FORBIDDEN,
                "El gimnasio tiene facturación vencida",
                "MemberPublicRegistroService | companyId: " + company.getId());
        ex.newSubError("motivo", "FACTURA_VENCIDA",
                "Regularice el pago en el CRM para recuperar el acceso a la plataforma.");
        throw ex;
    }

    private Company resolveCompanyOrThrow(String companyUuid) {
        String resolved = authService.resolveCompanyUuid(companyUuid);
        if (resolved == null || resolved.isBlank()) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Company requerida",
                    "Falta el header X-Company-Uuid");
        }
        Company company = companyRepository.findFirstByUuidWithStatusIsActive(resolved);
        if (company == null) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Empresa no encontrada",
                    "Company not found for uuid: " + resolved);
        }
        return company;
    }

    private MemberTitularAltaCommand toAltaCommand(MemberPublicRegistroCommand command) {
        MemberTitularAltaCommand alta = new MemberTitularAltaCommand();
        alta.setNombre(command.getNombre());
        alta.setApellido(command.getApellido());
        alta.setDni(command.getDni());
        alta.setFechaNacimiento(command.getFechaNacimiento());
        alta.setSexo(command.getSexo());
        alta.setTelefono(command.getTelefono());
        alta.setEmail(command.getEmail());
        alta.setPassword(command.getPassword());
        alta.setFechaAlta(LocalDate.now());
        return alta;
    }
}
