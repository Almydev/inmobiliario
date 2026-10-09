package co.inmobiliaria360.service;

import co.inmobiliaria360.config.EmpresaProps;
import co.inmobiliaria360.domain.CuentaCobro;
import co.inmobiliaria360.util.NumeroALetras;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.springframework.stereotype.Service;

/** Genera los PDF con el mismo formato de las hojas del Excel de la inmobiliaria. */
@Service
public class PdfService {

    private static final Color PETROLEO = new Color(0x1F, 0x3D, 0x4A);
    private static final Color ARENA = new Color(0xF4, 0xED, 0xE4);
    private static final Font TITULO = new Font(Font.HELVETICA, 18, Font.BOLD, PETROLEO);
    private static final Font NEGRITA = new Font(Font.HELVETICA, 10, Font.BOLD);
    private static final Font NORMAL = new Font(Font.HELVETICA, 10);
    private static final Font PEQUENA = new Font(Font.HELVETICA, 8, Font.NORMAL, Color.DARK_GRAY);

    private final EmpresaProps empresa;
    private final NumberFormat pesos = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-CO"));

    public PdfService(EmpresaProps empresa) {
        this.empresa = empresa;
        pesos.setMaximumFractionDigits(0);
    }

    public byte[] cuentaCobro(CuentaCobro c) {
        try (var out = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4, 42, 42, 36, 36);
            PdfWriter.getInstance(doc, out);
            doc.open();

            encabezado(doc, "REMISIÓN", c.getConsecutivo(), c.getFecha().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            doc.add(espacio(10));

            PdfPTable cliente = tabla();
            fila(cliente, "Señores:", c.getInquilino().getNombre());
            fila(cliente, "C.C.:", c.getInquilino().getDocumento());
            fila(cliente, "Teléfono:", c.getInquilino().getTelefono());
            fila(cliente, "Ciudad:", c.getInquilino().getCiudad());
            fila(cliente, "Dirección:", c.getInquilino().getDireccion());
            fila(cliente, "Inmueble:", c.getInmueble().getDescripcion() + " - " + c.getInmueble().getDireccion());
            doc.add(cliente);
            doc.add(espacio(14));

            PdfPTable valor = tabla();
            fila(valor, "VALOR A PAGAR:", NumeroALetras.pesos(c.getTotal()));
            doc.add(valor);
            doc.add(espacio(14));

            PdfPTable detalle = new PdfPTable(new float[] {4, 1.4f});
            detalle.setWidthPercentage(100);
            encabezadoTabla(detalle, "CONCEPTO");
            encabezadoTabla(detalle, "VALOR");
            linea(detalle, c.getConcepto(), c.getValorArriendo(), false);
            if (c.getValorAdministracion().signum() != 0) linea(detalle, "Administración", c.getValorAdministracion(), false);
            if (c.getOtros().signum() != 0) linea(detalle, "Otros", c.getOtros(), false);
            if (c.getReteFuente().signum() != 0) linea(detalle, "Rte. fuente", c.getReteFuente().negate(), false);
            linea(detalle, "TOTAL", c.getTotal(), true);
            doc.add(detalle);
            doc.add(espacio(18));

            doc.add(new Paragraph("Favor pagar a: " + n(empresa.beneficiario()), NEGRITA));
            doc.add(new Paragraph("C.C. " + n(empresa.beneficiarioDocumento()), NORMAL));
            doc.add(new Paragraph(n(empresa.cuentaTipo()) + ":", NORMAL));
            doc.add(new Paragraph("# " + n(empresa.cuentaNumero()), NEGRITA));
            doc.add(espacio(20));
            Paragraph pie = new Paragraph("Documento generado por " + n(empresa.nombre()) + ".", PEQUENA);
            doc.add(pie);

            doc.close();
            return out.toByteArray();
        } catch (DocumentException | IOException e) {
            throw new IllegalStateException("No se pudo generar el PDF", e);
        }
    }

    // ---- utilidades de maquetación ----

    private void encabezado(Document doc, String titulo, Long consecutivo, String fecha) throws DocumentException, IOException {
        PdfPTable t = new PdfPTable(new float[] {1.3f, 2});
        t.setWidthPercentage(100);

        PdfPCell logo = new PdfPCell();
        logo.setBorder(Rectangle.NO_BORDER);
        try (InputStream in = getClass().getResourceAsStream("/pdf/logo.jpg")) {
            if (in != null) {
                Image img = Image.getInstance(in.readAllBytes());
                img.scaleToFit(150, 80);
                logo.addElement(img);
            }
        }
        t.addCell(logo);

        PdfPCell datos = new PdfPCell();
        datos.setBorder(Rectangle.NO_BORDER);
        datos.setHorizontalAlignment(Element.ALIGN_RIGHT);
        Paragraph p = new Paragraph(titulo, TITULO);
        p.setAlignment(Element.ALIGN_RIGHT);
        datos.addElement(p);
        datos.addElement(derecha("Nro. " + consecutivo, NEGRITA));
        datos.addElement(derecha("Fecha: " + fecha, NORMAL));
        datos.addElement(derecha(n(empresa.direccion()), NORMAL));
        datos.addElement(derecha("Nit. " + n(empresa.nit()), NORMAL));
        datos.addElement(derecha("Email: " + n(empresa.email()), NORMAL));
        t.addCell(datos);
        doc.add(t);
    }

    private static Paragraph derecha(String texto, Font f) {
        Paragraph p = new Paragraph(texto, f);
        p.setAlignment(Element.ALIGN_RIGHT);
        return p;
    }

    private static Paragraph espacio(float alto) {
        Paragraph p = new Paragraph(" ");
        p.setLeading(alto);
        return p;
    }

    private static PdfPTable tabla() {
        PdfPTable t = new PdfPTable(new float[] {1.2f, 4});
        t.setWidthPercentage(100);
        return t;
    }

    private static void fila(PdfPTable t, String etiqueta, String valor) {
        PdfPCell a = new PdfPCell(new Phrase(etiqueta, NEGRITA));
        a.setBorder(Rectangle.NO_BORDER);
        a.setPaddingBottom(3);
        PdfPCell b = new PdfPCell(new Phrase(n(valor), NORMAL));
        b.setBorder(Rectangle.NO_BORDER);
        b.setPaddingBottom(3);
        t.addCell(a);
        t.addCell(b);
    }

    private static void encabezadoTabla(PdfPTable t, String texto) {
        PdfPCell c = new PdfPCell(new Phrase(texto, new Font(Font.HELVETICA, 10, Font.BOLD, Color.WHITE)));
        c.setBackgroundColor(PETROLEO);
        c.setPadding(6);
        c.setBorderColor(PETROLEO);
        t.addCell(c);
    }

    private void linea(PdfPTable t, String concepto, BigDecimal valor, boolean total) {
        Font f = total ? NEGRITA : NORMAL;
        PdfPCell a = new PdfPCell(new Phrase(concepto, f));
        PdfPCell b = new PdfPCell(new Phrase(pesos.format(valor), f));
        b.setHorizontalAlignment(Element.ALIGN_RIGHT);
        for (PdfPCell c : new PdfPCell[] {a, b}) {
            c.setPadding(6);
            c.setBorderColor(Color.LIGHT_GRAY);
            if (total) c.setBackgroundColor(ARENA);
            t.addCell(c);
        }
    }

    private static String n(String s) {
        return s == null || s.isBlank() ? "" : s;
    }
}
