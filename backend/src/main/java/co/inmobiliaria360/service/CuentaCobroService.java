package co.inmobiliaria360.service;

import co.inmobiliaria360.config.EmpresaProps;
import co.inmobiliaria360.domain.CuentaCobro;
import co.inmobiliaria360.domain.EstadoDocumento;
import co.inmobiliaria360.domain.MovimientoBanco;
import co.inmobiliaria360.repository.CuentaCobroRepository;
import co.inmobiliaria360.repository.InmuebleRepository;
import co.inmobiliaria360.repository.MovimientoBancoRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CuentaCobroService {

    public record ResultadoMes(int creadas, int omitidas) {}

    private final CuentaCobroRepository cuentas;
    private final InmuebleRepository inmuebles;
    private final MovimientoBancoRepository movimientos;
    private final PdfService pdf;
    private final MailService mail;
    private final EmpresaProps empresa;
    private final Set<Long> enviando = ConcurrentHashMap.newKeySet();

    public CuentaCobroService(CuentaCobroRepository cuentas, InmuebleRepository inmuebles,
                              MovimientoBancoRepository movimientos, PdfService pdf, MailService mail,
                              EmpresaProps empresa) {
        this.cuentas = cuentas;
        this.inmuebles = inmuebles;
        this.movimientos = movimientos;
        this.pdf = pdf;
        this.mail = mail;
        this.empresa = empresa;
    }

    @Transactional
    public CuentaCobro generar(Long inmuebleId, String periodo, boolean aplicarAdministracion,
                               BigDecimal otros, String concepto) {
        YearMonth ym = parsePeriodo(periodo);
        var inmueble = inmuebles.findById(inmuebleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "El inmueble no existe"));
        if (inmueble.getInquilino() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El inmueble no tiene inquilino asignado");
        }
        if (cuentas.existsByInmuebleIdAndPeriodo(inmuebleId, periodo)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe una cuenta de cobro de este inmueble para " + periodo);
        }

        BigDecimal arriendo = inmueble.getCanon();
        BigDecimal admin = aplicarAdministracion
                ? arriendo.multiply(inmueble.getPctAdminCobro()).divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        BigDecimal otrosValor = otros == null ? BigDecimal.ZERO : otros;

        CuentaCobro c = new CuentaCobro();
        c.setConsecutivo(cuentas.siguienteConsecutivo());
        c.setFecha(LocalDate.now());
        c.setPeriodo(periodo);
        c.setInmueble(inmueble);
        c.setInquilino(inmueble.getInquilino());
        c.setPropietario(inmueble.getPropietario());
        c.setConcepto(concepto == null || concepto.isBlank() ? conceptoPorDefecto(ym) : concepto.trim());
        c.setValorArriendo(arriendo);
        c.setValorAdministracion(admin);
        c.setOtros(otrosValor);
        c.setReteFuente(BigDecimal.ZERO);
        c.setTotal(arriendo.add(admin).add(otrosValor));
        try {
            return cuentas.save(c);
        } catch (DataIntegrityViolationException e) {
            // Otro clic u otra pestaña la creó en el mismo instante (índice único inmueble + periodo)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe una cuenta de cobro de este inmueble para " + periodo);
        }
    }

    @Transactional
    public ResultadoMes generarMes(String periodo) {
        parsePeriodo(periodo);
        int creadas = 0;
        int omitidas = 0;
        for (var i : inmuebles.activosConInquilino()) {
            if (cuentas.existsByInmuebleIdAndPeriodo(i.getId(), periodo)) {
                omitidas++;
            } else {
                generar(i.getId(), periodo, false, null, null);
                creadas++;
            }
        }
        return new ResultadoMes(creadas, omitidas);
    }

    @Transactional(readOnly = true)
    public CuentaCobro obtener(Long id) {
        return cuentas.detalle(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "La cuenta de cobro no existe"));
    }

    public byte[] pdf(Long id) {
        return pdf.cuentaCobro(obtener(id));
    }

    /** No es transaccional a proposito: no se retiene una conexion de BD mientras se habla con el servidor SMTP. */
    public CuentaCobro enviar(Long id) {
        if (!enviando.add(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Esta cuenta de cobro ya se está enviando");
        }
        try {
            return enviarSinGuarda(id);
        } finally {
            enviando.remove(id);
        }
    }

    private CuentaCobro enviarSinGuarda(Long id) {
        CuentaCobro c = obtener(id);
        String para = c.getInquilino().getEmail();
        if (para == null || para.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El inquilino no tiene correo registrado");
        }
        String asunto = "Cuenta de cobro No. " + c.getConsecutivo() + " - " + c.getPeriodo();
        String cuerpo = "Cordial saludo, " + limpiar(c.getInquilino().getNombre()) + ".\n\n"
                + "Adjuntamos su cuenta de cobro No. " + c.getConsecutivo() + " correspondiente a " + c.getPeriodo() + ".\n\n"
                + "Cualquier inquietud, escríbanos a " + empresa.email() + ".\n\n" + empresa.nombre();
        mail.enviarConAdjunto(para, asunto, cuerpo, "cuenta-cobro-" + c.getConsecutivo() + ".pdf", pdf.cuentaCobro(c));

        if (c.getEstado() == EstadoDocumento.BORRADOR) c.setEstado(EstadoDocumento.ENVIADO);
        c.setEnviadoEn(LocalDateTime.now());
        return cuentas.save(c);
    }

    @Transactional
    public CuentaCobro pagar(Long id) {
        // Actualizacion atomica: si dos clics llegan a la vez, solo uno cambia la fila; el otro recibe 409.
        if (cuentas.marcarPagada(id, LocalDateTime.now()) == 0) {
            obtener(id); // 404 si no existe
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La cuenta de cobro ya está pagada");
        }
        CuentaCobro c = obtener(id);

        MovimientoBanco m = new MovimientoBanco();
        m.setFecha(LocalDate.now());
        m.setConcepto("pago de arriendo " + c.getInmueble().getDescripcion() + " (" + c.getInquilino().getNombre() + ")");
        m.setIngreso(c.getTotal());
        m.setAdministracion(c.getValorAdministracion());
        m.setCuentaCobro(c);
        movimientos.save(m);
        return c;
    }

    static YearMonth parsePeriodo(String periodo) {
        try {
            return YearMonth.parse(periodo);
        } catch (DateTimeParseException | NullPointerException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El periodo debe tener formato AAAA-MM");
        }
    }

    static String conceptoPorDefecto(YearMonth ym) {
        String mes = ym.getMonth().getDisplayName(TextStyle.FULL, Locale.forLanguageTag("es-CO")).toUpperCase(Locale.ROOT);
        return "PAGO DE ARRENDAMIENTO DEL MES DE " + mes + " DE " + ym.getYear();
    }

    private static String limpiar(String s) {
        return s == null ? "" : s.replaceAll("[\\r\\n]+", " ");
    }
}
