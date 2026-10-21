package co.edu.elencano.plataforma.matricula.servicio;

import java.io.IOException;
import java.io.InputStream;
import java.text.Normalizer;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import co.edu.elencano.plataforma.comun.excepcion.ReglaNegocioException;

/**
 * Lee la primera hoja de un archivo de Excel como una tabla: la primera fila con varios valores es el
 * encabezado y cada columna se reconoce por su nombre, sin importar mayusculas, tildes, espacios o guiones.
 */
final class LectorExcel {

    /** Fila del archivo con su numero (como lo muestra Excel) y los valores por nombre de columna. */
    record Fila(int numero, Map<String, String> valores) {

        String valor(String columna) {
            String v = valores.get(columna);
            return v == null || v.isBlank() ? null : v.trim();
        }
    }

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE;

    private LectorExcel() {
    }

    /**
     * @param alias para cada columna esperada, los nombres con que puede venir en el encabezado
     * @param maximoFilas filas de datos que se aceptan como maximo
     */
    static List<Fila> leer(InputStream contenido, Map<String, List<String>> alias, int maximoFilas) {
        Map<String, String> columnaPorAlias = new HashMap<>();
        alias.forEach((columna, nombres) -> nombres.forEach(n -> columnaPorAlias.put(normalizar(n), columna)));
        try (Workbook libro = WorkbookFactory.create(contenido)) {
            Sheet hoja = libro.getSheetAt(0);
            DataFormatter formato = new DataFormatter();
            Map<Integer, String> encabezado = null;
            List<Fila> filas = new ArrayList<>();
            for (Row fila : hoja) {
                if (encabezado == null) {
                    encabezado = encabezado(fila, formato, columnaPorAlias);
                    continue;
                }
                Map<String, String> valores = new LinkedHashMap<>();
                boolean vacia = true;
                for (Map.Entry<Integer, String> columna : encabezado.entrySet()) {
                    String valor = texto(fila.getCell(columna.getKey()), formato);
                    if (!valor.isBlank()) {
                        vacia = false;
                    }
                    valores.put(columna.getValue(), valor);
                }
                if (vacia) {
                    continue;
                }
                if (filas.size() >= maximoFilas) {
                    throw new ReglaNegocioException("El archivo tiene más de " + maximoFilas
                            + " filas. Divídalo en varios archivos");
                }
                filas.add(new Fila(fila.getRowNum() + 1, valores));
            }
            if (encabezado == null) {
                throw new ReglaNegocioException("El archivo está vacío");
            }
            return filas;
        } catch (IOException | RuntimeException e) {
            if (e instanceof ReglaNegocioException regla) {
                throw regla;
            }
            throw new ReglaNegocioException("No se pudo leer el archivo. Use un archivo de Excel (.xlsx)");
        }
    }

    /** Devuelve el encabezado si la fila tiene al menos tres columnas reconocidas; si no, nulo. */
    private static Map<Integer, String> encabezado(Row fila, DataFormatter formato, Map<String, String> columnaPorAlias) {
        Map<Integer, String> columnas = new LinkedHashMap<>();
        for (Cell celda : fila) {
            String columna = columnaPorAlias.get(normalizar(texto(celda, formato)));
            if (columna != null && !columnas.containsValue(columna)) {
                columnas.put(celda.getColumnIndex(), columna);
            }
        }
        return columnas.size() >= 3 ? columnas : null;
    }

    /** Las fechas salen en formato aaaa-mm-dd; los numeros, sin decimales ni separadores de miles. */
    private static String texto(Cell celda, DataFormatter formato) {
        if (celda == null) {
            return "";
        }
        CellType tipo = celda.getCellType() == CellType.FORMULA ? celda.getCachedFormulaResultType() : celda.getCellType();
        if (tipo == CellType.NUMERIC) {
            if (DateUtil.isCellDateFormatted(celda)) {
                return celda.getLocalDateTimeCellValue().toLocalDate().format(ISO);
            }
            double numero = celda.getNumericCellValue();
            if (numero == Math.rint(numero) && Math.abs(numero) < 1e15) {
                return String.valueOf((long) numero);
            }
        }
        return formato.formatCellValue(celda).trim();
    }

    /** Minusculas, sin tildes y solo letras y numeros, para comparar nombres escritos de distintas formas. */
    static String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        String sinTildes = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return sinTildes.toLowerCase().replaceAll("[^a-z0-9]", "");
    }
}
