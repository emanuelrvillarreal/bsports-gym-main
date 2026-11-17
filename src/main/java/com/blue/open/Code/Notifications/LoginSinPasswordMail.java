package com.blue.open.Code.Notifications;

import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Core.Notifications.MailNotification;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.nio.charset.StandardCharsets;

public class LoginSinPasswordMail implements MailNotification {

    private static final String PLANTILLA = "templates/LoginSinPassword/login-sin-contrasena.html";

    private final String nombreUsuario;
    private final String codigoPlano;
    private final String frontUrl;
    private final Company company;

    public LoginSinPasswordMail(String nombreUsuario, String codigoPlano, String frontUrl, Company company) {
        this.nombreUsuario = nombreUsuario;
        this.codigoPlano = codigoPlano;
        this.frontUrl = frontUrl;
        this.company = company;
    }

    @Override
    public String getSubject() {
        return "Acceso a tu cuenta - B Sports";
    }

    @Override
    public String getBody() {
        String html = cargarPlantilla();
        String baseUrl = valorOVacio(frontUrl);
        String usuario = valorOVacio(nombreUsuario);

        String colorPrimario = "";
        String colorSecundario = "";
        String imgClub = "";
        if (company != null) {
            colorPrimario = valorOVacio(company.getColorPrimario());
            colorSecundario = valorOVacio(company.getColorSecundario());
            imgClub = valorOVacio(company.getLogoUrl());
        }

        return html.replace("{baseUrl}", baseUrl)
                .replace("{user}", usuario)
                .replace("{code}", codigoPlano)
                .replace("{colorPrimario}", colorPrimario)
                .replace("{colorSecundario}", colorSecundario)
                .replace("{imgClub}", imgClub);
    }

    private String cargarPlantilla() {
        Resource resource = new ClassPathResource(PLANTILLA);
        if (!resource.exists()) {
            return "ERROR: Plantilla HTML no encontrada en recursos";
        }

        try {
            return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (java.io.IOException e) {
            return "Error al cargar la plantilla: " + e.getMessage();
        }
    }

    private String valorOVacio(String valor) {
        if (valor == null) {
            return "";
        }
        return valor;
    }
}
