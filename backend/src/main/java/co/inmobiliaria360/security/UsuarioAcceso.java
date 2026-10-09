package co.inmobiliaria360.security;

import co.inmobiliaria360.domain.Usuario;
import co.inmobiliaria360.repository.UsuarioRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * Consulta el estado actual del usuario del token (activo, rol, cambio de contraseña pendiente) con una cache corta,
 * para que desactivar a alguien o cambiarle el rol surta efecto en segundos y no cuando venza su token.
 */
@Component
public class UsuarioAcceso {

    static final Duration VIGENCIA = Duration.ofSeconds(15);

    private record Entrada(Usuario usuario, Instant vence) {}

    private final UsuarioRepository usuarios;
    private final Clock reloj;
    private final ConcurrentHashMap<String, Entrada> cache = new ConcurrentHashMap<>();

    public UsuarioAcceso(UsuarioRepository usuarios, Clock reloj) {
        this.usuarios = usuarios;
        this.reloj = reloj;
    }

    /** Usuario activo con ese correo, o vacío si no existe o está dado de baja. */
    public Optional<Usuario> activo(String email) {
        Instant ahora = reloj.instant();
        Entrada e = cache.get(email);
        if (e != null && e.vence().isAfter(ahora)) {
            return Optional.ofNullable(e.usuario());
        }
        Usuario u = usuarios.findByEmail(email).filter(Usuario::isActivo).orElse(null);
        cache.put(email, new Entrada(u, ahora.plus(VIGENCIA)));
        return Optional.ofNullable(u);
    }

    public void invalidar(String email) {
        cache.remove(email);
    }
}
