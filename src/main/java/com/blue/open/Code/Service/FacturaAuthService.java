package com.blue.open.Code.Service;

import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Repository.CompanyRepository;
import com.blue.open.Core.Exceptions.CustomException;
import com.blue.open.Core.Service.Auth.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * Resuelve la company sobre la que se consultan las facturas, segun el rol de quien pide.
 * <p>
 * Admin de club: la company sale del header X-Company-Uuid, via AuthService. Sysadmin: no tiene
 * una company propia asociada al header, asi que toma el uuid que llega en el body del request.
 */
@Service
public class FacturaAuthService {

    @Autowired
    private AuthService authService;

    @Autowired
    private CompanyRepository companyRepository;

    public Company resolveCompany(String companyUuid) {
        if (authService.isSysAdmin()) {
            if (companyUuid == null || companyUuid.isBlank()) {
                throw new CustomException(HttpStatus.BAD_REQUEST, "Company requerida",
                        "FacturaAuthService | resolveCompany | sysadmin sin companyUuid en el body");
            }
            Company company = companyRepository.findFirstByUuidWithStatusIsActive(companyUuid);
            if (company == null) {
                throw new CustomException(HttpStatus.NOT_FOUND, "Gimnasio no encontrado",
                        "FacturaAuthService | resolveCompany | companyUuid: " + companyUuid);
            }
            return company;
        }
        return authService.getCurrentCompanyOrThrow();
    }
}
