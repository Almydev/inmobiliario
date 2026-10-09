package co.inmobiliaria360.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class LoginThrottleTest {

    /** Reloj que se puede adelantar a mano. */
    private static final class RelojManual extends Clock {
        Instant ahora = Instant.parse("2026-10-09T12:00:00Z");
        @Override public java.time.ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId z) { return this; }
        @Override public Instant instant() { return ahora; }
    }

    @Test
    void bloqueaTrasCincoFallosYSoloEseCorreo() {
        var t = new LoginThrottle(new RelojManual());
        for (int i = 0; i < 4; i++) t.fallo("a@x.co");
        assertFalse(t.bloqueado("a@x.co"));
        t.fallo("a@x.co");
        assertTrue(t.bloqueado("a@x.co"));
        assertFalse(t.bloqueado("otro@x.co"));
    }

    @Test
    void elBloqueoExpiraConElTiempo() {
        var reloj = new RelojManual();
        var t = new LoginThrottle(reloj);
        for (int i = 0; i < 5; i++) t.fallo("a@x.co");
        assertTrue(t.bloqueado("a@x.co"));
        reloj.ahora = reloj.ahora.plus(Duration.ofMinutes(11));
        assertFalse(t.bloqueado("a@x.co"));
    }

    @Test
    void unLoginExitosoReiniciaElContador() {
        var t = new LoginThrottle(new RelojManual());
        for (int i = 0; i < 4; i++) t.fallo("a@x.co");
        t.exito("a@x.co");
        for (int i = 0; i < 4; i++) t.fallo("a@x.co");
        assertFalse(t.bloqueado("a@x.co"));
    }
}
