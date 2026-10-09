package co.inmobiliaria360.security;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/** Frena la fuerza bruta: tras MAX_FALLOS intentos fallidos de un correo, lo bloquea durante VENTANA. En memoria. */
@Component
public class LoginThrottle {

    static final int MAX_FALLOS = 5;
    static final Duration VENTANA = Duration.ofMinutes(10);

    private record Estado(int fallos, Instant desde) {}

    private final ConcurrentHashMap<String, Estado> estados = new ConcurrentHashMap<>();
    private final Clock reloj;

    public LoginThrottle() {
        this(Clock.systemUTC());
    }

    LoginThrottle(Clock reloj) {
        this.reloj = reloj;
    }

    public boolean bloqueado(String clave) {
        Estado e = estados.get(clave);
        if (e == null) return false;
        if (e.desde().plus(VENTANA).isBefore(reloj.instant())) {
            estados.remove(clave, e);
            return false;
        }
        return e.fallos() >= MAX_FALLOS;
    }

    public void fallo(String clave) {
        Instant ahora = reloj.instant();
        estados.merge(clave, new Estado(1, ahora), (previo, nuevo) ->
                previo.desde().plus(VENTANA).isBefore(ahora) ? nuevo : new Estado(previo.fallos() + 1, previo.desde()));
        if (estados.size() > 10_000) estados.entrySet().removeIf(en -> en.getValue().desde().plus(VENTANA).isBefore(ahora));
    }

    public void exito(String clave) {
        estados.remove(clave);
    }
}
