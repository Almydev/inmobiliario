package co.inmobiliaria360.web;

import co.inmobiliaria360.domain.Usuario;
import co.inmobiliaria360.repository.UsuarioRepository;
import co.inmobiliaria360.security.JwtService;
import co.inmobiliaria360.security.LoginThrottle;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
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

    public record LoginResponse(String token, long expiraEnSegundos, UsuarioDto usuario) {}

    public record UsuarioDto(Long id, String email, String nombre, String rol) {
        static UsuarioDto de(Usuario u) {
            return new UsuarioDto(u.getId(), u.getEmail(), u.getNombre(), u.getRol().name());
        }
    }

    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final LoginThrottle throttle;

    public AuthController(UsuarioRepository usuarios, PasswordEncoder encoder, JwtService jwt, LoginThrottle throttle) {
        this.usuarios = usuarios;
        this.encoder = encoder;
        this.jwt = jwt;
        this.throttle = throttle;
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

    public record Error(String mensaje) {}
}
