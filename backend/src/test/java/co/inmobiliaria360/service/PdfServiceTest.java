package co.inmobiliaria360.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;

class PdfServiceTest {

    private final PdfService pdf = new PdfService(Fixtures.empresa());

    private String texto(byte[] bytes) throws Exception {
        PdfReader reader = new PdfReader(bytes);
        try {
            return new PdfTextExtractor(reader).getTextFromPage(1);
        } finally {
            reader.close();
        }
    }

    @Test
    void generaUnPdfValidoConLosDatosDeLaCuenta() throws Exception {
        byte[] bytes = pdf.cuentaCobro(Fixtures.cuentaCobro(7, "ESTEBAN RINCON OSORIO"));

        assertEquals("%PDF", new String(bytes, 0, 4, StandardCharsets.US_ASCII));
        String t = texto(bytes);
        assertTrue(t.contains("ESTEBAN RINCON OSORIO"), t);
        assertTrue(t.contains("Nro. 7"), t);
        assertTrue(t.contains("UN MILLON SETECIENTOS CINCUENTA MIL PESOS"), t);
        assertTrue(t.contains("1.750.000"), t);
        assertTrue(t.contains("Beneficiario Prueba"), t);
    }

    @Test
    void textoConCaracteresEspecialesNoRompeElPdf() throws Exception {
        byte[] bytes = pdf.cuentaCobro(Fixtures.cuentaCobro(8, "MUÑOZ & CÍA <script>alert(1)</script> \"Ñandú\""));
        assertTrue(texto(bytes).contains("MUÑOZ"));
    }

    @Test
    void rendimiento_300PdfEnParaleloEnTiempoRazonable() throws Exception {
        int total = 300;
        var pool = Executors.newFixedThreadPool(8);
        long inicio = System.nanoTime();
        List<Future<Integer>> futuros = new ArrayList<>();
        for (int i = 1; i <= total; i++) {
            final int n = i;
            Callable<Integer> tarea = () -> pdf.cuentaCobro(Fixtures.cuentaCobro(n, "INQUILINO " + n)).length;
            futuros.add(pool.submit(tarea));
        }
        for (Future<Integer> f : futuros) assertTrue(f.get() > 1000);
        pool.shutdown();
        long ms = (System.nanoTime() - inicio) / 1_000_000;
        System.out.printf("PDF: %d documentos en %d ms (%.1f ms/doc)%n", total, ms, (double) ms / total);

        // Cota holgada para no fallar en maquinas lentas; el objetivo es detectar regresiones graves.
        assertTrue(ms < 30_000, "Generar " + total + " PDF tardó " + ms + " ms");
    }
}
