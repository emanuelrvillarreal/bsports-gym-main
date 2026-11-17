package com.blue.open.Code.Notifications;

import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.User;
import com.blue.open.Core.Notifications.MailNotification;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class ForgotPasswordMail implements MailNotification {
    private final User user;
    private final String code;
    private final String frontUrl;
    private final Company company;

    public ForgotPasswordMail(User user, String code, String frontUrl, Company company) {
        this.user = user;
        this.code = code;
        this.frontUrl = frontUrl;
        this.company = company;
    }

    @Override
    public String getSubject() {
        return "Recuperación de Contraseña - B Sports";
    }

    @Override
    public String getBody() {
        String html = "";
        try {
            Resource resource = new ClassPathResource(
                    "templates/ForgotPassword/recuperar-password.html");
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
                .replace("?user={user}&code={code}&email={email}",
                        "?user=" + encodeUtf8(displayName)
                                + "&code=" + encodeUtf8(code)
                                + "&email=" + encodeUtf8(user.getEmail()))
                .replace("{user}", displayName)
                .replace("{code}", code)
                .replace("{email}", user.getEmail())
                .replace("{colorPrimario}", company != null && company.getColorPrimario() != null
                        ? company.getColorPrimario() : "")
                .replace("{colorSecundario}", company != null && company.getColorSecundario() != null
                        ? company.getColorSecundario() : "")
                .replace("{imgClub}", company != null && company.getLogoUrl() != null
                        ? company.getLogoUrl() : "");
    }

    private static String encodeUtf8(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
