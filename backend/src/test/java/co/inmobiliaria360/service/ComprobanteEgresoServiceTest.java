package co.inmobiliaria360.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.inmobiliaria360.domain.ComprobanteEgreso;
import co.inmobiliaria360.domain.EstadoDocumento;
import co.inmobiliaria360.domain.Inmueble;
import co.inmobiliaria360.domain.MovimientoBanco;
import co.inmobiliaria360.domain.Propietario;
import co.inmobiliaria360.repository.ComprobanteEgresoRepository;
import co.inmobiliaria360.repository.CuentaCobroRepository;
import co.inmobiliaria360.repository.InmuebleRepository;
import co.inmobiliaria360.repository.MovimientoBancoRepository;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.server.ResponseStatusException;

class ComprobanteEgresoServiceTest {

    private ComprobanteEgresoRepository egresos;
    private MovimientoBancoRepository movimientos;
    private InmuebleRepository inmuebles;
    private ComprobanteEgresoService servicio;

    @BeforeEach
    void preparar() {
        egresos = mock(ComprobanteEgresoRepository.class);
        movimientos = mock(MovimientoBancoRepository.class);
        inmuebles = mock(InmuebleRepository.class);
        var empresa = Fixtures.empresa();
        servicio = new ComprobanteEgresoService(egresos, inmuebles, mock(CuentaCobroRepository.class), movimientos,
                new PdfService(empresa), mock(MailService.class), empresa);

        when(egresos.siguienteConsecutivo()).thenReturn(114L);
        when(egresos.save(any(ComprobanteEgreso.class))).thenAnswer(i -> i.getArgument(0));
    }

    private Inmueble inmueble(String canon, String pctEgreso) {
        var p = new Propietario();
        p.setId(1L);
        p.setNombre("LUZ ORFILIA GOMEZ DE GARCIA");
        p.setDocumento("22108958");
        var i = new Inmueble();
        i.setId(5L);
        i.setDescripcion("Casa Ciudad Jardín # 1");
        i.setDireccion("Calle 27 # 9-112");
        i.setPropietario(p);
        i.setCanon(new BigDecimal(canon));
        i.setPctAdminEgreso(new BigDecimal(pctEgreso));
        when(inmuebles.findById(5L)).thenReturn(Optional.of(i));
        return i;
    }

    @Test
    void ejemploDelExcel_15DiasDeUnCanonDe3500000() {
        inmueble("3500000", "10");
        ComprobanteEgreso e = servicio.generar(5L, "2026-09", 15, null, null, null);

        assertEquals(114L, e.getConsecutivo());
        assertEquals(0, new BigDecimal("1750000").compareTo(e.getValorBruto()));          // 3.500.000 / 30 x 15
        assertEquals(0, new BigDecimal("175000").compareTo(e.getValorAdministracion()));  // 10 %
        assertEquals(0, new BigDecimal("1575000").compareTo(e.getTotalPagado()));         // igual al cuadre de banco
        assertTrue(e.getConcepto().contains("SEPTIEMBRE DE 2026 POR 15 DÍAS"), e.getConcepto());
        assertEquals(EstadoDocumento.BORRADOR, e.getEstado());
    }

    @Test
    void mesCompletoYDescuentosAdicionales() {
        inmueble("2000000", "10");
        ComprobanteEgreso e = servicio.generar(5L, "2026-10", 30, new BigDecimal("50000"), null, "Gasto 5130");
        assertEquals(0, new BigDecimal("2000000").compareTo(e.getValorBruto()));
        assertEquals(0, new BigDecimal("1750000").compareTo(e.getTotalPagado())); // 2.000.000 - 200.000 - 50.000
        assertEquals("Gasto 5130", e.getImputacionContable());
        assertTrue(!e.getConcepto().contains("DÍAS"));
    }

    @Test
    void redondeaALosPesos() {
        inmueble("1000000", "10");
        ComprobanteEgreso e = servicio.generar(5L, "2026-10", 7, null, null, null); // 233.333,33 -> 233.333
        assertEquals(0, new BigDecimal("233333").compareTo(e.getValorBruto()));
    }

    @Test
    void rechazaDescuentosMayoresAlArriendo() {
        inmueble("1000000", "10");
        var ex = assertThrows(ResponseStatusException.class,
                () -> servicio.generar(5L, "2026-10", 30, new BigDecimal("2000000"), null, null));
        assertEquals(400, ex.getStatusCode().value());
        verify(egresos, never()).save(any());
    }

    @Test
    void rechazaDiasFueraDeRangoYPeriodoMalo() {
        inmueble("1000000", "10");
        assertThrows(ResponseStatusException.class, () -> servicio.generar(5L, "2026-10", 0, null, null, null));
        assertThrows(ResponseStatusException.class, () -> servicio.generar(5L, "2026-10", 31, null, null, null));
        assertThrows(ResponseStatusException.class, () -> servicio.generar(5L, "2026-13", 30, null, null, null));
    }

    @Test
    void noDuplicaElMismoPeriodoYDias() {
        inmueble("1000000", "10");
        when(egresos.existsByInmuebleIdAndPeriodoAndDias(5L, "2026-10", 30)).thenReturn(true);
        var ex = assertThrows(ResponseStatusException.class, () -> servicio.generar(5L, "2026-10", 30, null, null, null));
        assertEquals(409, ex.getStatusCode().value());
    }

    @Test
    void pagarRegistraElGastoEnElBancoYNoSePuedePagarDosVeces() {
        inmueble("3500000", "10");
        ComprobanteEgreso e = servicio.generar(5L, "2026-09", 15, null, null, null);
        when(egresos.detalle(any())).thenReturn(Optional.of(e));

        servicio.pagar(1L);

        var cap = ArgumentCaptor.forClass(MovimientoBanco.class);
        verify(movimientos).save(cap.capture());
        assertEquals(0, new BigDecimal("1575000").compareTo(cap.getValue().getGasto()));
        assertEquals(0, BigDecimal.ZERO.compareTo(cap.getValue().getIngreso()));
        assertEquals(EstadoDocumento.PAGADO, e.getEstado());

        var ex = assertThrows(ResponseStatusException.class, () -> servicio.pagar(1L));
        assertEquals(409, ex.getStatusCode().value());
    }

    @Test
    void elPdfDelComprobanteTrae_beneficiario_valorYTotal() throws Exception {
        inmueble("3500000", "10");
        ComprobanteEgreso e = servicio.generar(5L, "2026-09", 15, null, null, null);

        byte[] pdf = new PdfService(Fixtures.empresa()).comprobanteEgreso(e);

        PdfReader lector = new PdfReader(pdf);
        String t = new PdfTextExtractor(lector).getTextFromPage(1);
        lector.close();
        assertTrue(t.contains("COMPROBANTE DE EGRESO"), t);
        assertTrue(t.contains("LUZ ORFILIA GOMEZ DE GARCIA"), t);
        assertTrue(t.contains("22108958"), t);
        assertTrue(t.contains("1.575.000"), t);
        assertTrue(t.contains("UN MILLON QUINIENTOS SETENTA Y CINCO MIL PESOS"), t);
    }
}
