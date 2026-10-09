package co.inmobiliaria360.service;

import co.inmobiliaria360.repository.ComprobanteEgresoRepository;
import co.inmobiliaria360.repository.CuentaCobroRepository;
import co.inmobiliaria360.repository.InmuebleRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Datos del panel principal: lo que el equipo necesita ver y hacer hoy. */
@Service
public class PanelService {

    public record Cobro(int emitidas, BigDecimal valorEmitido, int pagadas, BigDecimal valorRecaudado,
                        int porCobrar, BigDecimal valorPorCobrar, int sinEnviar, int porcentajeRecaudo) {}

    public record Egresos(int emitidos, BigDecimal valorTotal, int pagados, BigDecimal valorPagado,
                          int porPagar, BigDecimal valorPorPagar) {}

    public record Banco(BigDecimal saldoFinal, BigDecimal ingresos, BigDecimal gastos) {}

    public record Alerta(String nivel, String texto, String enlace) {}

    public record Panel(String periodo, Cobro cobro, Egresos egresos, Banco banco,
                        CarteraService.Cartera cartera, int inmueblesSinCuenta,
                        int propietariosPorPagar, BigDecimal valorPropietariosPorPagar,
                        List<Alerta> alertas, List<BancoService.Linea> ultimosMovimientos) {}

    private final CuentaCobroRepository cuentas;
    private final ComprobanteEgresoRepository egresos;
    private final InmuebleRepository inmuebles;
    private final CarteraService carteraService;
    private final BancoService bancoService;
    private final Clock reloj;

    public PanelService(CuentaCobroRepository cuentas, ComprobanteEgresoRepository egresos, InmuebleRepository inmuebles,
                        CarteraService carteraService, BancoService bancoService, Clock reloj) {
        this.cuentas = cuentas;
        this.egresos = egresos;
        this.inmuebles = inmuebles;
        this.carteraService = carteraService;
        this.bancoService = bancoService;
        this.reloj = reloj;
    }

    @Transactional(readOnly = true)
    public Panel panel(String periodo) {
        if (periodo == null || periodo.isBlank()) periodo = YearMonth.now(reloj).toString();
        CuentaCobroService.parsePeriodo(periodo);

        var resumenCobro = cuentas.resumenPorEstado(periodo);
        Map<String, long[]> c = conteos(resumenCobro);
        Map<String, BigDecimal> cv = sumas(resumenCobro);
        int emitidas = (int) (cnt(c, "BORRADOR") + cnt(c, "ENVIADO") + cnt(c, "PAGADO"));
        BigDecimal valorEmitido = cv.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal recaudado = cv.getOrDefault("PAGADO", BigDecimal.ZERO);
        int pagadas = (int) cnt(c, "PAGADO");
        int porCobrar = emitidas - pagadas;
        Cobro cobro = new Cobro(emitidas, valorEmitido, pagadas, recaudado, porCobrar, valorEmitido.subtract(recaudado),
                (int) cnt(c, "BORRADOR"), valorEmitido.signum() == 0 ? 0 : recaudado.multiply(BigDecimal.valueOf(100)).divide(valorEmitido, 0, java.math.RoundingMode.HALF_UP).intValue());

        var resumenEgresos = egresos.resumenPorEstado(periodo);
        Map<String, long[]> e = conteos(resumenEgresos);
        Map<String, BigDecimal> ev = sumas(resumenEgresos);
        int emitidosE = (int) (cnt(e, "BORRADOR") + cnt(e, "ENVIADO") + cnt(e, "PAGADO"));
        BigDecimal totalE = ev.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal pagadoE = ev.getOrDefault("PAGADO", BigDecimal.ZERO);
        Egresos egr = new Egresos(emitidosE, totalE, (int) cnt(e, "PAGADO"), pagadoE, emitidosE - (int) cnt(e, "PAGADO"), totalE.subtract(pagadoE));

        var cuadre = bancoService.cuadre(periodo);
        Banco banco = new Banco(cuadre.saldoFinal(), cuadre.totalIngresos(), cuadre.totalGastos());
        var cartera = carteraService.calcular();

        long conInquilino = inmuebles.countByActivoTrueAndInquilinoIsNotNull();
        int sinCuenta = (int) Math.max(0, conInquilino - cuentas.countByPeriodo(periodo));
        Object[] sinEgreso = cuentas.pagadasSinEgreso(periodo).get(0);
        int porPagarProp = ((Number) sinEgreso[0]).intValue();
        BigDecimal valorPorPagarProp = new BigDecimal(sinEgreso[1].toString());

        List<Alerta> alertas = new ArrayList<>();
        if (cartera.inquilinosEnMora() > 0) {
            alertas.add(new Alerta("alta", cartera.inquilinosEnMora() + " inquilino(s) en mora por un total vencido de " + pesos(cartera.totalVencido()), "/cartera"));
        }
        if (sinCuenta > 0) {
            alertas.add(new Alerta("media", sinCuenta + " inmueble(s) con inquilino aún sin cuenta de cobro este mes", "/cuentas-cobro"));
        }
        if (cobro.sinEnviar() > 0) {
            alertas.add(new Alerta("media", cobro.sinEnviar() + " cuenta(s) de cobro en borrador sin enviar", "/cuentas-cobro"));
        }
        if (porPagarProp > 0) {
            alertas.add(new Alerta("media", porPagarProp + " propietario(s) por pagar: el inquilino ya pagó y falta el comprobante de egreso", "/comprobantes-egreso"));
        }
        if (egr.porPagar() > 0) {
            alertas.add(new Alerta("baja", egr.porPagar() + " comprobante(s) de egreso pendiente(s) de pago", "/comprobantes-egreso"));
        }

        var movs = cuadre.movimientos();
        List<BancoService.Linea> ultimos = movs.subList(Math.max(0, movs.size() - 5), movs.size()).reversed();
        return new Panel(periodo, cobro, egr, banco, cartera, sinCuenta, porPagarProp, valorPorPagarProp, alertas, ultimos);
    }

    private static String pesos(BigDecimal valor) {
        var f = java.text.NumberFormat.getCurrencyInstance(java.util.Locale.forLanguageTag("es-CO"));
        f.setMaximumFractionDigits(0);
        return f.format(valor);
    }

    private static Map<String, long[]> conteos(List<Object[]> filas) {
        Map<String, long[]> m = new HashMap<>();
        for (Object[] f : filas) m.put(f[0].toString(), new long[] {((Number) f[1]).longValue()});
        return m;
    }

    private static Map<String, BigDecimal> sumas(List<Object[]> filas) {
        Map<String, BigDecimal> m = new HashMap<>();
        for (Object[] f : filas) m.put(f[0].toString(), new BigDecimal(f[2].toString()));
        return m;
    }

    private static long cnt(Map<String, long[]> m, String estado) {
        long[] v = m.get(estado);
        return v == null ? 0 : v[0];
    }
}
