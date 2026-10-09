package co.inmobiliaria360.web;

import co.inmobiliaria360.domain.Usuario;
import co.inmobiliaria360.repository.UsuarioRepository;
import co.inmobiliaria360.security.JwtService;
import co.inmobiliaria360.security.LoginThrottle;
import co.inmobiliaria360.security.PoliticaPassword;
import co.inmobiliaria360.security.UsuarioAcceso;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    public record LoginRequest(@NotBlank String email, @NotBlank String password) {}

    public record CambioPassword(@NotBlank @Size(max = 200) String actual, @NotBlank @Size(max = 200) String nueva) {}

    public record LoginResponse(String token, long expiraEnSegundos, UsuarioDto usuario) {}

    public record UsuarioDto(Long id, String email, String nombre, String rol, boolean activo, boolean debeCambiarPassword) {
        static UsuarioDto de(Usuario u) {
            return new UsuarioDto(u.getId(), u.getEmail(), u.getNombre(), u.getRol().name(), u.isActivo(), u.isDebeCambiarPassword());
        }
    }

    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final LoginThrottle throttle;
    private final UsuarioAcceso acceso;

    public AuthController(UsuarioRepository usuarios, PasswordEncoder encoder, JwtService jwt, LoginThrottle throttle,
                          UsuarioAcceso acceso) {
        this.usuarios = usuarios;
        this.encoder = encoder;
        this.jwt = jwt;
        this.throttle = throttle;
        this.acceso = acceso;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req) {
        String email = req.email().trim().toLowerCase();
        if (throttle.bloqueado(email)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(new Error("Demasiados intentos. Espera unos minutos e inténtalo de nuevo."));
        }
        Usuario u = usuarios.findByEmail(email).orElse(null);
        // Mismo mensaje para usuario inexistente, inactivo o clave incorrecta
        if (u == null || !u.isActivo() || !encoder.matches(req.password(), u.getPasswordHash())) {
            throttle.fallo(email);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new Error("Credenciales invalidas"));
        }
        throttle.exito(email);
        return ResponseEntity.ok(new LoginResponse(jwt.generar(u), jwt.segundosValidez(), UsuarioDto.de(u)));
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(Authentication auth) {
        return usuarios.findByEmail(auth.getName())
                .filter(Usuario::isActivo)
                .<ResponseEntity<?>>map(u -> ResponseEntity.ok(UsuarioDto.de(u)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

    /** El propio usuario cambia su contraseña (obligatorio en el primer ingreso del usuario inicial). */
    @PatchMapping("/password")
    public ResponseEntity<?> cambiarPassword(Authentication auth, @Valid @RequestBody CambioPassword req) {
        String email = auth.getName();
        String clave = "pwd:" + email;
        if (throttle.bloqueado(clave)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(new Error("Demasiados intentos. Espera unos minutos e inténtalo de nuevo."));
        }
        Usuario u = usuarios.findByEmail(email).filter(Usuario::isActivo).orElse(null);
        if (u == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        if (!encoder.matches(req.actual(), u.getPasswordHash())) {
            throttle.fallo(clave);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new Error("La contraseña actual no es correcta."));
        }
        if (req.actual().equals(req.nueva())) {
            return ResponseEntity.badRequest().body(new Error("La nueva contraseña debe ser distinta de la actual."));
        }
        PoliticaPassword.validar(req.nueva(), u.getEmail());
        throttle.exito(clave);
        u.setPasswordHash(encoder.encode(req.nueva()));
        u.setDebeCambiarPassword(false);
        usuarios.save(u);
        acceso.invalidar(email);
        return ResponseEntity.noContent().build();
    }

    public record Error(String mensaje) {}
}
