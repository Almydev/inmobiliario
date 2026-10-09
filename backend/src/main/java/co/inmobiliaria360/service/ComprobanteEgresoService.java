package co.inmobiliaria360.service;

import co.inmobiliaria360.config.EmpresaProps;
import co.inmobiliaria360.domain.ComprobanteEgreso;
import co.inmobiliaria360.domain.EstadoDocumento;
import co.inmobiliaria360.domain.Inmueble;
import co.inmobiliaria360.domain.MovimientoBanco;
import co.inmobiliaria360.repository.ComprobanteEgresoRepository;
import co.inmobiliaria360.repository.CuentaCobroRepository;
import co.inmobiliaria360.repository.InmuebleRepository;
import co.inmobiliaria360.repository.MovimientoBancoRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
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
public class ComprobanteEgresoService {

    public record ResultadoMes(int creados, int omitidos) {}

    private static final BigDecimal CIEN = BigDecimal.valueOf(100);

    private final ComprobanteEgresoRepository egresos;
    private final InmuebleRepository inmuebles;
    private final CuentaCobroRepository cuentas;
    private final MovimientoBancoRepository movimientos;
    private final PdfService pdf;
    private final MailService mail;
    private final EmpresaProps empresa;
    private final Set<Long> enviando = ConcurrentHashMap.newKeySet();

    public ComprobanteEgresoService(ComprobanteEgresoRepository egresos, InmuebleRepository inmuebles,
                                    CuentaCobroRepository cuentas, MovimientoBancoRepository movimientos,
                                    PdfService pdf, MailService mail, EmpresaProps empresa) {
        this.egresos = egresos;
        this.inmuebles = inmuebles;
        this.cuentas = cuentas;
        this.movimientos = movimientos;
        this.pdf = pdf;
        this.mail = mail;
        this.empresa = empresa;
    }

    /** Valor bruto = canon / 30 x dias; administracion = % del bruto; total = bruto - administracion - otros. */
    @Transactional
    public ComprobanteEgreso generar(Long inmuebleId, String periodo, int dias, BigDecimal otrosDescuentos,
                                     String concepto, String imputacionContable) {
        YearMonth ym = CuentaCobroService.parsePeriodo(periodo);
        if (dias < 1 || dias > 30) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Los días deben estar entre 1 y 30");
        }
        Inmueble inmueble = inmuebles.findById(inmuebleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "El inmueble no existe"));
        if (egresos.existsByInmuebleIdAndPeriodoAndDias(inmuebleId, periodo, dias)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ya existe un comprobante de este inmueble para " + periodo + " por " + dias + " días");
        }

        BigDecimal bruto = inmueble.getCanon().multiply(BigDecimal.valueOf(dias)).divide(BigDecimal.valueOf(30), 0, RoundingMode.HALF_UP);
        BigDecimal admin = bruto.multiply(inmueble.getPctAdminEgreso()).divide(CIEN, 0, RoundingMode.HALF_UP);
        BigDecimal otros = otrosDescuentos == null ? BigDecimal.ZERO : otrosDescuentos;
        BigDecimal total = bruto.subtract(admin).subtract(otros);
        if (total.signum() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Los descuentos superan el valor del arriendo");
        }

        ComprobanteEgreso e = new ComprobanteEgreso();
        e.setConsecutivo(egresos.siguienteConsecutivo());
        e.setFecha(LocalDate.now());
        e.setPeriodo(periodo);
        e.setPropietario(inmueble.getPropietario());
        e.setInmueble(inmueble);
        e.setDias(dias);
        e.setConcepto(concepto == null || concepto.isBlank() ? conceptoPorDefecto(ym, dias, inmueble) : concepto.trim());
        e.setValorBruto(bruto);
        e.setValorAdministracion(admin);
        e.setOtrosDescuentos(otros);
        e.setTotalPagado(total);
        e.setImputacionContable(imputacionContable == null || imputacionContable.isBlank() ? null : imputacionContable.trim());
        try {
            return egresos.save(e);
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ya existe un comprobante de este inmueble para " + periodo + " por " + dias + " días");
        }
    }

    /** Un comprobante (30 dias) por cada cuenta de cobro ya pagada en el periodo que aun no tenga el suyo. */
    @Transactional
    public ResultadoMes generarMes(String periodo) {
        CuentaCobroService.parsePeriodo(periodo);
        int creados = 0;
        int omitidos = 0;
        for (var c : cuentas.porPeriodoYEstado(periodo, EstadoDocumento.PAGADO)) {
            if (egresos.existsByInmuebleIdAndPeriodoAndDias(c.getInmueble().getId(), periodo, 30)) {
                omitidos++;
            } else {
                generar(c.getInmueble().getId(), periodo, 30, null, null, null);
                creados++;
            }
        }
        return new ResultadoMes(creados, omitidos);
    }

    @Transactional(readOnly = true)
    public ComprobanteEgreso obtener(Long id) {
        return egresos.detalle(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "El comprobante de egreso no existe"));
    }

    public byte[] pdf(Long id) {
        return pdf.comprobanteEgreso(obtener(id));
    }

    /** No es transaccional a proposito: no se retiene una conexion de BD mientras se habla con el servidor SMTP. */
    public ComprobanteEgreso enviar(Long id) {
        if (!enviando.add(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Este comprobante ya se está enviando");
        }
        try {
            return enviarSinGuarda(id);
        } finally {
            enviando.remove(id);
        }
    }

    private ComprobanteEgreso enviarSinGuarda(Long id) {
        ComprobanteEgreso e = obtener(id);
        String para = e.getPropietario().getEmail();
        if (para == null || para.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El propietario no tiene correo registrado");
        }
        String asunto = "Comprobante de egreso No. " + e.getConsecutivo() + " - " + e.getPeriodo();
        String cuerpo = "Cordial saludo, " + e.getPropietario().getNombre().replaceAll("[\\r\\n]+", " ") + ".\n\n"
                + "Adjuntamos el comprobante de egreso No. " + e.getConsecutivo() + " correspondiente al pago del arriendo de "
                + e.getPeriodo() + ".\n\n"
                + "Cualquier inquietud, escríbanos a " + empresa.email() + ".\n\n" + empresa.nombre();
        mail.enviarConAdjunto(para, asunto, cuerpo, "comprobante-egreso-" + e.getConsecutivo() + ".pdf", pdf.comprobanteEgreso(e));

        if (e.getEstado() == EstadoDocumento.BORRADOR) e.setEstado(EstadoDocumento.ENVIADO);
        e.setEnviadoEn(LocalDateTime.now());
        return egresos.save(e);
    }

    @Transactional
    public ComprobanteEgreso pagar(Long id) {
        // Actualizacion atomica: si dos clics llegan a la vez, solo uno cambia la fila; el otro recibe 409.
        if (egresos.marcarPagado(id) == 0) {
            obtener(id); // 404 si no existe
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El comprobante de egreso ya está pagado");
        }
        ComprobanteEgreso e = obtener(id);

        MovimientoBanco m = new MovimientoBanco();
        m.setFecha(LocalDate.now());
        m.setConcepto("pago de arriendo " + e.getInmueble().getDescripcion() + " (" + e.getPropietario().getNombre() + ")");
        m.setGasto(e.getTotalPagado());
        m.setAdministracion(e.getValorAdministracion());
        m.setComprobanteEgreso(e);
        movimientos.save(m);
        return e;
    }

    static String conceptoPorDefecto(YearMonth ym, int dias, Inmueble i) {
        String mes = ym.getMonth().getDisplayName(TextStyle.FULL, Locale.forLanguageTag("es-CO")).toUpperCase(Locale.ROOT);
        String parcial = dias == 30 ? "" : " POR " + dias + " DÍAS";
        return "PAGO DE ALQUILER DE ARRENDAMIENTO DEL MES DE " + mes + " DE " + ym.getYear() + parcial
                + " - " + i.getDescripcion() + ", " + i.getDireccion();
    }
}
