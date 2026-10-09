package co.inmobiliaria360.web;

import co.inmobiliaria360.domain.Rol;
import co.inmobiliaria360.domain.Usuario;
import co.inmobiliaria360.repository.UsuarioRepository;
import co.inmobiliaria360.security.PoliticaPassword;
import co.inmobiliaria360.security.UsuarioAcceso;
import co.inmobiliaria360.web.AuthController.UsuarioDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Solo ADMIN (ver SecurityConfig). */
@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    public record CrearUsuario(@NotBlank @Email String email, @NotBlank String nombre,
                               @NotNull Rol rol, @NotBlank @Size(max = 200) String password) {}

    public record ActualizarUsuario(String nombre, Rol rol, Boolean activo,
                                    @Size(max = 200) String password) {}

    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;
    private final UsuarioAcceso acceso;

    public UsuarioController(UsuarioRepository usuarios, PasswordEncoder encoder, UsuarioAcceso acceso) {
        this.usuarios = usuarios;
        this.encoder = encoder;
        this.acceso = acceso;
    }

    @GetMapping
    public List<UsuarioDto> listar() {
        return usuarios.findAll().stream().map(UsuarioDto::de).toList();
    }

    @PostMapping
    public ResponseEntity<?> crear(@Valid @RequestBody CrearUsuario req) {
        String email = req.email().trim().toLowerCase();
        PoliticaPassword.validar(req.password(), email);
        if (usuarios.findByEmail(email).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new AuthController.Error("El email ya existe"));
        }
        Usuario u = new Usuario();
        u.setEmail(email);
        u.setNombre(req.nombre().trim());
        u.setRol(req.rol());
        u.setPasswordHash(encoder.encode(req.password()));
        u.setDebeCambiarPassword(true); // la clave inicial la conoce quien lo crea: debe cambiarla al entrar
        return ResponseEntity.status(HttpStatus.CREATED).body(UsuarioDto.de(usuarios.save(u)));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<?> actualizar(Authentication auth, @PathVariable Long id, @Valid @RequestBody ActualizarUsuario req) {
        return usuarios.findById(id).<ResponseEntity<?>>map(u -> {
            boolean esUnoMismo = u.getEmail().equalsIgnoreCase(auth.getName());
            // Evita que el administrador se bloquee a sí mismo
            if (esUnoMismo && (Boolean.FALSE.equals(req.activo()) || (req.rol() != null && req.rol() != Rol.ADMIN))) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(new AuthController.Error("No puedes desactivarte ni quitarte el rol de administrador a ti mismo."));
            }
            if (req.nombre() != null && !req.nombre().isBlank()) u.setNombre(req.nombre().trim());
            if (req.rol() != null) u.setRol(req.rol());
            if (req.activo() != null) u.setActivo(req.activo());
            if (req.password() != null) {
                PoliticaPassword.validar(req.password(), u.getEmail());
                u.setPasswordHash(encoder.encode(req.password()));
                u.setDebeCambiarPassword(true); // restablecida por un administrador: debe cambiarla al entrar
            }
            var guardado = usuarios.save(u);
            acceso.invalidar(guardado.getEmail());
            return ResponseEntity.ok(UsuarioDto.de(guardado));
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }
}
