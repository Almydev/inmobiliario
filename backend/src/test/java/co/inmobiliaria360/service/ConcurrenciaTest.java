package co.inmobiliaria360.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.server.ResponseStatusException;

/**
 * Simula multiples clics (peticiones simultaneas) sobre el mismo documento contra una base H2 en memoria.
 * Nunca toca Neon: se anula la carga del .env y se fuerza la conexion de pruebas.
 */
@SpringBootTest(properties = {
        "spring.config.import=optional:file:./no-existe.properties",
        "spring.datasource.url=jdbc:h2:mem:concurrencia;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.defer-datasource-initialization=true",
        "spring.sql.init.mode=always",
        "app.jwt.secret=0123456789abcdef0123456789abcdef",
        "spring.datasource.hikari.maximum-pool-size=30"
})
class ConcurrenciaTest {

    private static final int HILOS = 20;

    @Autowired CuentaCobroService cuentaService;
    @Autowired ComprobanteEgresoService egresoService;
    @Autowired CuentaCobroRepository cuentas;
    @Autowired ComprobanteEgresoRepository egresos;
    @Autowired MovimientoBancoRepository movimientos;
    @Autowired InmuebleRepository inmuebles;
    @Autowired PropietarioRepository propietarios;
    @Autowired InquilinoRepository inquilinos;
    @MockitoBean MailService mail;

    private Long inmuebleId;
    private int n;

    @BeforeEach
    void datos() {
        movimientos.deleteAll();
        egresos.deleteAll();
        cuentas.deleteAll();
        inmuebles.deleteAll();
        inquilinos.deleteAll();
        propietarios.deleteAll();

        n++;
        var p = new Propietario();
        p.setNombre("Propietario");
        p.setDocumento("P" + n);
        p.setEmail("p@ejemplo.test");
        p = propietarios.save(p);
        var i = new Inquilino();
        i.setNombre("Inquilino");
        i.setDocumento("I" + n);
        i.setEmail("i@ejemplo.test");
        i = inquilinos.save(i);
        var m = new Inmueble();
        m.setDescripcion("Apto");
        m.setDireccion("Calle 1");
        m.setPropietario(p);
        m.setInquilino(i);
        m.setCanon(new BigDecimal("1000000"));
        inmuebleId = inmuebles.save(m).getId();
    }

    /** Lanza HILOS tareas a la vez; devuelve cuantas terminaron bien y cuantas con ResponseStatusException(esperado). */
    private int[] simultaneo(java.util.concurrent.Callable<Object> tarea) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(HILOS);
        CountDownLatch salida = new CountDownLatch(1);
        List<Future<Integer>> futuros = new ArrayList<>();
        for (int k = 0; k < HILOS; k++) {
            futuros.add(pool.submit(() -> {
                salida.await();
                try {
                    tarea.call();
                    return 0;
                } catch (ResponseStatusException e) {
                    return 1;
                } catch (Exception e) {
                    return 2; // cualquier otro error (500) es un fallo
                }
            }));
        }
        salida.countDown();
        int ok = 0, conflicto = 0, inesperado = 0;
        for (Future<Integer> f : futuros) {
            switch (f.get()) {
                case 0 -> ok++;
                case 1 -> conflicto++;
                default -> inesperado++;
            }
        }
        pool.shutdown();
        return new int[] {ok, conflicto, inesperado};
    }

    @Test
    void pagarLaMismaCuentaVariasVecesRegistraUnSoloIngreso() throws Exception {
        Long id = cuentaService.generar(inmuebleId, "2026-10", false, null, null).getId();

        int[] r = simultaneo(() -> cuentaService.pagar(id));

        assertEquals(1, r[0], "solo un pago debe prosperar");
        assertEquals(HILOS - 1, r[1], "el resto debe recibir 409");
        assertEquals(0, r[2], "no debe haber errores inesperados");
        assertEquals(1, movimientos.count(), "un solo movimiento de banco");
    }

    @Test
    void pagarElMismoEgresoVariasVecesRegistraUnSoloGasto() throws Exception {
        Long id = egresoService.generar(inmuebleId, "2026-10", 30, null, null, null).getId();

        int[] r = simultaneo(() -> egresoService.pagar(id));

        assertEquals(1, r[0]);
        assertEquals(HILOS - 1, r[1]);
        assertEquals(0, r[2]);
        assertEquals(1, movimientos.count());
    }

    @Test
    void generarLaMismaCuentaVariasVecesCreaUnaSola() throws Exception {
        int[] r = simultaneo(() -> cuentaService.generar(inmuebleId, "2026-10", false, null, null));

        assertEquals(1, r[0], "solo una cuenta debe crearse");
        assertEquals(1, cuentas.count());
        assertEquals(0, r[2], "los duplicados deben salir como 409, no como error interno");
    }

    @Test
    void generarElMismoEgresoVariasVecesCreaUnoSolo() throws Exception {
        int[] r = simultaneo(() -> egresoService.generar(inmuebleId, "2026-10", 30, null, null, null));

        assertEquals(1, r[0]);
        assertEquals(1, egresos.count());
        assertEquals(0, r[2]);
    }

    @Test
    void enviarLaMismaCuentaVariasVecesEnviaUnSoloCorreo() throws Exception {
        Long id = cuentaService.generar(inmuebleId, "2026-10", false, null, null).getId();
        AtomicInteger enviados = new AtomicInteger();
        doAnswer(inv -> {
            enviados.incrementAndGet();
            Thread.sleep(400); // un SMTP lento mantiene el envio en curso mientras llegan los otros clics
            return null;
        }).when(mail).enviarConAdjunto(anyString(), anyString(), anyString(), anyString(), any());

        int[] r = simultaneo(() -> cuentaService.enviar(id));

        assertEquals(1, r[0], "solo un envio debe prosperar");
        assertEquals(0, r[2]);
        assertEquals(1, enviados.get(), "un solo correo");
        verify(mail, times(1)).enviarConAdjunto(anyString(), anyString(), anyString(), anyString(), any());
    }
}
