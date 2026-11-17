package com.blue.open.Code.Web;

import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Service.CompanyAccesoService;
import com.blue.open.Core.Exceptions.CustomException;
import com.blue.open.Core.Service.Auth.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Corta el acceso a la plataforma de un club bloqueado, sea por facturacion vencida o por no
 * tener una suscripcion vigente. Para el club la unica forma de destrabarlo es del lado del CRM
 * — pagar la deuda, que dispara el ACK que lo desbloquea, o suscribirse a un plan.
 * <p>
 * Unica excepcion de rol: el sysadmin pasa siempre. Es quien administra los clubes desde el panel
 * interno, incluido el de suscribir a uno que justamente esta bloqueado por no tener plan, asi que
 * dejarlo afuera lo dejaria sin manera de resolverlo.
 * <p>
 * getCurrentCompanyOrThrow() ya resuelve exactamente los casos en los que este interceptor tiene
 * que dejar pasar (sin JWT, sin header X-Company-Uuid, sin acceso a esa company), lanzando
 * CustomException. No hace falta duplicar esa logica: si de verdad el request no correspondia, el
 * propio controller la vuelve a lanzar igual al llamar a AuthService.
 */
@Component
public class AccesoBloqueadoInterceptor implements HandlerInterceptor {

    @Autowired
    private AuthService authService;

    @Autowired
    private CompanyAccesoService companyAccesoService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (this.authService.isSysAdmin()) {
            return true;
        }

        Company company;
        try {
            company = this.authService.getCurrentCompanyOrThrow();
        } catch (CustomException e) {
            return true;
        }

        if (this.companyAccesoService.estaBloqueada(company)) {
            CustomException ex = new CustomException(HttpStatus.FORBIDDEN,
                    "El gimnasio tiene facturación vencida",
                    "AccesoBloqueadoInterceptor | companyId: " + company.getId());
            ex.newSubError("motivo", "FACTURA_VENCIDA",
                    "Regularice el pago en el CRM para recuperar el acceso a la plataforma.");
            throw ex;
        }
        return true;
    }
}
