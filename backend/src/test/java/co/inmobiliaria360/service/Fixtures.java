package co.inmobiliaria360.service;

import co.inmobiliaria360.config.EmpresaProps;
import co.inmobiliaria360.domain.CuentaCobro;
import co.inmobiliaria360.domain.Inmueble;
import co.inmobiliaria360.domain.Inquilino;
import co.inmobiliaria360.domain.Propietario;
import java.math.BigDecimal;
import java.time.LocalDate;

final class Fixtures {
    private Fixtures() {}

    static EmpresaProps empresa() {
        return new EmpresaProps("Soluciones Inmobiliarias 360", "Calle de prueba 1-23", "900000000",
                "contacto@prueba.test", "Beneficiario Prueba", "100200300", "Cuenta de ahorros Bancolombia", "00011122233");
    }

    static CuentaCobro cuentaCobro(long consecutivo, String nombreInquilino) {
        var propietario = new Propietario();
        propietario.setId(1L);
        propietario.setNombre("Luz Propietaria");

        var inquilino = new Inquilino();
        inquilino.setId(2L);
        inquilino.setNombre(nombreInquilino);
        inquilino.setDocumento("1077463346");
        inquilino.setEmail("inquilino@prueba.test");
        inquilino.setCiudad("La Ceja - Antioquia");
        inquilino.setDireccion("Calle 17 # 21 45");

        var inmueble = new Inmueble();
        inmueble.setId(3L);
        inmueble.setDescripcion("Apartamento 402");
        inmueble.setDireccion("Carrera 10 # 5-20");

        var c = new CuentaCobro();
        c.setId(consecutivo);
        c.setConsecutivo(consecutivo);
        c.setFecha(LocalDate.of(2026, 10, 9));
        c.setPeriodo("2026-10");
        c.setInmueble(inmueble);
        c.setInquilino(inquilino);
        c.setPropietario(propietario);
        c.setConcepto("PAGO DE ARRENDAMIENTO DEL MES DE OCTUBRE DE 2026");
        c.setValorArriendo(new BigDecimal("1750000"));
        c.setTotal(new BigDecimal("1750000"));
        return c;
    }
}
