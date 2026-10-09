package co.inmobiliaria360.service;

import co.inmobiliaria360.config.EmpresaProps;
import jakarta.mail.MessagingException;
import java.io.UnsupportedEncodingException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class MailService {

    private final JavaMailSender sender;
    private final EmpresaProps empresa;
    private final String from;

    public MailService(JavaMailSender sender, EmpresaProps empresa, @Value("${app.mail.from}") String from) {
        this.sender = sender;
        this.empresa = empresa;
        this.from = from;
    }

    public void enviarConAdjunto(String para, String asunto, String cuerpo, String nombreArchivo, byte[] pdf) {
        try {
            var msg = sender.createMimeMessage();
            var h = new MimeMessageHelper(msg, true, "UTF-8");
            h.setFrom(from, empresa.nombre());
            h.setTo(para);
            h.setSubject(asunto);
            h.setText(cuerpo, false);
            h.addAttachment(nombreArchivo, new ByteArrayResource(pdf), "application/pdf");
            sender.send(msg);
        } catch (MessagingException | MailException | UnsupportedEncodingException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "No se pudo enviar el correo a " + para);
        }
    }
}
