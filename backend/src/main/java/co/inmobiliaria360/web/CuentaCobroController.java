package co.inmobiliaria360.web;

import co.inmobiliaria360.domain.CuentaCobro;
import co.inmobiliaria360.repository.CuentaCobroRepository;
import co.inmobiliaria360.service.CuentaCobroService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@Validated
@RestController
@RequestMapping("/api/cuentas-cobro")
public class CuentaCobroController {

    private static final String PERIODO = "^\\d{4}-(0[1-9]|1[0-2])$";

    public record GenerarRequest(@NotNull Long inmuebleId,
                                 @NotNull @Pattern(regexp = PERIODO, message = "debe tener formato AAAA-MM") String periodo,
                                 Boolean aplicarAdministracion,
                                 @DecimalMin("0.0") BigDecimal otros,
                                 @Size(max = 300) String concepto) {}

    public record PeriodoRequest(@NotNull @Pattern(regexp = PERIODO, message = "debe tener formato AAAA-MM") String periodo) {}

    public record LoteRequest(@NotEmpty @Size(max = 100, message = "máximo 100 documentos por envío") List<Long> ids) {}

    public record ResultadoEnvio(Long id, boolean ok, String mensaje) {}

    public record CuentaCobroDto(Long id, Long consecutivo, LocalDate fecha, String periodo,
                                 Long inmuebleId, String inmueble, Long inquilinoId, String inquilino, String inquilinoEmail,
                                 String propietario, String concepto, BigDecimal valorArriendo, BigDecimal valorAdministracion,
                                 BigDecimal otros, BigDecimal total, String estado,
                                 LocalDateTime enviadoEn, LocalDateTime pagadoEn) {
        static CuentaCobroDto de(CuentaCobro c) {
            return new CuentaCobroDto(c.getId(), c.getConsecutivo(), c.getFecha(), c.getPeriodo(),
                    c.getInmueble().getId(), c.getInmueble().getDescripcion(),
                    c.getInquilino().getId(), c.getInquilino().getNombre(), c.getInquilino().getEmail(),
                    c.getPropietario().getNombre(), c.getConcepto(), c.getValorArriendo(), c.getValorAdministracion(),
                    c.getOtros(), c.getTotal(), c.getEstado().name(), c.getEnviadoEn(), c.getPagadoEn());
        }
    }

    private final CuentaCobroService servicio;
    private final CuentaCobroRepository repo;

    public CuentaCobroController(CuentaCobroService servicio, CuentaCobroRepository repo) {
        this.servicio = servicio;
        this.repo = repo;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<CuentaCobroDto> listar(@RequestParam(defaultValue = "") String q,
                                       @RequestParam(defaultValue = "") @Pattern(regexp = "^(\\d{4}-(0[1-9]|1[0-2]))?$") String periodo) {
        return repo.buscar(q.trim(), periodo).stream().map(CuentaCobroDto::de).toList();
    }

    @PostMapping
    @Transactional
    public ResponseEntity<CuentaCobroDto> generar(@Valid @RequestBody GenerarRequest r) {
        var c = servicio.generar(r.inmuebleId(), r.periodo(), Boolean.TRUE.equals(r.aplicarAdministracion()), r.otros(), r.concepto());
        return ResponseEntity.status(HttpStatus.CREATED).body(CuentaCobroDto.de(c));
    }

    @PostMapping("/generar-mes")
    public CuentaCobroService.ResultadoMes generarMes(@Valid @RequestBody PeriodoRequest r) {
        return servicio.generarMes(r.periodo());
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> pdf(@PathVariable Long id) {
        var c = servicio.obtener(id);
        byte[] bytes = servicio.pdf(id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename("cuenta-cobro-" + c.getConsecutivo() + ".pdf").build().toString())
                .header("X-Content-Type-Options", "nosniff")
                .body(bytes);
    }

    @PostMapping("/{id}/enviar")
    public CuentaCobroDto enviar(@PathVariable Long id) {
        return CuentaCobroDto.de(servicio.enviar(id));
    }

    @PostMapping("/enviar-lote")
    public List<ResultadoEnvio> enviarLote(@Valid @RequestBody LoteRequest r) {
        List<ResultadoEnvio> out = new ArrayList<>();
        for (Long id : r.ids().stream().distinct().toList()) {
            try {
                servicio.enviar(id);
                out.add(new ResultadoEnvio(id, true, "Enviada"));
            } catch (ResponseStatusException e) {
                out.add(new ResultadoEnvio(id, false, e.getReason()));
            }
        }
        return out;
    }

    @PostMapping("/{id}/pagar")
    public CuentaCobroDto pagar(@PathVariable Long id) {
        return CuentaCobroDto.de(servicio.pagar(id));
    }
}
