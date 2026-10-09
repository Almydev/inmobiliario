package co.inmobiliaria360.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import co.inmobiliaria360.PruebaH2;
import co.inmobiliaria360.domain.Inmueble;
import co.inmobiliaria360.domain.Inquilino;
import co.inmobiliaria360.domain.Propietario;
import co.inmobiliaria360.repository.ComprobanteEgresoRepository;
import co.inmobiliaria360.repository.CuentaCobroRepository;
import co.inmobiliaria360.repository.InmuebleRepository;
import co.inmobiliaria360.repository.InquilinoRepository;
import co.inmobiliaria360.repository.MovimientoBancoRepository;
import co.inmobiliaria360.repository.PropietarioRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/** Panel y cartera contra datos reales en H2: los numeros deben cuadrar con los documentos. */
@PruebaH2
class PanelServiceTest {

    private static final String P = "2026-10";

    @Autowired PanelService panel;
    @Autowired CarteraService cartera;
    @Autowired CuentaCobroService cuentaService;
    @Autowired ComprobanteEgresoService egresoService;
    @Autowired CuentaCobroRepository cuentas;
    @Autowired ComprobanteEgresoRepository egresos;
    @Autowired MovimientoBancoRepository movimientos;
    @Autowired InmuebleRepository inmuebles;
    @Autowired PropietarioRepository propietarios;
    @Autowired InquilinoRepository inquilinos;
    @MockitoBean MailService mail;

    private final List<Long> ids = new ArrayList<>();
    private int n;

    @BeforeEach
    void datos() {
        movimientos.deleteAll();
        egresos.deleteAll();
        cuentas.deleteAll();
        inmuebles.deleteAll();
        inquilinos.deleteAll();
        propietarios.deleteAll();
        ids.clear();

        for (int k = 1; k <= 3; k++) { // 3 inmuebles de 1.000.000 / 2.000.000 / 3.000.000
            n++;
            var p = new Propietario();
            p.setNombre("Prop " + k);
            p.setDocumento("PP" + n);
            p = propietarios.save(p);
            var i = new Inquilino();
            i.setNombre("Inq " + k);
            i.setDocumento("II" + n);
            i = inquilinos.save(i);
            var m = new Inmueble();
            m.setDescripcion("Inmueble " + k);
            m.setDireccion("Calle " + k);
            m.setPropietario(p);
            m.setInquilino(i);
            m.setCanon(new BigDecimal(k * 1_000_000));
            ids.add(inmuebles.save(m).getId());
        }
    }

    @Test
    void panelCuadraConLosDocumentos() {
        Long c1 = cuentaService.generar(ids.get(0), P, false, null, null).getId();
        Long c2 = cuentaService.generar(ids.get(1), P, false, null, null).getId();
        cuentaService.generar(ids.get(2), P, false, null, null);
        cuentaService.pagar(c1);   // 1.000.000 recaudado
        cuentaService.pagar(c2);   // + 2.000.000
        egresoService.generar(ids.get(0), P, 30, null, null, null); // 900.000 por pagar (queda un propietario sin egreso: el 2)

        var p = panel.panel(P);

        assertEquals(3, p.cobro().emitidas());
        assertEquals(0, new BigDecimal("6000000").compareTo(p.cobro().valorEmitido()));
        assertEquals(2, p.cobro().pagadas());
        assertEquals(0, new BigDecimal("3000000").compareTo(p.cobro().valorRecaudado()));
        assertEquals(1, p.cobro().porCobrar());
        assertEquals(0, new BigDecimal("3000000").compareTo(p.cobro().valorPorCobrar()));
        assertEquals(50, p.cobro().porcentajeRecaudo());
        assertEquals(1, p.cobro().sinEnviar(), "una cuenta sigue en borrador");

        assertEquals(1, p.egresos().emitidos());
        assertEquals(0, new BigDecimal("900000").compareTo(p.egresos().valorTotal()));
        assertEquals(1, p.egresos().porPagar());

        assertEquals(1, p.propietariosPorPagar(), "el inmueble 2 ya pago el inquilino y no tiene egreso");
        assertEquals(0, new BigDecimal("2000000").compareTo(p.valorPropietariosPorPagar()));

        assertEquals(0, new BigDecimal("3000000").compareTo(p.banco().ingresos()));
        assertEquals(0, new BigDecimal("3000000").compareTo(p.banco().saldoFinal()));
        assertEquals(2, p.ultimosMovimientos().size());
        assertTrue(p.alertas().stream().anyMatch(a -> a.enlace().equals("/comprobantes-egreso")));
    }

    @Test
    void inmueblesConInquilinoSinCuentaSeSenalan() {
        cuentaService.generar(ids.get(0), P, false, null, null);
        var p = panel.panel(P);
        assertEquals(2, p.inmueblesSinCuenta());
        assertTrue(p.alertas().stream().anyMatch(a -> a.texto().contains("sin cuenta de cobro")));
    }

    @Test
    void mesSinActividadNoFallaYSaleEnCero() {
        var p = panel.panel("2030-01");
        assertEquals(0, p.cobro().emitidas());
        assertEquals(0, p.cobro().porcentajeRecaudo());
        assertEquals(0, p.egresos().emitidos());
        assertEquals(0, p.propietariosPorPagar());
        assertTrue(p.ultimosMovimientos().isEmpty());
    }

    @Test
    void carteraListaSoloLoNoPagado() {
        Long c1 = cuentaService.generar(ids.get(0), "2020-01", false, null, null).getId();
        cuentaService.generar(ids.get(1), "2020-01", false, null, null);
        cuentaService.pagar(c1);

        var c = cartera.calcular();

        assertEquals(1, c.cuentasPendientes());
        assertEquals("Inq 2", c.inquilinos().get(0).nombre());
        assertEquals(0, new BigDecimal("2000000").compareTo(c.totalPendiente()));
        assertTrue(c.inquilinos().get(0).maxDiasMora() > 90);
        assertEquals(1, c.rangos().get(4).cuentas(), "es una mora de mas de 90 dias");
    }
}
