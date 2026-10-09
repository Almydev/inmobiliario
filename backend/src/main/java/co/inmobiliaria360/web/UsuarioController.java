package co.inmobiliaria360.web;

import co.inmobiliaria360.domain.Rol;
import co.inmobiliaria360.domain.Usuario;
import co.inmobiliaria360.repository.UsuarioRepository;
import co.inmobiliaria360.web.AuthController.UsuarioDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
                               @NotNull Rol rol, @NotBlank @Size(min = 8) String password) {}

    public record ActualizarUsuario(String nombre, Rol rol, Boolean activo,
                                    @Size(min = 8) String password) {}

    private final UsuarioRepository usuarios;
    private final PasswordEncoder encoder;

    public UsuarioController(UsuarioRepository usuarios, PasswordEncoder encoder) {
        this.usuarios = usuarios;
        this.encoder = encoder;
    }

    @GetMapping
    public List<UsuarioDto> listar() {
        return usuarios.findAll().stream().map(UsuarioDto::de).toList();
    }

    @PostMapping
    public ResponseEntity<?> crear(@Valid @RequestBody CrearUsuario req) {
        String email = req.email().trim().toLowerCase();
        if (usuarios.findByEmail(email).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new AuthController.Error("El email ya existe"));
        }
        Usuario u = new Usuario();
        u.setEmail(email);
        u.setNombre(req.nombre().trim());
        u.setRol(req.rol());
        u.setPasswordHash(encoder.encode(req.password()));
        return ResponseEntity.status(HttpStatus.CREATED).body(UsuarioDto.de(usuarios.save(u)));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable Long id, @Valid @RequestBody ActualizarUsuario req) {
        return usuarios.findById(id).<ResponseEntity<?>>map(u -> {
            if (req.nombre() != null && !req.nombre().isBlank()) u.setNombre(req.nombre().trim());
            if (req.rol() != null) u.setRol(req.rol());
            if (req.activo() != null) u.setActivo(req.activo());
            if (req.password() != null) u.setPasswordHash(encoder.encode(req.password()));
            return ResponseEntity.ok(UsuarioDto.de(usuarios.save(u)));
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }
}
