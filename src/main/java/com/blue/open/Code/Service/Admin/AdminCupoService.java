package com.blue.open.Code.Service.Admin;

import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Service.CrmPlanLimiteResolver;
import com.blue.open.Core.Exceptions.CustomException;
import com.blue.open.Core.Repository.UserCompanyRoleRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Corta el alta de un ADMIN adicional si supera el cupo del plan del club en el CRM.
 * <p>
 * El cupo sale del producto "bsports-admin" dentro de plan.productos[] (mismo mecanismo que
 * MemberCupoService usa para "bsports-socios", via CrmPlanLimiteResolver). Fail-closed: si no se
 * puede determinar el limite, se bloquea.
 */
@Service
@Slf4j
public class AdminCupoService {

    private static final String CODIGO_PRODUCTO_ADMINS = "bsports-admin";
    private static final String ROLE_NAME_ADMIN = "ADMIN";

    @Autowired
    private CrmPlanLimiteResolver crmPlanLimiteResolver;

    @Autowired
    private UserCompanyRoleRepository userCompanyRoleRepository;

    @Transactional(readOnly = true)
    public void validarCupoDisponible(Company company, int nuevosAdmins) {
        Integer limite = this.crmPlanLimiteResolver.resolverLimite(company, CODIGO_PRODUCTO_ADMINS)
                .orElse(null);
        if (limite == null) {
            throw new CustomException(HttpStatus.BAD_REQUEST,
                    "No se pudo determinar el límite de administradores del plan del gimnasio",
                    "AdminCupoService | companyId: " + company.getId() + " | sin limite configurado "
                            + "(CompanySuscripcionCrm/PlanCrm/producto bsports-admin)");
        }

        long actuales = this.userCompanyRoleRepository.findByCompanyAndRoleName(company, ROLE_NAME_ADMIN).size();
        if (actuales + nuevosAdmins > limite) {
            throw new CustomException(HttpStatus.BAD_REQUEST,
                    "El gimnasio alcanzó el límite de administradores de su plan",
                    "AdminCupoService | companyId: " + company.getId() + " | actuales: " + actuales
                            + " | nuevos: " + nuevosAdmins + " | limite: " + limite);
        }
    }
}
