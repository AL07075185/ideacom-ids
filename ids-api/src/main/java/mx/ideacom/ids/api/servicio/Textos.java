package mx.ideacom.ids.api.servicio;

import java.util.Locale;
import mx.ideacom.ids.api.excepcion.ApiException;

/** Validaciones y normalizaciones de texto compartidas por los servicios. */
final class Textos {

    private Textos() {
    }

    static boolean vacio(String s) {
        return s == null || s.isBlank();
    }

    /** Recorta y limita la longitud; cadena vacia se vuelve null. */
    static String limpio(String s, int max) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        if (t.isEmpty()) {
            return null;
        }
        return t.length() > max ? t.substring(0, max) : t;
    }

    static String obligatorio(String s, int max, String campo) {
        String t = limpio(s, max);
        if (t == null) {
            throw ApiException.peticionInvalida("El campo '" + campo + "' es obligatorio");
        }
        return t;
    }

    /** Normaliza una MAC a formato AA:BB:CC:DD:EE:FF. */
    static String mac(String mac) {
        if (vacio(mac)) {
            return null;
        }
        String hex = mac.replaceAll("[^0-9A-Fa-f]", "").toUpperCase(Locale.ROOT);
        if (hex.length() != 12) {
            throw ApiException.peticionInvalida("MAC invalida: " + mac);
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 12; i += 2) {
            if (i > 0) {
                sb.append(':');
            }
            sb.append(hex, i, i + 2);
        }
        return sb.toString();
    }

    static <E extends Enum<E>> E enumeracion(Class<E> tipo, String valor, String campo) {
        if (vacio(valor)) {
            return null;
        }
        try {
            return Enum.valueOf(tipo, valor.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw ApiException.peticionInvalida("Valor inválido para '" + campo + "': " + valor);
        }
    }
}
