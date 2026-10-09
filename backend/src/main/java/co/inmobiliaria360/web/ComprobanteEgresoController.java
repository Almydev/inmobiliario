package co.inmobiliaria360.web;

import co.inmobiliaria360.domain.ComprobanteEgreso;
import co.inmobiliaria360.repository.ComprobanteEgresoRepository;
import co.inmobiliaria360.service.ComprobanteEgresoService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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
@RequestMapping("/api/comprobantes-egreso")
public class ComprobanteEgresoController {

    private static final String PERIODO = "^\\d{4}-(0[1-9]|1[0-2])$";

    public record GenerarRequest(@NotNull Long inmuebleId,
                                 @NotNull @Pattern(regexp = PERIODO, message = "debe tener formato AAAA-MM") String periodo,
                                 @Min(1) @Max(30) Integer dias,
                                 @DecimalMin("0.0") BigDecimal otrosDescuentos,
                                 @Size(max = 400) String concepto,
                                 @Size(max = 200) String imputacionContable) {}

    public record PeriodoRequest(@NotNull @Pattern(regexp = PERIODO, message = "debe tener formato AAAA-MM") String periodo) {}

    public record LoteRequest(@NotEmpty @Size(max = 100, message = "máximo 100 documentos por envío") List<Long> ids) {}

    public record ResultadoEnvio(Long id, boolean ok, String mensaje) {}

    public record EgresoDto(Long id, Long consecutivo, LocalDate fecha, String periodo,
                            Long inmuebleId, String inmueble, Long propietarioId, String propietario, String propietarioEmail,
                            String concepto, int dias, BigDecimal valorBruto, BigDecimal valorAdministracion,
                            BigDecimal otrosDescuentos, BigDecimal total, String imputacionContable, String estado,
                            LocalDateTime enviadoEn) {
        static EgresoDto de(ComprobanteEgreso e) {
            return new EgresoDto(e.getId(), e.getConsecutivo(), e.getFecha(), e.getPeriodo(),
                    e.getInmueble().getId(), e.getInmueble().getDescripcion(),
                    e.getPropietario().getId(), e.getPropietario().getNombre(), e.getPropietario().getEmail(),
                    e.getConcepto(), e.getDias(), e.getValorBruto(), e.getValorAdministracion(), e.getOtrosDescuentos(),
                    e.getTotalPagado(), e.getImputacionContable(), e.getEstado().name(), e.getEnviadoEn());
        }
    }

    private final ComprobanteEgresoService servicio;
    private final ComprobanteEgresoRepository repo;

    public ComprobanteEgresoController(ComprobanteEgresoService servicio, ComprobanteEgresoRepository repo) {
        this.servicio = servicio;
        this.repo = repo;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<EgresoDto> listar(@RequestParam(defaultValue = "") String q,
                                  @RequestParam(defaultValue = "") @Pattern(regexp = "^(\\d{4}-(0[1-9]|1[0-2]))?$") String periodo) {
        return repo.buscar(q.trim(), periodo).stream().map(EgresoDto::de).toList();
    }

    @PostMapping
    @Transactional
    public ResponseEntity<EgresoDto> generar(@Valid @RequestBody GenerarRequest r) {
        var e = servicio.generar(r.inmuebleId(), r.periodo(), r.dias() == null ? 30 : r.dias(),
                r.otrosDescuentos(), r.concepto(), r.imputacionContable());
        return ResponseEntity.status(HttpStatus.CREATED).body(EgresoDto.de(e));
    }

    @PostMapping("/generar-mes")
    public ComprobanteEgresoService.ResultadoMes generarMes(@Valid @RequestBody PeriodoRequest r) {
        return servicio.generarMes(r.periodo());
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> pdf(@PathVariable Long id) {
        var e = servicio.obtener(id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline().filename("comprobante-egreso-" + e.getConsecutivo() + ".pdf").build().toString())
                .header("X-Content-Type-Options", "nosniff")
                .body(servicio.pdf(id));
    }

    @PostMapping("/{id}/enviar")
    public EgresoDto enviar(@PathVariable Long id) {
        return EgresoDto.de(servicio.enviar(id));
    }

    @PostMapping("/enviar-lote")
    public List<ResultadoEnvio> enviarLote(@Valid @RequestBody LoteRequest r) {
        List<ResultadoEnvio> out = new ArrayList<>();
        for (Long id : r.ids().stream().distinct().toList()) {
            try {
                servicio.enviar(id);
                out.add(new ResultadoEnvio(id, true, "Enviado"));
            } catch (ResponseStatusException ex) {
                out.add(new ResultadoEnvio(id, false, ex.getReason()));
            }
        }
        return out;
    }

    @PostMapping("/{id}/pagar")
    public EgresoDto pagar(@PathVariable Long id) {
        return EgresoDto.de(servicio.pagar(id));
    }
}
