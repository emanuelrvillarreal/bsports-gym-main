package com.blue.open.Code.Notifications;

import java.nio.charset.StandardCharsets;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import com.blue.open.Code.Entity.Db.User;
import com.blue.open.Core.Notifications.MailNotification;

public class RegistrationMail implements MailNotification {
    private final User user;
    private final String code;
    private final String frontUrl;

    public RegistrationMail(User user, String code, String frontUrl) {
        this.user = user;
        this.code = code;
        this.frontUrl = frontUrl;
    }

    @Override
    public String getSubject() {
        return "Código de Verificación - Blue Open Data";
    }

    @Override
    public String getBody() {
        String html = "";
        try {
            Resource resource = new ClassPathResource(
                    "templates/Register/verification-code-email.html");
            if (resource.exists()) {
                html = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            } else {
                html = "ERROR: Plantilla HTML no encontrada en recursos";
            }
        } catch (java.io.IOException e) {
            html = "Error al cargar la plantilla: " + e.getMessage();
        }

        String displayName = user.getUsername();
        return html.replace("{baseUrl}", frontUrl != null ? frontUrl : "")
                .replace("{front.url}", frontUrl != null ? frontUrl : "")
                .replace("{user}", displayName)
                .replace("{code}", code);
    }
}
