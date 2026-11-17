package com.blue.open.Code.Service;

import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Repository.MemberRepository;
import com.blue.open.Core.Exceptions.CustomException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Corta el alta de un socio si supera el cupo del plan del club en el CRM (blue-crm).
 * <p>
 * El cupo sale del producto "bsports-socios" dentro de plan.productos[] (PlanCrm.productosJson,
 * sincronizado por CrmCatalogoSyncService o CrmWebhookProcesador.onPlanCambio): sus "unidades"
 * son la cantidad maxima de socios activos que el plan permite.
 * <p>
 * A diferencia del resto de la integracion CRM, este chequeo es "fail-closed": si no se puede
 * determinar el limite (sin CompanySuscripcionCrm, sin plan, o sin el producto), se bloquea en
 * vez de dejar pasar. Es un limite de capacidad paga, no un dato informativo.
 */
@Service
@Slf4j
public class MemberCupoService {

    private static final int STATUS_ACTIVE_MEMBER = MemberStatusConstants.STATUS_ACTIVE;
    private static final String CODIGO_PRODUCTO_SOCIOS = "bsports-socios";

    @Autowired
    private CrmPlanLimiteResolver crmPlanLimiteResolver;

    @Autowired
    private MemberRepository memberRepository;

    /**
     * Corta si agregar nuevosSocios superaria el limite del producto "bsports-socios" del plan
     * del club. Si no se puede determinar el limite, corta igual (ver clase).
     */
    @Transactional(readOnly = true)
    public void validarCupoDisponible(Company company, int nuevosSocios) {
        Integer limite = this.crmPlanLimiteResolver.resolverLimite(company, CODIGO_PRODUCTO_SOCIOS)
                .orElse(null);
        if (limite == null) {
            throw new CustomException(HttpStatus.BAD_REQUEST,
                    "No se pudo determinar el límite de socios del plan del gimnasio",
                    "MemberCupoService | companyId: " + company.getId() + " | sin limite configurado "
                            + "(CompanySuscripcionCrm/PlanCrm/producto bsports-socios)");
        }

        long actuales = this.memberRepository.countByCompany_IdAndStatus(company.getId(), STATUS_ACTIVE_MEMBER);
        if (actuales + nuevosSocios > limite) {
            throw new CustomException(HttpStatus.BAD_REQUEST,
                    "El gimnasio alcanzó el límite de socios de su plan",
                    "MemberCupoService | companyId: " + company.getId() + " | actuales: " + actuales
                            + " | nuevos: " + nuevosSocios + " | limite: " + limite);
        }
    }
}
