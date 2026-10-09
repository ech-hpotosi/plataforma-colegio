package co.edu.elencano.plataforma.notas.servicio;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.lowagie.text.Document;
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

import co.edu.elencano.plataforma.notas.entidad.Desempeno;

/**
 * Dibuja el boletin en PDF, un estudiante por pagina, con los elementos del SIEE art. 14. Tamano carta para
 * imprimir en el colegio. Los datos llegan calculados en {@link DatosBoletin}.
 */
@Component
public class BoletinPdf {

    private static final String INSTITUCION = "Institución Educativa Municipal El Encano";
    private static final String LEGAL = "Decreto 0338 de agosto 26 de 2003 de la Alcaldía Municipal de Pasto. "
            + "NIT 800 060022-0. DANE 452001002528. Corregimiento de El Encano, Municipio de Pasto.";
    private static final Map<Desempeno, String> NOMBRE = Map.of(Desempeno.BAJO, "Bajo", Desempeno.BASICO, "Básico",
            Desempeno.ALTO, "Alto", Desempeno.SUPERIOR, "Superior");
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy",
            Locale.forLanguageTag("es-CO"));

    // Colores del tema del frontend (tema.ts)
    private static final Color LAGUNA = new Color(0x1b, 0x4a, 0x72);
    private static final Color LINEA = new Color(0xe2, 0xdd, 0xd2);
    private static final Color ENCABEZADO = new Color(0xf4, 0xf2, 0xec);
    private static final Color TINTA_SUAVE = new Color(0x5b, 0x66, 0x70);

    private static final Font TITULO = new Font(Font.HELVETICA, 13, Font.BOLD, LAGUNA);
    private static final Font SUBTITULO = new Font(Font.HELVETICA, 10, Font.BOLD);
    private static final Font NORMAL = new Font(Font.HELVETICA, 8.5f);
    private static final Font NEGRITA = new Font(Font.HELVETICA, 8.5f, Font.BOLD);
    private static final Font PEQUENA = new Font(Font.HELVETICA, 7, Font.NORMAL, TINTA_SUAVE);
    private static final Font ETIQUETA = new Font(Font.HELVETICA, 6.5f, Font.BOLD, TINTA_SUAVE);
    private static final Font COLUMNA = new Font(Font.HELVETICA, 7, Font.BOLD, LAGUNA);

    private final byte[] escudo;

    public BoletinPdf() {
        try (InputStream entrada = getClass().getResourceAsStream("/boletin/escudo.png")) {
            escudo = entrada == null ? null : entrada.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public byte[] generar(DatosBoletin datos) {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        Document documento = new Document(PageSize.LETTER, 36, 36, 32, 32);
        PdfWriter.getInstance(documento, salida);
        documento.addTitle("Boletín " + datos.grupo() + " periodo " + datos.periodo() + " de " + datos.anio());
        documento.open();
        boolean primero = true;
        for (DatosBoletin.Estudiante e : datos.estudiantes()) {
            if (!primero) {
                documento.newPage();
            }
            primero = false;
            pagina(documento, datos, e);
        }
        documento.close();
        return salida.toByteArray();
    }

    private void pagina(Document documento, DatosBoletin datos, DatosBoletin.Estudiante e) {
        documento.add(encabezado(datos));
        documento.add(identificacion(datos, e));
        documento.add(asignaturas(e));
        documento.add(comportamiento(e));
        documento.add(escala(datos.escala()));
        documento.add(firmas(datos));
        Paragraph legal = new Paragraph(LEGAL, PEQUENA);
        legal.setAlignment(Element.ALIGN_CENTER);
        legal.setSpacingBefore(10);
        documento.add(legal);
    }

    private PdfPTable encabezado(DatosBoletin datos) {
        PdfPTable tabla = new PdfPTable(new float[] {1.2f, 6, 2.2f});
        tabla.setWidthPercentage(100);
        PdfPCell logo = sinBorde(new PdfPCell());
        if (escudo != null) {
            try {
                Image imagen = Image.getInstance(escudo);
                imagen.scaleToFit(52, 52);
                logo = sinBorde(new PdfPCell(imagen));
            } catch (IOException ex) {
                throw new UncheckedIOException(ex);
            }
        }
        logo.setVerticalAlignment(Element.ALIGN_MIDDLE);
        tabla.addCell(logo);

        PdfPCell centro = sinBorde(new PdfPCell());
        centro.setVerticalAlignment(Element.ALIGN_MIDDLE);
        centro.addElement(new Paragraph(INSTITUCION, TITULO));
        centro.addElement(new Paragraph("Boletín de aprendizaje y convivencia", SUBTITULO));
        centro.addElement(new Paragraph("Estudio, trabajo y ciencia", PEQUENA));
        tabla.addCell(centro);

        PdfPCell derecha = sinBorde(new PdfPCell());
        derecha.setVerticalAlignment(Element.ALIGN_MIDDLE);
        Paragraph periodo = new Paragraph("Periodo " + datos.periodo(), TITULO);
        periodo.setAlignment(Element.ALIGN_RIGHT);
        Paragraph anio = new Paragraph("Año lectivo " + datos.anio(), NORMAL);
        anio.setAlignment(Element.ALIGN_RIGHT);
        derecha.addElement(periodo);
        derecha.addElement(anio);
        tabla.addCell(derecha);
        tabla.setSpacingAfter(8);
        return tabla;
    }

    private PdfPTable identificacion(DatosBoletin datos, DatosBoletin.Estudiante e) {
        PdfPTable tabla = new PdfPTable(new float[] {4, 1.6f, 2.2f, 2.2f});
        tabla.setWidthPercentage(100);
        tabla.addCell(dato("Estudiante", e.nombre()));
        tabla.addCell(dato("Código", e.codigo() == null ? "-" : e.codigo()));
        tabla.addCell(dato("Documento", e.documento() == null ? "-" : e.documento()));
        tabla.addCell(dato("Fecha", FECHA.format(datos.fecha())));
        tabla.addCell(dato("Sede", datos.sede()));
        tabla.addCell(dato("Grado y grupo", datos.grupo()));
        tabla.addCell(dato("Jornada", datos.jornada()));
        tabla.addCell(dato("Director de grado", datos.director() == null ? "-" : datos.director()));
        tabla.setSpacingAfter(10);
        return tabla;
    }

    private PdfPTable asignaturas(DatosBoletin.Estudiante e) {
        PdfPTable tabla = new PdfPTable(new float[] {5, 0.8f, 1, 1.5f, 1.1f, 1.1f});
        tabla.setWidthPercentage(100);
        tabla.setHeaderRows(1);
        for (String titulo : new String[] {"Área y asignatura", "IH", "Nota", "Desempeño", "Faltas just.",
                "Faltas sin just."}) {
            PdfPCell celda = new PdfPCell(new Phrase(titulo, COLUMNA));
            celda.setBackgroundColor(ENCABEZADO);
            celda.setBorderColor(LINEA);
            celda.setPadding(4);
            celda.setHorizontalAlignment(titulo.startsWith("Área") ? Element.ALIGN_LEFT : Element.ALIGN_CENTER);
            tabla.addCell(celda);
        }
        boolean hayParciales = false;
        for (DatosBoletin.Asignatura a : e.asignaturas()) {
            PdfPCell nombre = celda(Element.ALIGN_LEFT);
            nombre.addElement(new Phrase(a.area(), PEQUENA));
            nombre.addElement(new Phrase(a.nombre(), NEGRITA));
            if (a.docente() != null) {
                nombre.addElement(new Phrase("Docente: " + a.docente(), PEQUENA));
            }
            nombre.setBorderWidthBottom(0);
            tabla.addCell(nombre);
            tabla.addCell(texto(a.intensidad() == null ? "-" : String.valueOf(a.intensidad()), NORMAL));
            hayParciales |= a.nota() != null && !a.completa();
            tabla.addCell(texto(a.nota() == null ? "-" : a.nota().toPlainString() + (a.completa() ? "" : "*"), NEGRITA));
            tabla.addCell(texto(a.desempeno() == null ? "-" : NOMBRE.get(a.desempeno()), NORMAL));
            tabla.addCell(texto(String.valueOf(a.faltasJustificadas()), NORMAL));
            tabla.addCell(texto(String.valueOf(a.faltasSinJustificar()), NORMAL));

            PdfPCell detalle = celda(Element.ALIGN_LEFT);
            detalle.setColspan(6);
            detalle.setBorderWidthTop(0);
            detalle.setPaddingTop(0);
            Paragraph concepto = new Paragraph();
            concepto.add(new Phrase("Concepto: ", NEGRITA));
            concepto.add(new Phrase(a.concepto() == null ? "Sin concepto registrado." : a.concepto(), NORMAL));
            detalle.addElement(concepto);
            if (a.observacion() != null) {
                Paragraph observacion = new Paragraph();
                observacion.add(new Phrase("Observación: ", NEGRITA));
                observacion.add(new Phrase(a.observacion(), NORMAL));
                detalle.addElement(observacion);
            }
            tabla.addCell(detalle);
        }
        if (hayParciales) {
            PdfPCell nota = sinBorde(new PdfPCell(new Phrase(
                    "* Nota parcial: la asignatura todavía no tiene notas en todas las dimensiones (Saber, Hacer y Ser).",
                    PEQUENA)));
            nota.setColspan(6);
            nota.setPaddingTop(3);
            tabla.addCell(nota);
        }
        tabla.setSpacingAfter(10);
        return tabla;
    }

    private PdfPTable comportamiento(DatosBoletin.Estudiante e) {
        PdfPTable tabla = new PdfPTable(1);
        tabla.setWidthPercentage(100);
        Paragraph texto = new Paragraph();
        texto.add(new Phrase("Valoración del comportamiento: ", NEGRITA));
        if (e.comportamiento() == null) {
            texto.add(new Phrase("sin valoraciones registradas en el periodo.", NORMAL));
        } else {
            int docentes = e.valoracionesComportamiento();
            texto.add(new Phrase(e.comportamiento().toPlainString() + " (" + NOMBRE.get(e.desempenoComportamiento())
                    + "), " + (docentes == 1 ? "valoración de 1 docente." : "promedio de las valoraciones de "
                    + docentes + " docentes."), NORMAL));
        }
        PdfPCell celda = new PdfPCell();
        celda.addElement(texto);
        celda.setBorderColor(LINEA);
        celda.setPadding(6);
        celda.setPaddingTop(1);
        tabla.addCell(celda);
        tabla.setSpacingAfter(6);
        return tabla;
    }

    private Paragraph escala(DatosBoletin.Escala escala) {
        BigDecimal decima = new BigDecimal("0.1");
        String texto = "Escala de valoración (Decreto 1290 y SIEE art. 7): Superior de " + escala.limiteSuperior()
                + " a " + escala.maxima() + "; Alto de " + escala.limiteAlto() + " a "
                + escala.limiteSuperior().subtract(decima) + "; Básico de " + escala.aprobatoria() + " a "
                + escala.limiteAlto().subtract(decima) + "; Bajo de " + escala.minima() + " a "
                + escala.aprobatoria().subtract(decima) + ". IH: intensidad horaria semanal. Las faltas se cuentan en horas.";
        return new Paragraph(texto, PEQUENA);
    }

    private PdfPTable firmas(DatosBoletin datos) {
        PdfPTable tabla = new PdfPTable(new float[] {1, 0.3f, 1});
        tabla.setWidthPercentage(80);
        tabla.setSpacingBefore(42);
        tabla.addCell(firma(datos.director(), "Director(a) de grado"));
        tabla.addCell(sinBorde(new PdfPCell()));
        tabla.addCell(firma(datos.coordinador(), "Coordinación académica"));
        return tabla;
    }

    private PdfPCell firma(String nombre, String cargo) {
        PdfPCell celda = sinBorde(new PdfPCell());
        celda.setBorderWidthTop(0.6f);
        celda.setBorderColorTop(TINTA_SUAVE);
        Paragraph quien = new Paragraph(nombre == null ? " " : nombre, NEGRITA);
        quien.setAlignment(Element.ALIGN_CENTER);
        Paragraph rol = new Paragraph(cargo, PEQUENA);
        rol.setAlignment(Element.ALIGN_CENTER);
        celda.addElement(quien);
        celda.addElement(rol);
        return celda;
    }

    private static PdfPCell dato(String etiqueta, String valor) {
        PdfPCell celda = new PdfPCell();
        celda.setBorderColor(LINEA);
        celda.setPadding(4);
        celda.addElement(new Phrase(etiqueta, ETIQUETA));
        celda.addElement(new Phrase(valor, NORMAL));
        return celda;
    }

    private static PdfPCell celda(int alineacion) {
        PdfPCell celda = new PdfPCell();
        celda.setBorderColor(LINEA);
        celda.setPadding(4);
        celda.setHorizontalAlignment(alineacion);
        return celda;
    }

    private static PdfPCell texto(String valor, Font fuente) {
        PdfPCell celda = new PdfPCell(new Phrase(valor, fuente));
        celda.setBorderColor(LINEA);
        celda.setBorderWidthBottom(0);
        celda.setPadding(4);
        celda.setHorizontalAlignment(Element.ALIGN_CENTER);
        celda.setVerticalAlignment(Element.ALIGN_MIDDLE);
        return celda;
    }

    private static PdfPCell sinBorde(PdfPCell celda) {
        celda.setBorder(Rectangle.NO_BORDER);
        return celda;
    }
}
