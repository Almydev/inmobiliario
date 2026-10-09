package co.inmobiliaria360.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Convierte valores en pesos a letras en mayúsculas ("UN MILLON SETECIENTOS CINCUENTA MIL PESOS"). */
public final class NumeroALetras {

    private static final String[] UNIDADES = {"", "UN", "DOS", "TRES", "CUATRO", "CINCO", "SEIS", "SIETE", "OCHO", "NUEVE",
            "DIEZ", "ONCE", "DOCE", "TRECE", "CATORCE", "QUINCE", "DIECISEIS", "DIECISIETE", "DIECIOCHO", "DIECINUEVE",
            "VEINTE", "VEINTIUN", "VEINTIDOS", "VEINTITRES", "VEINTICUATRO", "VEINTICINCO", "VEINTISEIS", "VEINTISIETE",
            "VEINTIOCHO", "VEINTINUEVE"};
    private static final String[] DECENAS = {"", "", "", "TREINTA", "CUARENTA", "CINCUENTA", "SESENTA", "SETENTA", "OCHENTA", "NOVENTA"};
    private static final String[] CENTENAS = {"", "CIENTO", "DOSCIENTOS", "TRESCIENTOS", "CUATROCIENTOS", "QUINIENTOS",
            "SEISCIENTOS", "SETECIENTOS", "OCHOCIENTOS", "NOVECIENTOS"};

    private NumeroALetras() {}

    public static String pesos(BigDecimal valor) {
        long n = valor.setScale(0, RoundingMode.HALF_UP).longValueExact();
        if (n < 0) throw new IllegalArgumentException("El valor no puede ser negativo");
        if (n == 0) return "CERO PESOS";
        String letras = convertir(n);
        // "UN MILLON DE PESOS", "DOS MILLONES DE PESOS" (los millones exactos llevan "DE")
        boolean exactoMillon = n % 1_000_000 == 0;
        if (n == 1) return "UN PESO";
        return letras + (exactoMillon ? " DE PESOS" : " PESOS");
    }

    private static String convertir(long n) {
        if (n >= 1_000_000_000_000L) throw new IllegalArgumentException("Valor demasiado grande");
        if (n >= 1_000_000) {
            long millones = n / 1_000_000;
            long resto = n % 1_000_000;
            String m = millones == 1 ? "UN MILLON" : convertir(millones) + " MILLONES";
            return resto == 0 ? m : m + " " + convertir(resto);
        }
        if (n >= 1000) {
            long miles = n / 1000;
            long resto = n % 1000;
            String m = miles == 1 ? "MIL" : convertir(miles) + " MIL";
            return resto == 0 ? m : m + " " + convertir(resto);
        }
        return centenas((int) n);
    }

    private static String centenas(int n) {
        if (n == 100) return "CIEN";
        StringBuilder sb = new StringBuilder();
        if (n >= 100) sb.append(CENTENAS[n / 100]).append(' ');
        int r = n % 100;
        if (r > 0) {
            if (r < 30) sb.append(UNIDADES[r]);
            else {
                sb.append(DECENAS[r / 10]);
                if (r % 10 > 0) sb.append(" Y ").append(UNIDADES[r % 10]);
            }
        }
        return sb.toString().trim();
    }
}
