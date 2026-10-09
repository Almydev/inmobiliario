package co.inmobiliaria360.web;

import co.inmobiliaria360.domain.Inmueble;
import co.inmobiliaria360.repository.InmuebleRepository;
import co.inmobiliaria360.repository.InquilinoRepository;
import co.inmobiliaria360.repository.PropietarioRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/inmuebles")
public class InmuebleController {

    public record InmuebleRequest(
            @NotBlank @Size(max = 200) String descripcion,
            @NotBlank @Size(max = 200) String direccion,
            @Size(max = 80) String ciudad,
            @NotNull Long propietarioId,
            Long inquilinoId,
            @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal canon,
            @DecimalMin("0.0") @DecimalMax("100.0") BigDecimal pctAdminCobro,
            @DecimalMin("0.0") @DecimalMax("100.0") BigDecimal pctAdminEgreso,
            Boolean activo) {}

    public record InmuebleDto(Long id, String descripcion, String direccion, String ciudad,
                              Long propietarioId, String propietarioNombre,
                              Long inquilinoId, String inquilinoNombre,
                              BigDecimal canon, BigDecimal pctAdminCobro, BigDecimal pctAdminEgreso,
                              boolean activo) {
        static InmuebleDto de(Inmueble i) {
            var t = i.getInquilino();
            return new InmuebleDto(i.getId(), i.getDescripcion(), i.getDireccion(), i.getCiudad(),
                    i.getPropietario().getId(), i.getPropietario().getNombre(),
                    t == null ? null : t.getId(), t == null ? null : t.getNombre(),
                    i.getCanon(), i.getPctAdminCobro(), i.getPctAdminEgreso(), i.isActivo());
        }
    }

    private final InmuebleRepository repo;
    private final PropietarioRepository propietarios;
    private final InquilinoRepository inquilinos;

    public InmuebleController(InmuebleRepository repo, PropietarioRepository propietarios, InquilinoRepository inquilinos) {
        this.repo = repo;
        this.propietarios = propietarios;
        this.inquilinos = inquilinos;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<InmuebleDto> listar(@RequestParam(defaultValue = "") String q) {
        return repo.buscar(q.trim()).stream().map(InmuebleDto::de).toList();
    }

    @PostMapping
    @Transactional
    public ResponseEntity<?> crear(@Valid @RequestBody InmuebleRequest req) {
        return guardar(new Inmueble(), req, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<?> actualizar(@PathVariable Long id, @Valid @RequestBody InmuebleRequest req) {
        return repo.findById(id)
                .<ResponseEntity<?>>map(i -> guardar(i, req, HttpStatus.OK))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private ResponseEntity<?> guardar(Inmueble i, InmuebleRequest r, HttpStatus ok) {
        var propietario = propietarios.findById(r.propietarioId()).orElse(null);
        if (propietario == null) return error("El propietario no existe");
        i.setPropietario(propietario);
        if (r.inquilinoId() == null) {
            i.setInquilino(null);
        } else {
            var inquilino = inquilinos.findById(r.inquilinoId()).orElse(null);
            if (inquilino == null) return error("El inquilino no existe");
            i.setInquilino(inquilino);
        }
        i.setDescripcion(r.descripcion().trim());
        i.setDireccion(r.direccion().trim());
        i.setCiudad(r.ciudad() == null || r.ciudad().isBlank() ? null : r.ciudad().trim());
        i.setCanon(r.canon());
        if (r.pctAdminCobro() != null) i.setPctAdminCobro(r.pctAdminCobro());
        if (r.pctAdminEgreso() != null) i.setPctAdminEgreso(r.pctAdminEgreso());
        if (r.activo() != null) i.setActivo(r.activo());
        return ResponseEntity.status(ok).body(InmuebleDto.de(repo.save(i)));
    }

    private static ResponseEntity<?> error(String mensaje) {
        return ResponseEntity.badRequest().body(new AuthController.Error(mensaje));
    }
}
