package co.inmobiliaria360.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.ServerSetupTest;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.web.server.ResponseStatusException;

/** Envia por SMTP real contra un servidor de pruebas en memoria: no sale ningun correo a nadie. */
class MailServiceTest {

    @RegisterExtension
    static GreenMailExtension greenMail = new GreenMailExtension(ServerSetupTest.SMTP);

    private MailService servicio(int puerto) {
        var sender = new JavaMailSenderImpl();
        sender.setHost("localhost");
        sender.setPort(puerto);
        return new MailService(sender, Fixtures.empresa(), "no-reply@prueba.test");
    }

    @Test
    void enviaElCorreoConElPdfAdjunto() throws Exception {
        byte[] pdf = new PdfService(Fixtures.empresa()).cuentaCobro(Fixtures.cuentaCobro(1, "ANA"));

        servicio(greenMail.getSmtp().getPort())
                .enviarConAdjunto("inquilino@prueba.test", "Cuenta de cobro No. 1", "Hola", "cuenta-cobro-1.pdf", pdf);

        MimeMessage[] recibidos = greenMail.getReceivedMessages();
        assertEquals(1, recibidos.length);
        assertEquals("Cuenta de cobro No. 1", recibidos[0].getSubject());
        assertEquals("inquilino@prueba.test", recibidos[0].getAllRecipients()[0].toString());
        var partes = (MimeMultipart) recibidos[0].getContent();
        assertEquals(2, partes.getCount());
        assertEquals("cuenta-cobro-1.pdf", partes.getBodyPart(1).getFileName());
        assertTrue(partes.getBodyPart(1).getContentType().toLowerCase().startsWith("application/pdf"));
    }

    @Test
    void siElServidorSmtpFallaDevuelve502SinFiltrarDetalles() {
        var ex = assertThrows(ResponseStatusException.class, () ->
                servicio(1).enviarConAdjunto("a@prueba.test", "x", "x", "x.pdf", new byte[] {1}));
        assertEquals(502, ex.getStatusCode().value());
        assertTrue(ex.getReason().startsWith("No se pudo enviar el correo"));
    }
}
