package co.inmobiliaria360.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class NumeroALetrasTest {

    private static String p(String v) {
        return NumeroALetras.pesos(new BigDecimal(v));
    }

    @Test
    void ejemploDelExcel() {
        assertEquals("UN MILLON SETECIENTOS CINCUENTA MIL PESOS", p("1750000"));
    }

    @Test
    void casosBorde() {
        assertEquals("CERO PESOS", p("0"));
        assertEquals("UN PESO", p("1"));
        assertEquals("CIEN PESOS", p("100"));
        assertEquals("CIENTO UN PESOS", p("101"));
        assertEquals("VEINTIUN MIL PESOS", p("21000"));
        assertEquals("MIL PESOS", p("1000"));
        assertEquals("UN MILLON DE PESOS", p("1000000"));
        assertEquals("DOS MILLONES DE PESOS", p("2000000"));
        assertEquals("TRES MILLONES QUINIENTOS MIL PESOS", p("3500000"));
        assertEquals("DOCE MILLONES TRESCIENTOS CUARENTA Y CINCO MIL SEISCIENTOS SETENTA Y OCHO PESOS", p("12345678"));
    }

    @Test
    void redondeaCentavos() {
        assertEquals("MIL PESOS", p("999.60"));
    }

    @Test
    void rechazaNegativos() {
        assertThrows(IllegalArgumentException.class, () -> p("-5"));
    }
}
