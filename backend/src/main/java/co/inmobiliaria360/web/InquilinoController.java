package co.inmobiliaria360.web;

import co.inmobiliaria360.domain.Inquilino;
import co.inmobiliaria360.repository.InquilinoRepository;
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
@RequestMapping("/api/inquilinos")
public class InquilinoController {

    public record InquilinoRequest(
            @NotBlank @Size(max = 150) String nombre,
            @NotBlank @Size(max = 30) String documento,
            @Email @Size(max = 150) String email,
            @Size(max = 30) String telefono,
            @Size(max = 80) String ciudad,
            @Size(max = 200) String direccion,
            Boolean activo) {}

    private final InquilinoRepository repo;

    public InquilinoController(InquilinoRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public List<Inquilino> listar(@RequestParam(defaultValue = "") String q) {
        return repo.buscar(q.trim());
    }

    @PostMapping
    public ResponseEntity<?> crear(@Valid @RequestBody InquilinoRequest req) {
        if (repo.findByDocumento(req.documento().trim()).isPresent()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new AuthController.Error("Ya existe un inquilino con ese documento"));
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(repo.save(aplicar(new Inquilino(), req)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable Long id, @Valid @RequestBody InquilinoRequest req) {
        return repo.findById(id).<ResponseEntity<?>>map(i -> {
            var otro = repo.findByDocumento(req.documento().trim());
            if (otro.isPresent() && !otro.get().getId().equals(id)) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(new AuthController.Error("Ya existe un inquilino con ese documento"));
            }
            return ResponseEntity.ok(repo.save(aplicar(i, req)));
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    private Inquilino aplicar(Inquilino i, InquilinoRequest r) {
        i.setNombre(r.nombre().trim());
        i.setDocumento(r.documento().trim());
        i.setEmail(vacioANulo(r.email()));
        i.setTelefono(vacioANulo(r.telefono()));
        i.setCiudad(vacioANulo(r.ciudad()));
        i.setDireccion(vacioANulo(r.direccion()));
        if (r.activo() != null) i.setActivo(r.activo());
        return i;
    }

    private static String vacioANulo(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
