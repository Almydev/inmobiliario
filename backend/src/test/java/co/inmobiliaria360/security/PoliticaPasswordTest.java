package co.inmobiliaria360.security;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class PoliticaPasswordTest {

    @Test
    void aceptaUnaClaveFuerte() {
        assertDoesNotThrow(() -> PoliticaPassword.validar("Casa-Verde-2026", "ana@x.co"));
    }

    @Test
    void rechazaClavesDebiles() {
        for (String mala : new String[] {"corta1A", "sinmayuscula123", "SINMINUSCULA123", "SinNumerosAqui", "Password-12345", "Inmobiliaria-2026"}) {
            assertThrows(ResponseStatusException.class, () -> PoliticaPassword.validar(mala, "ana@x.co"), mala);
        }
    }

    @Test
    void rechazaClavesQueContienenElCorreo() {
        assertThrows(ResponseStatusException.class, () -> PoliticaPassword.validar("MariaLopez-2026", "marialopez@x.co"));
    }

    @Test
    void rechazaClavesMasLargasQueElLimiteDeBcrypt() {
        String larga = "Aa1-" + "x".repeat(80);
        var ex = assertThrows(ResponseStatusException.class, () -> PoliticaPassword.validar(larga, "a@x.co"));
        assertTrue(ex.getReason().contains("máximo"));
    }

    @Test
    void elMensajeListaTodoLoQueFalta() {
        var ex = assertThrows(ResponseStatusException.class, () -> PoliticaPassword.validar("abc", "a@x.co"));
        assertTrue(ex.getReason().contains("10 caracteres") && ex.getReason().contains("mayúscula") && ex.getReason().contains("número"));
    }
}
