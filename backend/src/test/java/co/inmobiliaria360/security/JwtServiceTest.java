package co.inmobiliaria360.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import co.inmobiliaria360.domain.Rol;
import co.inmobiliaria360.domain.Usuario;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;

class JwtServiceTest {

    private static final String SECRET = "0123456789abcdef0123456789abcdef";

    private Usuario usuario() {
        Usuario u = new Usuario();
        u.setEmail("ana@360.co");
        u.setNombre("Ana");
        u.setRol(Rol.OPERADOR);
        return u;
    }

    @Test
    void tokenGeneradoSeDecodificaConEmailYRol() {
        JwtService svc = new JwtService(SECRET, 8);
        Jwt jwt = svc.decoder().decode(svc.generar(usuario()));
        assertEquals("ana@360.co", jwt.getSubject());
        assertEquals("OPERADOR", jwt.getClaimAsString("rol"));
    }

    @Test
    void tokenFirmadoConOtraClaveSeRechaza() {
        String token = new JwtService("ffffffffffffffffffffffffffffffff", 8).generar(usuario());
        assertThrows(JwtException.class, () -> new JwtService(SECRET, 8).decoder().decode(token));
    }

    @Test
    void secretoCortoNoSePermite() {
        assertThrows(IllegalStateException.class, () -> new JwtService("corto", 8));
    }
}
