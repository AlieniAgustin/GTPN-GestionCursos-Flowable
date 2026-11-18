package com.backend.proxy.service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Component;

import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

/**
 * Arma el PDF de una versión de la propuesta (enunciado 4.1.3) con OpenPDF.
 *
 * <p>Es solo presentación: recibe los datos ya validados y devuelve los bytes del
 * archivo. No guarda nada.
 */
@Component
public class ProposalPdfBuilder {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Font TITULO = new Font(Font.HELVETICA, 18, Font.BOLD);
    private static final Font SUBTITULO = new Font(Font.HELVETICA, 11, Font.NORMAL);
    private static final Font ETIQUETA = new Font(Font.HELVETICA, 10, Font.BOLD);
    private static final Font TEXTO = new Font(Font.HELVETICA, 10, Font.NORMAL);

    /** Datos que van en el documento. */
    public record Datos(
            String empresa,
            String curso,
            String descripcion,
            String programa,
            BigDecimal presupuesto,
            List<LocalDate> fechas,
            int version,
            String elaboradaPor,
            LocalDate emitida
    ) {}

    public byte[] build(Datos datos) {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        Document documento = new Document(PageSize.A4, 56, 56, 56, 56);
        PdfWriter.getInstance(documento, salida);
        documento.open();

        documento.add(new Paragraph("Instituto de Capacitación", SUBTITULO));
        documento.add(new Paragraph("Propuesta de capacitación", TITULO));
        documento.add(new Paragraph(
                "Versión " + datos.version() + " · emitida el " + datos.emitida().format(FECHA),
                SUBTITULO));
        documento.add(new Paragraph(" "));

        PdfPTable tabla = new PdfPTable(new float[] {1.2f, 3f});
        tabla.setWidthPercentage(100);
        fila(tabla, "Empresa", datos.empresa());
        fila(tabla, "Curso", datos.curso());
        fila(tabla, "Descripción", datos.descripcion());
        fila(tabla, "Programa", datos.programa());
        fila(tabla, "Presupuesto", pesos(datos.presupuesto()));
        fila(tabla, "Fechas posibles", fechas(datos.fechas()));
        documento.add(tabla);

        documento.add(new Paragraph(" "));
        documento.add(new Paragraph(
                "Elaborada por " + datos.elaboradaPor() + ". La empresa puede aceptar la propuesta, "
                        + "rechazarla o pedir modificaciones.", TEXTO));

        documento.close();
        return salida.toByteArray();
    }

    private void fila(PdfPTable tabla, String etiqueta, String valor) {
        PdfPCell izquierda = new PdfPCell(new Phrase(etiqueta, ETIQUETA));
        izquierda.setPadding(6);
        PdfPCell derecha = new PdfPCell(new Phrase(valor == null || valor.isBlank() ? "—" : valor, TEXTO));
        derecha.setPadding(6);
        tabla.addCell(izquierda);
        tabla.addCell(derecha);
    }

    private String pesos(BigDecimal monto) {
        return NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-AR")).format(monto);
    }

    private String fechas(List<LocalDate> fechas) {
        return String.join(", ", fechas.stream().map(fecha -> fecha.format(FECHA)).toList());
    }
}
