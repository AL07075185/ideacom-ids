package mx.ideacom.ids.api.servicio;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Lectura y escritura de CSV (RFC 4180) sin librerias externas.
 * <ul>
 *   <li>Escribe con BOM UTF-8 y fin de linea CRLF para que Excel muestre bien acentos.</li>
 *   <li>Lee archivos separados por coma o por punto y coma (Excel en algunos idiomas).</li>
 * </ul>
 */
public final class Csv {

    private static final char BOM = '﻿';

    private Csv() {
    }

    // ------------------------------------------------------------ escritura

    public static String escribir(List<String> encabezados, List<List<Object>> filas) {
        StringBuilder sb = new StringBuilder().append(BOM);
        linea(sb, new ArrayList<>(encabezados));
        for (List<Object> fila : filas) {
            linea(sb, fila);
        }
        return sb.toString();
    }

    private static void linea(StringBuilder sb, List<?> valores) {
        for (int i = 0; i < valores.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            Object v = valores.get(i);
            String t = v == null ? "" : v.toString();
            boolean comillas = t.indexOf(',') >= 0 || t.indexOf(';') >= 0 || t.indexOf('"') >= 0
                    || t.indexOf('\n') >= 0 || t.indexOf('\r') >= 0;
            if (comillas) {
                sb.append('"').append(t.replace("\"", "\"\"")).append('"');
            } else {
                sb.append(t);
            }
        }
        sb.append("\r\n");
    }

    // ------------------------------------------------------------ lectura

    /**
     * Lee un CSV con encabezado. Cada fila se devuelve como mapa con la clave
     * normalizada (minusculas, sin acentos ni simbolos): "Conectado a" y
     * "conectadoA" quedan ambas como "conectadoa".
     *
     * @throws IllegalArgumentException si el archivo esta vacio o tiene comillas sin cerrar
     */
    public static List<Map<String, String>> leer(String texto) {
        if (texto == null || texto.isBlank()) {
            throw new IllegalArgumentException("El archivo está vacío");
        }
        String t = texto.charAt(0) == BOM ? texto.substring(1) : texto;
        char separador = detectarSeparador(t);
        List<List<String>> registros = registros(t, separador);
        if (registros.isEmpty()) {
            throw new IllegalArgumentException("El archivo no tiene encabezado");
        }
        List<String> encabezados = registros.get(0).stream().map(Csv::clave).toList();
        List<Map<String, String>> filas = new ArrayList<>();
        for (int i = 1; i < registros.size(); i++) {
            List<String> r = registros.get(i);
            if (r.stream().allMatch(String::isBlank)) {
                continue;
            }
            Map<String, String> fila = new LinkedHashMap<>();
            for (int c = 0; c < encabezados.size(); c++) {
                fila.put(encabezados.get(c), c < r.size() ? r.get(c).trim() : "");
            }
            filas.add(fila);
        }
        return filas;
    }

    /** Normaliza un nombre de columna: minusculas, sin acentos, solo letras y numeros. */
    public static String clave(String nombre) {
        if (nombre == null) {
            return "";
        }
        String sinAcentos = Normalizer.normalize(nombre, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return sinAcentos.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    private static char detectarSeparador(String t) {
        int fin = t.indexOf('\n');
        String primera = fin < 0 ? t : t.substring(0, fin);
        long comas = primera.chars().filter(c -> c == ',').count();
        long puntoComa = primera.chars().filter(c -> c == ';').count();
        return puntoComa > comas ? ';' : ',';
    }

    private static List<List<String>> registros(String t, char sep) {
        List<List<String>> salida = new ArrayList<>();
        List<String> actual = new ArrayList<>();
        StringBuilder campo = new StringBuilder();
        boolean enComillas = false;
        for (int i = 0; i < t.length(); i++) {
            char c = t.charAt(i);
            if (enComillas) {
                if (c == '"') {
                    if (i + 1 < t.length() && t.charAt(i + 1) == '"') {
                        campo.append('"');
                        i++;
                    } else {
                        enComillas = false;
                    }
                } else {
                    campo.append(c);
                }
            } else if (c == '"') {
                enComillas = true;
            } else if (c == sep) {
                actual.add(campo.toString());
                campo.setLength(0);
            } else if (c == '\n' || c == '\r') {
                if (c == '\r' && i + 1 < t.length() && t.charAt(i + 1) == '\n') {
                    i++;
                }
                actual.add(campo.toString());
                campo.setLength(0);
                salida.add(actual);
                actual = new ArrayList<>();
            } else {
                campo.append(c);
            }
        }
        if (enComillas) {
            throw new IllegalArgumentException("El CSV tiene comillas sin cerrar");
        }
        if (campo.length() > 0 || !actual.isEmpty()) {
            actual.add(campo.toString());
            salida.add(actual);
        }
        return salida;
    }
}
