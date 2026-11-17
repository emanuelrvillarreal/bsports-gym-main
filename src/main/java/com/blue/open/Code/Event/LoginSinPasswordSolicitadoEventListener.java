package com.blue.open.Code.Event;

import com.blue.open.Code.Config.FrontUrlProperties;
import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Notifications.LoginSinPasswordMail;
import com.blue.open.Code.Repository.CompanyRepository;
import com.blue.open.Core.Service.JavaMailSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@Slf4j
public class LoginSinPasswordSolicitadoEventListener {

    // Blue Open Data: marca por defecto cuando el club no tiene logo o colores, igual que forgot-password
    private static final String UUID_COMPANY_POR_DEFECTO = "2";

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private FrontUrlProperties frontUrlProperties;

    @Autowired
    private JavaMailSender javaMailSender;

    @Async("AsyncTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onLoginSinPasswordSolicitado(LoginSinPasswordSolicitadoEvent event) {
        try {
            Company company = resolverCompany(event.getPath());
            LoginSinPasswordMail mail = new LoginSinPasswordMail(
                    event.getNombreUsuario(),
                    event.getCodigoPlano(),
                    frontUrlProperties.getMailUrl(),
                    company);
            javaMailSender.sendEmail(event.getEmail(), mail.getSubject(), mail.getBody());
        } catch (Exception ex) {
            log.error("Error al enviar el mail de login sin contraseña | email={}", event.getEmail(), ex);
        }
    }

    private Company resolverCompany(String path) {
        if (path == null || path.isBlank()) {
            return companyRepository.findFirstByUuid(UUID_COMPANY_POR_DEFECTO);
        }

        Company company = companyRepository.findFirstByPath(path.trim());
        boolean tieneMarcaCompleta = company != null
                && company.getLogoUrl() != null
                && company.getColorPrimario() != null
                && company.getColorSecundario() != null;
        if (tieneMarcaCompleta) {
            return company;
        }
        return companyRepository.findFirstByUuid(UUID_COMPANY_POR_DEFECTO);
    }
}
