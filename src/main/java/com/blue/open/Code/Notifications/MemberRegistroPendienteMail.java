package com.blue.open.Code.Notifications;

import com.blue.open.Code.Entity.Db.Company;
import com.blue.open.Code.Entity.Db.Member;
import com.blue.open.Code.Entity.Db.User;
import com.blue.open.Code.Entity.Enums.MemberRegistroTipoEnum;
import com.blue.open.Core.Notifications.MailNotification;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;

public class MemberRegistroPendienteMail implements MailNotification {

    private static final DateTimeFormatter FECHA_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final Company company;
    private final Member member;
    private final MemberRegistroTipoEnum tipoRegistro;

    public MemberRegistroPendienteMail(Company company, Member member, MemberRegistroTipoEnum tipoRegistro) {
        this.company = company;
        this.member = member;
        this.tipoRegistro = tipoRegistro;
    }

    @Override
    public String getSubject() {
        String club = company.getNombreComercial() != null ? company.getNombreComercial() : "Club";
        return "Nueva solicitud de registro - " + club;
    }

    @Override
    public String getBody() {
        User user = member.getUser();
        String nombre = user != null && user.getNombre() != null ? user.getNombre() : "-";
        String apellido = user != null && user.getApellido() != null ? user.getApellido() : "-";
        String email = user != null && user.getEmail() != null ? user.getEmail() : "-";
        String dni = user != null && user.getDni() != null ? user.getDni() : "-";
        String telefono = user != null && user.getPhone() != null ? user.getPhone() : "-";
        String fechaNacimiento = user != null && user.getFechaNacimiento() != null
                ? user.getFechaNacimiento().toString()
                : "-";
        String club = company.getNombreComercial() != null ? company.getNombreComercial() : "-";
        String fechaSolicitud = member.getDateCreated() != null
                ? member.getDateCreated().toLocalDateTime().format(FECHA_FORMAT)
                : "-";
        String parentesco = member.getParentesco() != null ? member.getParentesco().name() : "-";
        String titularInfo = "-";
        if (member.getTitular() != null) {
            User titularUser = member.getTitular().getUser();
            String titularNombre = titularUser != null
                    ? (nullToEmpty(titularUser.getNombre()) + " " + nullToEmpty(titularUser.getApellido())).trim()
                    : "";
            titularInfo = titularNombre.isBlank()
                    ? member.getTitular().getUuid()
                    : titularNombre + " (" + member.getTitular().getUuid() + ")";
        }

        String html = "";
        try {
            Resource resource = new ClassPathResource(
                    "templates/MemberRegistroPendiente/registro-pendiente.html");
            if (resource.exists()) {
                html = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            } else {
                html = "ERROR: Plantilla HTML no encontrada en recursos";
            }
        } catch (java.io.IOException e) {
            html = "Error al cargar la plantilla: " + e.getMessage();
        }

        return html.replace("{club}", escapeHtml(club))
                .replace("{tipo}", escapeHtml(tipoRegistro.name()))
                .replace("{memberUuid}", escapeHtml(member.getUuid()))
                .replace("{nombre}", escapeHtml(nombre))
                .replace("{apellido}", escapeHtml(apellido))
                .replace("{email}", escapeHtml(email))
                .replace("{dni}", escapeHtml(dni))
                .replace("{telefono}", escapeHtml(telefono))
                .replace("{fechaNacimiento}", escapeHtml(fechaNacimiento))
                .replace("{parentesco}", escapeHtml(parentesco))
                .replace("{titular}", escapeHtml(titularInfo))
                .replace("{fechaSolicitud}", escapeHtml(fechaSolicitud));
    }

    private static String nullToEmpty(String value) {
        return value != null ? value : "";
    }

    private static String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
