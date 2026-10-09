package co.inmobiliaria360.service;

import co.inmobiliaria360.domain.MovimientoBanco;
import co.inmobiliaria360.repository.MovimientoBancoRepository;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Cuadre de banco: el saldo no se guarda, se calcula acumulando ingreso - gasto en orden de fecha e id. */
@Service
public class BancoService {

    public record Linea(Long id, LocalDate fecha, String concepto, BigDecimal administracion, BigDecimal ingreso,
                        BigDecimal gasto, BigDecimal saldo, String origen, boolean manual) {}

    public record Cuadre(String periodo, BigDecimal saldoInicial, BigDecimal totalIngresos, BigDecimal totalGastos,
                         BigDecimal totalAdministracion, BigDecimal saldoFinal, List<Linea> movimientos) {}

    private final MovimientoBancoRepository repo;

    public BancoService(MovimientoBancoRepository repo) {
        this.repo = repo;
    }

    @Transactional(readOnly = true)
    public Cuadre cuadre(String periodo) {
        YearMonth ym = CuentaCobroService.parsePeriodo(periodo);
        LocalDate desde = ym.atDay(1);
        BigDecimal saldo = repo.saldoAntesDe(desde);
        BigDecimal inicial = saldo;
        BigDecimal ingresos = BigDecimal.ZERO;
        BigDecimal gastos = BigDecimal.ZERO;
        BigDecimal admin = BigDecimal.ZERO;

        List<Linea> lineas = new ArrayList<>();
        for (MovimientoBanco m : repo.entre(desde, ym.atEndOfMonth())) {
            saldo = saldo.add(m.getIngreso()).subtract(m.getGasto());
            ingresos = ingresos.add(m.getIngreso());
            gastos = gastos.add(m.getGasto());
            admin = admin.add(m.getAdministracion());
            lineas.add(new Linea(m.getId(), m.getFecha(), m.getConcepto(), m.getAdministracion(), m.getIngreso(),
                    m.getGasto(), saldo, origen(m), m.getCuentaCobro() == null && m.getComprobanteEgreso() == null));
        }
        return new Cuadre(periodo, inicial, ingresos, gastos, admin, saldo, lineas);
    }

    @Transactional
    public MovimientoBanco crearManual(LocalDate fecha, String concepto, BigDecimal ingreso, BigDecimal gasto, BigDecimal administracion) {
        BigDecimal i = ingreso == null ? BigDecimal.ZERO : ingreso;
        BigDecimal g = gasto == null ? BigDecimal.ZERO : gasto;
        if (i.signum() < 0 || g.signum() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Los valores no pueden ser negativos");
        }
        if (i.signum() > 0 && g.signum() > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Un movimiento es un ingreso o un gasto, no ambos");
        }
        if (i.signum() == 0 && g.signum() == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Indica el valor del ingreso o del gasto");
        }
        MovimientoBanco m = new MovimientoBanco();
        m.setFecha(fecha);
        m.setConcepto(concepto.trim());
        m.setIngreso(i);
        m.setGasto(g);
        m.setAdministracion(administracion == null ? BigDecimal.ZERO : administracion);
        return repo.save(m);
    }

    @Transactional
    public void eliminarManual(Long id) {
        MovimientoBanco m = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "El movimiento no existe"));
        if (m.getCuentaCobro() != null || m.getComprobanteEgreso() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Este movimiento viene de un documento y no se puede eliminar");
        }
        repo.delete(m);
    }

    /** CSV compatible con Excel (UTF-8 con BOM, separador ;). Neutraliza celdas que empiezan con = + - @ (inyeccion de formulas). */
    public byte[] csv(String periodo) {
        Cuadre c = cuadre(periodo);
        StringBuilder sb = new StringBuilder("﻿");
        sb.append("FECHA;CONCEPTO;ADMINISTRACION;INGRESO;GASTO;SALDO\r\n");
        sb.append(";Saldo inicial;;;;").append(c.saldoInicial().toPlainString()).append("\r\n");
        for (Linea l : c.movimientos()) {
            sb.append(l.fecha()).append(';')
                    .append(celda(l.concepto())).append(';')
                    .append(l.administracion().toPlainString()).append(';')
                    .append(l.ingreso().toPlainString()).append(';')
                    .append(l.gasto().toPlainString()).append(';')
                    .append(l.saldo().toPlainString()).append("\r\n");
        }
        sb.append(";Totales;").append(c.totalAdministracion().toPlainString()).append(';')
                .append(c.totalIngresos().toPlainString()).append(';')
                .append(c.totalGastos().toPlainString()).append(';')
                .append(c.saldoFinal().toPlainString()).append("\r\n");
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    static String celda(String texto) {
        String t = texto == null ? "" : texto.replace("\r", " ").replace("\n", " ");
        if (!t.isEmpty() && "=+-@\t".indexOf(t.charAt(0)) >= 0) t = "'" + t;
        return "\"" + t.replace("\"", "\"\"") + "\"";
    }

    private static String origen(MovimientoBanco m) {
        if (m.getCuentaCobro() != null) return "Cuenta de cobro " + m.getCuentaCobro().getConsecutivo();
        if (m.getComprobanteEgreso() != null) return "Egreso " + m.getComprobanteEgreso().getConsecutivo();
        return "Manual";
    }
}
