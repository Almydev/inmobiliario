package co.inmobiliaria360.demo;

import co.inmobiliaria360.domain.EstadoDocumento;
import co.inmobiliaria360.domain.Inmueble;
import co.inmobiliaria360.domain.Inquilino;
import co.inmobiliaria360.domain.Propietario;
import co.inmobiliaria360.repository.ComprobanteEgresoRepository;
import co.inmobiliaria360.repository.CuentaCobroRepository;
import co.inmobiliaria360.repository.InmuebleRepository;
import co.inmobiliaria360.repository.InquilinoRepository;
import co.inmobiliaria360.repository.MovimientoBancoRepository;
import co.inmobiliaria360.repository.PropietarioRepository;
import co.inmobiliaria360.service.BancoService;
import co.inmobiliaria360.service.ComprobanteEgresoService;
import co.inmobiliaria360.service.CarteraService;
import co.inmobiliaria360.service.CuentaCobroService;
import co.inmobiliaria360.service.PanelService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Carga datos de ejemplo (5 por cada proceso) para probar en QA, o los borra.
 * Apagado por defecto. Activar con SEED_DEMO=true (cargar) o SEED_DEMO_LIMPIAR=true (borrar).
 * Todo lo demo se identifica por el dominio de correo @ejemplo.test y el prefijo "[DEMO]" en los movimientos manuales.
 * NO activar en produccion.
 */
@Component
@ConditionalOnExpression("'${app.seed.demo:false}' == 'true' or '${app.seed.limpiar:false}' == 'true'")
public class DatosDemo implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DatosDemo.class);
    static final String DOMINIO = "@ejemplo.test";
    static final String MARCA = "[DEMO] ";

    private final PropietarioRepository propietarios;
    private final InquilinoRepository inquilinos;
    private final InmuebleRepository inmuebles;
    private final CuentaCobroRepository cuentas;
    private final ComprobanteEgresoRepository egresos;
    private final MovimientoBancoRepository movimientos;
    private final CuentaCobroService cuentaService;
    private final ComprobanteEgresoService egresoService;
    private final BancoService bancoService;
    private final PanelService panelService;
    private final CarteraService carteraService;
    private final TransactionTemplate tx;
    private final boolean limpiar;

    public DatosDemo(PropietarioRepository propietarios, InquilinoRepository inquilinos, InmuebleRepository inmuebles,
                     CuentaCobroRepository cuentas, ComprobanteEgresoRepository egresos, MovimientoBancoRepository movimientos,
                     CuentaCobroService cuentaService, ComprobanteEgresoService egresoService, BancoService bancoService,
                     PanelService panelService, CarteraService carteraService,
                     TransactionTemplate tx, @org.springframework.beans.factory.annotation.Value("${app.seed.limpiar:false}") boolean limpiar) {
        this.propietarios = propietarios;
        this.inquilinos = inquilinos;
        this.inmuebles = inmuebles;
        this.cuentas = cuentas;
        this.egresos = egresos;
        this.movimientos = movimientos;
        this.cuentaService = cuentaService;
        this.egresoService = egresoService;
        this.bancoService = bancoService;
        this.panelService = panelService;
        this.carteraService = carteraService;
        this.tx = tx;
        this.limpiar = limpiar;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (limpiar) {
            tx.executeWithoutResult(s -> borrar());
            return;
        }
        if (propietarios.findByDocumento("900000001").isPresent()) {
            log.warn("Los datos demo ya estaban cargados: no se hace nada. Usa SEED_DEMO_LIMPIAR=true para borrarlos.");
            resumen();
            return;
        }
        cargar();
    }

    private void cargar() {
        YearMonth ym = YearMonth.now();
        String periodo = ym.toString();

        List<Propietario> props = new ArrayList<>();
        props.add(propietario("Marta Elena Ruiz Londoño", "900000001", "marta.ruiz", "Bancolombia", "Ahorros", "00011100001"));
        props.add(propietario("Carlos Andrés Mejía Torres", "900000002", "carlos.mejia", "Davivienda", "Ahorros", "00011100002"));
        props.add(propietario("Inversiones Altos del Retiro S.A.S.", "900000003", "altos.retiro", "Bancolombia", "Corriente", "00011100003"));
        props.add(propietario("Gloria Patricia Henao Giraldo", "900000004", "gloria.henao", "Nequi", "Ahorros", "3000000004"));
        props.add(propietario("Jorge Iván Ospina Cardona", "900000005", "jorge.ospina", "Banco de Bogotá", "Ahorros", "00011100005"));

        List<Inquilino> inqs = new ArrayList<>();
        inqs.add(inquilino("Esteban Rincón Osorio", "910000001", "esteban.rincon", "Calle 17 # 21-45"));
        inqs.add(inquilino("Camilo Paternina Vélez", "910000002", "camilo.paternina", "Carrera 9 # 12-30"));
        inqs.add(inquilino("Angie Tatiana Flórez Calle", "910000003", "angie.florez", "Calle 20 # 18-11"));
        inqs.add(inquilino("Gabriel Vargas Zuluaga", "910000004", "gabriel.vargas", "Carrera 15 # 8-62"));
        inqs.add(inquilino("Fabiola Sarmiento Gómez", "910000005", "fabiola.sarmiento", "Calle 12 # 25-07"));

        List<Inmueble> inms = new ArrayList<>();
        inms.add(inmueble("Apartamento 402 Portobello", "Carrera 10 # 5-20", props.get(0), inqs.get(0), "1750000", "20", "10"));
        inms.add(inmueble("Casa Villalaura", "Calle 15 # 22-40", props.get(1), inqs.get(4), "1500000", "20", "10"));
        inms.add(inmueble("Casa Pinolinda", "Carrera 21 # 16-29", props.get(2), inqs.get(2), "2500000", "20", "10"));
        inms.add(inmueble("Apartamento Ara 301", "Calle 27 # 9-112", props.get(3), inqs.get(1), "1650000", "20", "10"));
        inms.add(inmueble("Local Comercial Centro", "Carrera 20 # 17-05", props.get(4), inqs.get(3), "3500000", "20", "10"));

        // --- 5 cuentas de cobro, una por inmueble, en distintos estados ---
        Long c1 = cuentaService.generar(inms.get(0).getId(), periodo, false, null, null).getId();           // borrador
        Long c2 = cuentaService.generar(inms.get(1).getId(), periodo, true, null, null).getId();            // borrador, con administracion
        Long c3 = cuentaService.generar(inms.get(2).getId(), periodo, false, null, null).getId();           // pagada
        Long c4 = cuentaService.generar(inms.get(3).getId(), periodo, false, new BigDecimal("30000"), "PAGO DE ARRENDAMIENTO MAS CUOTA DE PARQUEADERO").getId(); // pagada
        Long c5 = cuentaService.generar(inms.get(4).getId(), periodo, false, null, null).getId();           // enviada
        cuentaService.pagar(c3);
        cuentaService.pagar(c4);
        marcarEnviada(c5);

        // --- 5 comprobantes de egreso ---
        Long e1 = egresoService.generar(inms.get(2).getId(), periodo, 30, null, null, "Arrendamientos 4120").getId();                 // pagado
        Long e2 = egresoService.generar(inms.get(3).getId(), periodo, 30, null, null, null).getId();                                  // enviado
        egresoService.generar(inms.get(4).getId(), periodo, 15, null, null, null);                                                    // borrador, prorrateado
        egresoService.generar(inms.get(0).getId(), periodo, 30, new BigDecimal("25000"), null, "Mantenimiento 5135");                 // borrador, con descuento
        egresoService.generar(inms.get(1).getId(), periodo, 20, null, null, null);                                                    // borrador, 20 dias
        egresoService.pagar(e1);
        marcarEgresoEnviado(e2);

        // --- 5 movimientos manuales del cuadre de banco (los pagos anteriores ya generaron 3 automaticos) ---
        LocalDate d = ym.atDay(1);
        bancoService.crearManual(d.plusDays(1), MARCA + "pago de gravamen 4x1000", null, new BigDecimal("7560"), null);
        bancoService.crearManual(d.plusDays(2), MARCA + "comisión por transferencias", null, new BigDecimal("12400"), null);
        bancoService.crearManual(d.plusDays(3), MARCA + "intereses ganados cuenta de ahorros", new BigDecimal("18250"), null, null);
        bancoService.crearManual(d.plusDays(4), MARCA + "consignación de arriendo recibido en efectivo", new BigDecimal("500000"), null, null);
        bancoService.crearManual(d.plusDays(5), MARCA + "cuota de manejo tarjeta débito", null, new BigDecimal("19900"), null);

        resumen();
        log.info("Datos demo cargados para {}: 5 propietarios, 5 inquilinos, 5 inmuebles, 5 cuentas de cobro, 5 egresos, 5 movimientos manuales.", periodo);
    }

    private void resumen() {
        String periodo = YearMonth.now().toString();
        var c = bancoService.cuadre(periodo);
        log.info("RESUMEN {} | propietarios={} inquilinos={} inmuebles={} cuentasCobro={} egresos={} movimientos={}",
                periodo, propietarios.count(), inquilinos.count(), inmuebles.count(), cuentas.count(), egresos.count(), movimientos.count());
        cuentas.buscar("", periodo).forEach(x -> log.info("CUENTA No.{} {} | {} | total={} | {}",
                x.getConsecutivo(), x.getInmueble().getDescripcion(), x.getInquilino().getNombre(), x.getTotal(), x.getEstado()));
        egresos.buscar("", periodo).forEach(x -> log.info("EGRESO No.{} {} | {} | dias={} bruto={} admin={} otros={} total={} | {}",
                x.getConsecutivo(), x.getInmueble().getDescripcion(), x.getPropietario().getNombre(), x.getDias(),
                x.getValorBruto(), x.getValorAdministracion(), x.getOtrosDescuentos(), x.getTotalPagado(), x.getEstado()));
        log.info("BANCO {} | inicial={} ingresos={} gastos={} admin={} final={} | lineas={}", periodo, c.saldoInicial(),
                c.totalIngresos(), c.totalGastos(), c.totalAdministracion(), c.saldoFinal(), c.movimientos().size());
        var p = panelService.panel(periodo);
        log.info("PANEL cobro={} egresos={} propietariosPorPagar={} ({}) sinCuenta={}", p.cobro(), p.egresos(),
                p.propietariosPorPagar(), p.valorPropietariosPorPagar(), p.inmueblesSinCuenta());
        p.alertas().forEach(a -> log.info("PANEL alerta [{}] {}", a.nivel(), a.texto()));
        var ca = carteraService.calcular();
        log.info("CARTERA pendiente={} vencido={} cuentas={} enMora={}", ca.totalPendiente(), ca.totalVencido(), ca.cuentasPendientes(), ca.inquilinosEnMora());
        ca.inquilinos().forEach(i -> log.info("CARTERA {} | pendiente={} | mora={} dias | cuentas={}", i.nombre(), i.totalPendiente(), i.maxDiasMora(), i.cuentas().size()));
        c.movimientos().forEach(l -> log.info("  {} | {} | ing={} gas={} saldo={} | {}", l.fecha(), l.concepto(), l.ingreso(), l.gasto(), l.saldo(), l.origen()));
    }

    private void borrar() {
        var demoInmuebles = inmuebles.findAll().stream().filter(i -> i.getPropietario().getEmail() != null
                && i.getPropietario().getEmail().endsWith(DOMINIO)).toList();
        var ids = demoInmuebles.stream().map(Inmueble::getId).toList();

        movimientos.deleteAll(movimientos.findAll().stream().filter(m -> m.getConcepto().startsWith(MARCA)
                || (m.getCuentaCobro() != null && ids.contains(m.getCuentaCobro().getInmueble().getId()))
                || (m.getComprobanteEgreso() != null && ids.contains(m.getComprobanteEgreso().getInmueble().getId()))).toList());
        egresos.deleteAll(egresos.findAll().stream().filter(e -> ids.contains(e.getInmueble().getId())).toList());
        cuentas.deleteAll(cuentas.findAll().stream().filter(c -> ids.contains(c.getInmueble().getId())).toList());
        inmuebles.deleteAll(demoInmuebles);
        inquilinos.deleteAll(inquilinos.findAll().stream().filter(i -> i.getEmail() != null && i.getEmail().endsWith(DOMINIO)).toList());
        propietarios.deleteAll(propietarios.findAll().stream().filter(p -> p.getEmail() != null && p.getEmail().endsWith(DOMINIO)).toList());
        log.info("Datos demo eliminados.");
    }

    private void marcarEnviada(Long id) {
        var c = cuentaService.obtener(id);
        c.setEstado(EstadoDocumento.ENVIADO);
        c.setEnviadoEn(LocalDateTime.now());
        cuentas.save(c);
    }

    private void marcarEgresoEnviado(Long id) {
        var e = egresoService.obtener(id);
        e.setEstado(EstadoDocumento.ENVIADO);
        e.setEnviadoEn(LocalDateTime.now());
        egresos.save(e);
    }

    private Propietario propietario(String nombre, String doc, String usuario, String banco, String tipo, String cuenta) {
        var p = new Propietario();
        p.setNombre(nombre);
        p.setDocumento(doc);
        p.setEmail(usuario + DOMINIO);
        p.setTelefono("300" + doc.substring(doc.length() - 7));
        p.setBanco(banco);
        p.setTipoCuenta(tipo);
        p.setNumeroCuenta(cuenta);
        return propietarios.save(p);
    }

    private Inquilino inquilino(String nombre, String doc, String usuario, String direccion) {
        var i = new Inquilino();
        i.setNombre(nombre);
        i.setDocumento(doc);
        i.setEmail(usuario + DOMINIO);
        i.setTelefono("310" + doc.substring(doc.length() - 7));
        i.setCiudad("La Ceja - Antioquia");
        i.setDireccion(direccion);
        return inquilinos.save(i);
    }

    private Inmueble inmueble(String descripcion, String direccion, Propietario p, Inquilino i, String canon, String pctCobro, String pctEgreso) {
        var m = new Inmueble();
        m.setDescripcion(descripcion);
        m.setDireccion(direccion);
        m.setCiudad("La Ceja - Antioquia");
        m.setPropietario(p);
        m.setInquilino(i);
        m.setCanon(new BigDecimal(canon));
        m.setPctAdminCobro(new BigDecimal(pctCobro));
        m.setPctAdminEgreso(new BigDecimal(pctEgreso));
        return inmuebles.save(m);
    }
}
