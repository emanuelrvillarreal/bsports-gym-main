package com.blue.open.Core.Service;

import com.blue.open.Core.Exceptions.CustomException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Properties;

@Service
public class JavaMailSender {

    @Value("${spring.mail.host}")
    private String host;

    @Value("${spring.mail.port}")
    private int port;

    @Value("${spring.mail.username}")
    private String username;

    @Value("${spring.mail.password}")
    private String password;

    @Value("${spring.mail.protocol:smtp}")
    private String protocol;

    @Value("${spring.mail.properties.mail.smtp.auth:true}")
    private String auth;

    @Value("${spring.mail.properties.mail.smtp.starttls.enable:true}")
    private String starttls;

    @Value("${spring.mail.properties.mail.debug:false}")
    private String debug;

    @Value("${mail.option:smtp}")
    private String mailOption;

    private JavaMailSenderImpl getMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(host);
        mailSender.setPort(port);
        mailSender.setUsername(username);
        mailSender.setPassword(password);

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", protocol);
        props.put("mail.smtp.auth", auth);
        props.put("mail.smtp.starttls.enable", starttls);
        props.put("mail.debug", debug);

        return mailSender;
    }

    /**
     * Envía un correo electrónico con configuración desde application.properties
     *
     * @param to          Destinatario
     * @param subject     Asunto
     * @param body        Cuerpo del mensaje (HTML soportado)
     * @param attachments Mapa de archivos adjuntos (Nombre archivo -> Bytes contenido). Puede ser null.
     */
    public void sendEmail(String to, String subject, String body, Map<String, byte[]> attachments) throws MessagingException {
        if ("notify".equalsIgnoreCase(mailOption)) {
            throw new CustomException(HttpStatus.NOT_IMPLEMENTED, "Funcionalidad no implementada", "El envio por notificacion (notify) no esta implementado todavia");
        }

        // Por defecto o si es 'smtp', usamos JavaMailSender
        JavaMailSenderImpl mailSender = getMailSender();

        MimeMessage message = mailSender.createMimeMessage();
        // true = multipart (para adjuntos)
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(username);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(body, true); // true = es HTML

        if (attachments != null && !attachments.isEmpty()) {
            for (Map.Entry<String, byte[]> entry : attachments.entrySet()) {
                if (entry.getValue() != null && entry.getKey() != null) {
                    helper.addAttachment(entry.getKey(), new ByteArrayResource(entry.getValue()));
                }
            }
        }

        mailSender.send(message);
    }

    /**
     * Envía un correo electrónico sin archivos adjuntos
     *
     * @param to      Destinatario
     * @param subject Asunto
     * @param body    Cuerpo del mensaje (HTML soportado)
     */
    public void sendEmail(String to, String subject, String body) throws MessagingException {
        sendEmail(to, subject, body, null);
    }
}
