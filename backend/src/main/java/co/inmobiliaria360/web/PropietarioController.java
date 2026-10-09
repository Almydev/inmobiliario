package co.inmobiliaria360.web;

import co.inmobiliaria360.domain.Propietario;
import co.inmobiliaria360.repository.PropietarioRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/propietarios")
public class PropietarioController {

    public record PropietarioRequest(
            @NotBlank @Size(max = 150) String nombre,
            @NotBlank @Size(max = 30) String documento,
            @Email @Size(max = 150) String email,
            @Size(max = 30) String telefono,
            @Size(max = 80) String banco,
            @Size(max = 30) String tipoCuenta,
            @Size(max = 40) String numeroCuenta,
            Boolean activo) {}

    private final PropietarioRepository repo;

    public PropietarioController(PropietarioRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public List<Propietario> listar(@RequestParam(defaultValue = "") String q) {
        return repo.buscar(q.trim());
    }

    @PostMapping
    public ResponseEntity<?> crear(@Valid @RequestBody PropietarioRequest req) {
        if (repo.findByDocumento(req.documento().trim()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new AuthController.Error("Ya existe un propietario con ese documento"));
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(repo.save(aplicar(new Propietario(), req)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable Long id, @Valid @RequestBody PropietarioRequest req) {
        return repo.findById(id).<ResponseEntity<?>>map(p -> {
            var otro = repo.findByDocumento(req.documento().trim());
            if (otro.isPresent() && !otro.get().getId().equals(id)) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(new AuthController.Error("Ya existe un propietario con ese documento"));
            }
            return ResponseEntity.ok(repo.save(aplicar(p, req)));
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    private Propietario aplicar(Propietario p, PropietarioRequest r) {
        p.setNombre(r.nombre().trim());
        p.setDocumento(r.documento().trim());
        p.setEmail(vacioANulo(r.email()));
        p.setTelefono(vacioANulo(r.telefono()));
        p.setBanco(vacioANulo(r.banco()));
        p.setTipoCuenta(vacioANulo(r.tipoCuenta()));
        p.setNumeroCuenta(vacioANulo(r.numeroCuenta()));
        if (r.activo() != null) p.setActivo(r.activo());
        return p;
    }

    private static String vacioANulo(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
