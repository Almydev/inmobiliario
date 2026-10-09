package co.inmobiliaria360.web;

import co.inmobiliaria360.service.BancoService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/banco")
public class BancoController {

    private static final String PERIODO = "^\\d{4}-(0[1-9]|1[0-2])$";
    private static final String TOPE = "100000000000";

    public record MovimientoRequest(@NotNull LocalDate fecha,
                                    @NotBlank @Size(max = 400) String concepto,
                                    @DecimalMin("0.0") @DecimalMax(TOPE) BigDecimal ingreso,
                                    @DecimalMin("0.0") @DecimalMax(TOPE) BigDecimal gasto,
                                    @DecimalMin("0.0") @DecimalMax(TOPE) BigDecimal administracion) {}

    private final BancoService servicio;

    public BancoController(BancoService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    public BancoService.Cuadre cuadre(@RequestParam @Pattern(regexp = PERIODO, message = "debe tener formato AAAA-MM") String periodo) {
        return servicio.cuadre(periodo);
    }

    @GetMapping("/csv")
    public ResponseEntity<byte[]> csv(@RequestParam @Pattern(regexp = PERIODO, message = "debe tener formato AAAA-MM") String periodo) {
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "csv", java.nio.charset.StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename("cuadre-banco-" + periodo + ".csv").build().toString())
                .header("X-Content-Type-Options", "nosniff")
                .body(servicio.csv(periodo));
    }

    @PostMapping("/movimientos")
    public ResponseEntity<Void> crear(@Valid @RequestBody MovimientoRequest r) {
        servicio.crearManual(r.fecha(), r.concepto(), r.ingreso(), r.gasto(), r.administracion());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/movimientos/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        servicio.eliminarManual(id);
        return ResponseEntity.noContent().build();
    }
}
