package com.sigtm.backend.service;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * Envía correos en formato HTML y construye las plantillas de correo del sistema.
 *
 * <p>Los envíos son «best effort»: si no hay remitente configurado o falla el servidor
 * SMTP, solo se registra el aviso en el log y nunca se interrumpe el flujo de negocio.</p>
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String mailFrom;

    @Value("${app.reset-password.base-url:http://localhost:3000}")
    private String baseUrl;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Envía un correo con el cuerpo en formato HTML.
     *
     * <p>No propaga errores: si {@code MAIL_USERNAME} no está configurado se limita a
     * registrar un aviso y retorna, y si el envío falla por cualquier causa se registra
     * el error en el log. El mensaje se compone con codificación UTF-8.</p>
     *
     * @param to       dirección de destino
     * @param subject  asunto del mensaje
     * @param htmlBody contenido del mensaje en HTML
     */
    public void sendHtmlEmail(String to, String subject, String htmlBody) {
        if (mailFrom == null || mailFrom.isBlank()) {
            log.warn("Correo no enviado a '{}': MAIL_USERNAME no configurado. Configura las variables MAIL_USERNAME/MAIL_PASSWORD.", to);
            return;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(mailFrom);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
            log.info("Correo enviado a {}", to);
        } catch (Exception e) {
            log.error("Error al enviar correo a {}", to, e);
        }
    }

    /**
     * Construye el correo HTML con el enlace de restablecimiento de contraseña.
     *
     * <p>El enlace se compone con la ruta {@code /restablecer-contrasena?token=} sobre
     * la URL base configurada en {@code app.reset-password.base-url}.</p>
     *
     * @param name  nombre del usuario al que se saluda
     * @param token token de recuperación que se incrusta en el enlace
     * @return el cuerpo del mensaje listo para enviar
     */
    public String buildResetPasswordEmail(String name, String token) {
        String link = baseUrl + "/restablecer-contrasena?token=" + token;
        return "<div style=\"font-family:Arial,Helvetica,sans-serif;max-width:560px;margin:0 auto;padding:24px;border:1px solid #e2e8f0;border-radius:16px;background:#ffffff;\">\n" +
                "  <div style=\"text-align:center;margin-bottom:20px;\">\n" +
                "    <div style=\"display:inline-flex;align-items:center;justify-content:center;width:56px;height:56px;border-radius:16px;background:#2563eb;color:#ffffff;font-size:24px;font-weight:bold;\">\u2699</div>\n" +
                "    <h1 style=\"color:#0f172a;font-size:22px;margin:12px 0 4px;\">SIGTM</h1>\n" +
                "    <p style=\"color:#64748b;font-size:14px;margin:0;\">Sistema Integral de Gestión de Taller Mecánico</p>\n" +
                "  </div>\n" +
                "  <h2 style=\"color:#0f172a;font-size:18px;\">Recuperación de contraseña</h2>\n" +
                "  <p style=\"color:#475569;font-size:14px;line-height:1.6;\">Hola <strong>%s</strong>, hemos recibido una solicitud para restablecer la contraseña de tu cuenta.</p>\n" +
                "  <p style=\"color:#475569;font-size:14px;line-height:1.6;\">Haz clic en el siguiente botón para crear una nueva contraseña. Este enlace es válido por <strong>30 minutos</strong>.</p>\n" +
                "  <div style=\"text-align:center;margin:28px 0;\">\n" +
                "    <a href=\"%s\" style=\"display:inline-block;background:#2563eb;color:#ffffff;text-decoration:none;padding:12px 28px;border-radius:10px;font-size:15px;font-weight:600;\">Restablecer contraseña</a>\n" +
                "  </div>\n" +
                "  <p style=\"color:#94a3b8;font-size:13px;text-align:center;\">Si no solicitaste este cambio, ignora este correo.</p>\n" +
                "</div>\n".formatted(name, link);
    }

    /**
     * Construye el correo HTML que anuncia novedades de una orden de trabajo.
     *
     * <p>El asunto del mensaje se toma de los primeros 80 caracteres de {@code message},
     * recortados con puntos suspensivos si excede esa longitud.</p>
     *
     * @param customerName nombre del cliente al que se saluda
     * @param orderNumber  número de la orden de trabajo
     * @param message      mensaje o novedad a comunicar
     * @return el cuerpo del mensaje HTML listo para enviar
     */
    public String buildOrderReadyEmail(String customerName, String orderNumber, String message) {
        return "<div style=\"font-family:Arial,Helvetica,sans-serif;max-width:560px;margin:0 auto;padding:24px;border:1px solid #e2e8f0;border-radius:16px;background:#ffffff;\">\n" +
                "  <div style=\"text-align:center;margin-bottom:20px;\">\n" +
                "    <div style=\"display:inline-flex;align-items:center;justify-content:center;width:56px;height:56px;border-radius:16px;background:#2563eb;color:#ffffff;font-size:24px;\">\u2699</div>\n" +
                "    <h1 style=\"color:#0f172a;font-size:22px;margin:12px 0 4px;\">SIGTM</h1>\n" +
                "  </div>\n" +
                "  <h2 style=\"color:#0f172a;font-size:18px;\">%s</h2>\n" +
                "  <p style=\"color:#475569;font-size:14px;line-height:1.6;\">Hola <strong>%s</strong>,</p>\n" +
                "  <p style=\"color:#475569;font-size:14px;line-height:1.6;\">Tu orden de trabajo <strong>%s</strong> tiene novedades.</p>\n" +
                "  <p style=\"color:#475569;font-size:14px;line-height:1.6;\">%s</p>\n" +
                "</div>\n".formatted(subjectOf(message), customerName, orderNumber, message);
    }

    private String subjectOf(String message) {
        return message.length() > 80 ? message.substring(0, 80) + "\u2026" : message;
    }
}