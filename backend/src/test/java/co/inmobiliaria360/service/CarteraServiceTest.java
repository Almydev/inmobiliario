package co.inmobiliaria360.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import co.inmobiliaria360.domain.CuentaCobro;
import co.inmobiliaria360.domain.EstadoDocumento;
import co.inmobiliaria360.domain.Inmueble;
import co.inmobiliaria360.domain.Inquilino;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class CarteraServiceTest {

    private static final LocalDate HOY = LocalDate.of(2026, 10, 20);

    private static Inquilino inquilino(long id, String nombre) {
        var i = new Inquilino();
        i.setId(id);
        i.setNombre(nombre);
        i.setEmail(nombre.toLowerCase() + "@ejemplo.test");
        return i;
    }

    private static CuentaCobro cuenta(long id, Inquilino i, String periodo, String total, EstadoDocumento estado) {
        var inm = new Inmueble();
        inm.setId(id);
        inm.setDescripcion("Inmueble " + id);
        var c = new CuentaCobro();
        c.setId(id);
        c.setConsecutivo(id);
        c.setPeriodo(periodo);
        c.setInquilino(i);
        c.setInmueble(inm);
        c.setTotal(new BigDecimal(total));
        c.setEstado(estado);
        return c;
    }

    @Test
    void clasificaLaMoraPorAntiguedadDesdeElDia5() {
        var ana = inquilino(1, "Ana");
        var luis = inquilino(2, "Luis");
        var eva = inquilino(3, "Eva");
        var sin = inquilino(4, "Sin");
        var cuentas = List.of(
                cuenta(1, ana, "2026-10", "1000000", EstadoDocumento.ENVIADO),   // vence 05-oct: 15 dias de mora
                cuenta(2, luis, "2026-08", "2000000", EstadoDocumento.ENVIADO),  // vence 05-ago: 76 dias
                cuenta(3, eva, "2026-06", "500000", EstadoDocumento.BORRADOR),   // vence 05-jun: 137 dias
                cuenta(4, sin, "2026-11", "700000", EstadoDocumento.BORRADOR));  // vence 05-nov: al dia

        var c = CarteraService.calcular(cuentas, HOY, 5);

        assertEquals(0, new BigDecimal("4200000").compareTo(c.totalPendiente()));
        assertEquals(0, new BigDecimal("3500000").compareTo(c.totalVencido()));
        assertEquals(4, c.cuentasPendientes());
        assertEquals(3, c.inquilinosEnMora());
        assertEquals(1, c.rangos().get(0).cuentas()); // al dia
        assertEquals(1, c.rangos().get(1).cuentas()); // 1-30
        assertEquals(0, c.rangos().get(2).cuentas()); // 31-60
        assertEquals(1, c.rangos().get(3).cuentas()); // 61-90
        assertEquals(1, c.rangos().get(4).cuentas()); // +90
        assertEquals("Eva", c.inquilinos().get(0).nombre(), "el mas atrasado va primero");
        assertEquals(137, c.inquilinos().get(0).maxDiasMora());
    }

    @Test
    void agrupaVariasCuentasDelMismoInquilino() {
        var ana = inquilino(1, "Ana");
        var c = CarteraService.calcular(List.of(
                cuenta(1, ana, "2026-09", "1000000", EstadoDocumento.ENVIADO),
                cuenta(2, ana, "2026-10", "1000000", EstadoDocumento.ENVIADO)), HOY, 5);

        assertEquals(1, c.inquilinos().size());
        var i = c.inquilinos().get(0);
        assertEquals(2, i.cuentas().size());
        assertEquals(0, new BigDecimal("2000000").compareTo(i.totalPendiente()));
        assertEquals(LocalDate.of(2026, 9, 5), i.cuentas().get(0).vencimiento(), "la mas antigua primero");
        assertEquals(45, i.cuentas().get(0).diasMora());
    }

    @Test
    void sinPendientesTodoEnCero() {
        var c = CarteraService.calcular(List.of(), HOY, 5);
        assertEquals(0, BigDecimal.ZERO.compareTo(c.totalPendiente()));
        assertEquals(0, c.inquilinosEnMora());
        assertTrue(c.inquilinos().isEmpty());
        assertEquals(5, c.rangos().size());
    }

    @Test
    void elDiaDeVencimientoNoCuentaComoMora() {
        var ana = inquilino(1, "Ana");
        var c = CarteraService.calcular(List.of(cuenta(1, ana, "2026-10", "100", EstadoDocumento.ENVIADO)), LocalDate.of(2026, 10, 5), 5);
        assertEquals(0, c.inquilinosEnMora());
        assertEquals(0, c.inquilinos().get(0).maxDiasMora());
    }

    @Test
    void elDiaDeVencimientoSeAjustaAMesesCortos() {
        assertEquals(LocalDate.of(2026, 2, 28), CarteraService.vencimiento("2026-02", 31));
    }
}
