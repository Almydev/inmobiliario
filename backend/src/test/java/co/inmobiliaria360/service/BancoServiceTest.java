package co.inmobiliaria360.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.inmobiliaria360.domain.ComprobanteEgreso;
import co.inmobiliaria360.domain.MovimientoBanco;
import co.inmobiliaria360.repository.MovimientoBancoRepository;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class BancoServiceTest {

    private MovimientoBancoRepository repo;
    private BancoService servicio;

    @BeforeEach
    void preparar() {
        repo = mock(MovimientoBancoRepository.class);
        servicio = new BancoService(repo);
    }

    private static MovimientoBanco mov(long id, String fecha, String concepto, String ingreso, String gasto) {
        var m = new MovimientoBanco();
        m.setId(id);
        m.setFecha(LocalDate.parse(fecha));
        m.setConcepto(concepto);
        m.setIngreso(new BigDecimal(ingreso));
        m.setGasto(new BigDecimal(gasto));
        return m;
    }

    private static BigDecimal n(String v) {
        return new BigDecimal(v);
    }

    @Test
    void saldoAcumuladoComoLaHojaCuadreDeBancoDelExcel() {
        when(repo.saldoAntesDe(LocalDate.of(2026, 9, 1))).thenReturn(BigDecimal.ZERO);
        when(repo.entre(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30))).thenReturn(List.of(
                mov(1, "2026-09-05", "pago de gravamen", "0", "7560"),
                mov(2, "2026-09-05", "pago de arriendo casa villalaura", "1500000", "0"),
                mov(3, "2026-09-07", "pago de arrendamiento casa pinolinda", "2500000", "0"),
                mov(4, "2026-09-07", "pago de arrendamiento apartamento ara", "0", "1485000"),
                mov(5, "2026-09-07", "pago de arriendo apto 402 portobello", "0", "1575000")));

        var c = servicio.cuadre("2026-09");

        assertEquals(0, n("-7560").compareTo(c.movimientos().get(0).saldo()));
        assertEquals(0, n("1492440").compareTo(c.movimientos().get(1).saldo()));
        assertEquals(0, n("3992440").compareTo(c.movimientos().get(2).saldo()));
        assertEquals(0, n("932440").compareTo(c.saldoFinal()));
        assertEquals(0, n("4000000").compareTo(c.totalIngresos()));
        assertEquals(0, n("3067560").compareTo(c.totalGastos()));
        assertTrue(c.movimientos().get(0).manual());
    }

    @Test
    void elSaldoInicialArrastraLosMesesAnteriores() {
        when(repo.saldoAntesDe(LocalDate.of(2026, 10, 1))).thenReturn(n("932440"));
        when(repo.entre(any(), any())).thenReturn(List.of(mov(9, "2026-10-02", "pago", "100000", "0")));

        var c = servicio.cuadre("2026-10");

        assertEquals(0, n("932440").compareTo(c.saldoInicial()));
        assertEquals(0, n("1032440").compareTo(c.saldoFinal()));
    }

    @Test
    void mesSinMovimientosConservaElSaldo() {
        when(repo.saldoAntesDe(any())).thenReturn(n("500"));
        when(repo.entre(any(), any())).thenReturn(List.of());
        var c = servicio.cuadre("2026-11");
        assertEquals(0, n("500").compareTo(c.saldoFinal()));
        assertTrue(c.movimientos().isEmpty());
    }

    @Test
    void periodoInvalidoSeRechaza() {
        assertThrows(ResponseStatusException.class, () -> servicio.cuadre("2026-13"));
        verify(repo, never()).entre(any(), any());
    }

    @Test
    void movimientoManualDebeSerIngresoOGastoNoAmbosNiNinguno() {
        var f = LocalDate.of(2026, 9, 5);
        assertThrows(ResponseStatusException.class, () -> servicio.crearManual(f, "x", n("10"), n("10"), null));
        assertThrows(ResponseStatusException.class, () -> servicio.crearManual(f, "x", null, null, null));
        assertThrows(ResponseStatusException.class, () -> servicio.crearManual(f, "x", n("-1"), null, null));
        verify(repo, never()).save(any());
    }

    @Test
    void movimientoManualValidoSeGuarda() {
        when(repo.save(any(MovimientoBanco.class))).thenAnswer(i -> i.getArgument(0));
        var m = servicio.crearManual(LocalDate.of(2026, 9, 5), "  pago de gravamen ", null, n("7560"), null);
        assertEquals("pago de gravamen", m.getConcepto());
        assertEquals(0, n("7560").compareTo(m.getGasto()));
    }

    @Test
    void soloSePuedenEliminarMovimientosManuales() {
        var deDocumento = mov(1, "2026-09-05", "egreso", "0", "100");
        deDocumento.setComprobanteEgreso(new ComprobanteEgreso());
        when(repo.findById(1L)).thenReturn(Optional.of(deDocumento));
        var ex = assertThrows(ResponseStatusException.class, () -> servicio.eliminarManual(1L));
        assertEquals(409, ex.getStatusCode().value());
        verify(repo, never()).delete(any());

        var manual = mov(2, "2026-09-05", "gravamen", "0", "7");
        when(repo.findById(2L)).thenReturn(Optional.of(manual));
        servicio.eliminarManual(2L);
        verify(repo).delete(manual);
    }

    @Test
    void csvNeutralizaFormulasYEscapaComillas() {
        when(repo.saldoAntesDe(any())).thenReturn(BigDecimal.ZERO);
        when(repo.entre(any(), any())).thenReturn(List.of(
                mov(1, "2026-09-05", "=HYPERLINK(\"http://malo.test\")", "10", "0"),
                mov(2, "2026-09-06", "pago \"especial\"\nlinea", "0", "5")));

        String csv = new String(servicio.csv("2026-09"), StandardCharsets.UTF_8);

        assertTrue(csv.startsWith("﻿FECHA;CONCEPTO"));
        assertTrue(csv.contains("\"'=HYPERLINK(\"\"http://malo.test\"\")\""), csv);
        assertTrue(csv.contains("\"pago \"\"especial\"\" linea\""), csv);
        assertFalse(csv.contains("\"=HYPERLINK"), csv);
    }
}
