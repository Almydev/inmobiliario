package co.inmobiliaria360.security;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Reglas de contraseña: largo, mayúscula, minúscula y número; sin claves comunes ni parecidas al correo. */
public final class PoliticaPassword {

    public static final int MIN = 10;
    /** BCrypt solo usa los primeros 72 bytes: se rechaza lo que lo supere en vez de truncarlo en silencio. */
    public static final int MAX_BYTES = 72;

    private static final Set<String> COMUNES = Set.of("password", "contrasena", "inmobiliaria", "123456789", "qwertyuiop", "abcdefghij");

    private PoliticaPassword() {}

    public static List<String> problemas(String password, String email) {
        List<String> p = new ArrayList<>();
        if (password == null || password.length() < MIN) {
            p.add("al menos " + MIN + " caracteres");
        }
        if (password == null) return p;
        if (password.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES) p.add("máximo " + MAX_BYTES + " caracteres");
        if (password.chars().noneMatch(Character::isLowerCase)) p.add("una letra minúscula");
        if (password.chars().noneMatch(Character::isUpperCase)) p.add("una letra mayúscula");
        if (password.chars().noneMatch(Character::isDigit)) p.add("un número");
        String bajo = password.toLowerCase();
        if (COMUNES.stream().anyMatch(bajo::contains)) p.add("no usar palabras comunes como 'password' o 'inmobiliaria'");
        if (email != null && !email.isBlank()) {
            String local = email.split("@")[0].toLowerCase();
            if (local.length() >= 4 && bajo.contains(local)) p.add("no incluir tu correo");
        }
        return p;
    }

    public static void validar(String password, String email) {
        List<String> p = problemas(password, email);
        if (!p.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La contraseña debe tener: " + String.join(", ", p) + ".");
        }
    }
}
