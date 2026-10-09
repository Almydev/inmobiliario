package co.inmobiliaria360.service;

import co.inmobiliaria360.domain.CuentaCobro;
import co.inmobiliaria360.repository.CuentaCobroRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cartera: cuentas de cobro aun no pagadas, agrupadas por inquilino y por antiguedad de la mora.
 * Una cuenta vence el dia N (app.cartera.dia-vencimiento, 5 por defecto) del mes que cobra.
 */
@Service
public class CarteraService {

    public record CuentaPendiente(Long id, Long consecutivo, String periodo, String inmueble, BigDecimal total,
                                  String estado, LocalDate vencimiento, long diasMora) {}

    public record InquilinoCartera(Long inquilinoId, String nombre, String email, String telefono,
                                   BigDecimal totalPendiente, BigDecimal totalVencido, long maxDiasMora,
                                   List<CuentaPendiente> cuentas) {}

    public record Rango(String etiqueta, int cuentas, BigDecimal valor) {}

    public record Cartera(BigDecimal totalPendiente, BigDecimal totalVencido, int cuentasPendientes,
                          int inquilinosEnMora, List<Rango> rangos, List<InquilinoCartera> inquilinos) {}

    private static final String[] ETIQUETAS = {"Al día", "1 a 30 días", "31 a 60 días", "61 a 90 días", "Más de 90 días"};

    private final CuentaCobroRepository cuentas;
    private final Clock reloj;
    private final int diaVencimiento;

    public CarteraService(CuentaCobroRepository cuentas, Clock reloj,
                          @Value("${app.cartera.dia-vencimiento:5}") int diaVencimiento) {
        this.cuentas = cuentas;
        this.reloj = reloj;
        this.diaVencimiento = Math.max(1, Math.min(28, diaVencimiento));
    }

    @Transactional(readOnly = true)
    public Cartera calcular() {
        return calcular(cuentas.pendientes(), LocalDate.now(reloj), diaVencimiento);
    }

    /** Logica pura (sin BD) para poder probarla con fechas fijas. */
    static Cartera calcular(List<CuentaCobro> pendientes, LocalDate hoy, int dia) {
        BigDecimal[] valores = new BigDecimal[ETIQUETAS.length];
        int[] conteo = new int[ETIQUETAS.length];
        for (int i = 0; i < valores.length; i++) valores[i] = BigDecimal.ZERO;

        Map<Long, List<CuentaPendiente>> porInquilino = new LinkedHashMap<>();
        Map<Long, CuentaCobro> muestra = new LinkedHashMap<>();
        BigDecimal total = BigDecimal.ZERO;
        BigDecimal vencido = BigDecimal.ZERO;

        for (CuentaCobro c : pendientes) {
            LocalDate vence = vencimiento(c.getPeriodo(), dia);
            long mora = Math.max(0, ChronoUnit.DAYS.between(vence, hoy));
            int r = rango(mora);
            valores[r] = valores[r].add(c.getTotal());
            conteo[r]++;
            total = total.add(c.getTotal());
            if (mora > 0) vencido = vencido.add(c.getTotal());

            porInquilino.computeIfAbsent(c.getInquilino().getId(), k -> new ArrayList<>()).add(new CuentaPendiente(
                    c.getId(), c.getConsecutivo(), c.getPeriodo(), c.getInmueble().getDescripcion(), c.getTotal(),
                    c.getEstado().name(), vence, mora));
            muestra.putIfAbsent(c.getInquilino().getId(), c);
        }

        List<InquilinoCartera> inquilinos = new ArrayList<>();
        for (var e : porInquilino.entrySet()) {
            var i = muestra.get(e.getKey()).getInquilino();
            List<CuentaPendiente> lista = e.getValue().stream().sorted(Comparator.comparing(CuentaPendiente::vencimiento)).toList();
            BigDecimal suma = lista.stream().map(CuentaPendiente::total).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal sumaVencida = lista.stream().filter(x -> x.diasMora() > 0).map(CuentaPendiente::total).reduce(BigDecimal.ZERO, BigDecimal::add);
            long max = lista.stream().mapToLong(CuentaPendiente::diasMora).max().orElse(0);
            inquilinos.add(new InquilinoCartera(i.getId(), i.getNombre(), i.getEmail(), i.getTelefono(), suma, sumaVencida, max, lista));
        }
        // Primero los que mas deben y mas atrasados
        inquilinos.sort(Comparator.comparing(InquilinoCartera::maxDiasMora).reversed()
                .thenComparing(Comparator.comparing(InquilinoCartera::totalPendiente).reversed()));

        List<Rango> rangos = new ArrayList<>();
        for (int i = 0; i < ETIQUETAS.length; i++) rangos.add(new Rango(ETIQUETAS[i], conteo[i], valores[i]));
        int enMora = (int) inquilinos.stream().filter(x -> x.maxDiasMora() > 0).count();
        return new Cartera(total, vencido, pendientes.size(), enMora, rangos, inquilinos);
    }

    static LocalDate vencimiento(String periodo, int dia) {
        YearMonth ym = YearMonth.parse(periodo);
        return ym.atDay(Math.min(dia, ym.lengthOfMonth()));
    }

    static int rango(long mora) {
        if (mora <= 0) return 0;
        if (mora <= 30) return 1;
        if (mora <= 60) return 2;
        if (mora <= 90) return 3;
        return 4;
    }
}
