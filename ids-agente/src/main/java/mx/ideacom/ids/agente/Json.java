package mx.ideacom.ids.agente;

import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * JSON minimo para el agente (sin librerias externas): convierte records,
 * listas y mapas a texto, y lee objetos JSON planos a Map.
 */
public final class Json {

    private Json() {
    }

    // ------------------------------------------------------------ escritura

    public static String escribir(Object valor) {
        StringBuilder sb = new StringBuilder();
        escribir(valor, sb);
        return sb.toString();
    }

    private static void escribir(Object v, StringBuilder sb) {
        if (v == null) {
            sb.append("null");
        } else if (v instanceof String s) {
            texto(s, sb);
        } else if (v instanceof Number || v instanceof Boolean) {
            sb.append(v);
        } else if (v instanceof Enum<?> e) {
            texto(e.name(), sb);
        } else if (v instanceof Map<?, ?> m) {
            sb.append('{');
            boolean primero = true;
            for (Map.Entry<?, ?> en : m.entrySet()) {
                if (en.getValue() == null) {
                    continue;
                }
                if (!primero) {
                    sb.append(',');
                }
                texto(String.valueOf(en.getKey()), sb);
                sb.append(':');
                escribir(en.getValue(), sb);
                primero = false;
            }
            sb.append('}');
        } else if (v instanceof Collection<?> c) {
            sb.append('[');
            boolean primero = true;
            for (Object o : c) {
                if (!primero) {
                    sb.append(',');
                }
                escribir(o, sb);
                primero = false;
            }
            sb.append(']');
        } else if (v.getClass().isRecord()) {
            Map<String, Object> campos = new LinkedHashMap<>();
            for (RecordComponent rc : v.getClass().getRecordComponents()) {
                try {
                    campos.put(rc.getName(), rc.getAccessor().invoke(v));
                } catch (ReflectiveOperationException e) {
                    throw new IllegalStateException("No se pudo leer " + rc.getName(), e);
                }
            }
            escribir(campos, sb);
        } else {
            texto(v.toString(), sb);
        }
    }

    private static void texto(String s, StringBuilder sb) {
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        sb.append('"');
    }

    // ------------------------------------------------------------ lectura

    /** Lee un objeto JSON. Numeros enteros como Long, decimales como Double. */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> leerObjeto(String json) {
        Object v = new Lector(json).leer();
        if (!(v instanceof Map)) {
            throw new IllegalArgumentException("Se esperaba un objeto JSON");
        }
        return (Map<String, Object>) v;
    }

    public static String texto(Map<String, Object> m, String clave) {
        Object v = m.get(clave);
        return v == null ? null : v.toString();
    }

    public static Integer entero(Map<String, Object> m, String clave) {
        Object v = m.get(clave);
        return v instanceof Number n ? n.intValue() : null;
    }

    private static final class Lector {
        private final String s;
        private int i;

        Lector(String s) {
            this.s = s == null ? "" : s;
        }

        Object leer() {
            Object v = valor();
            espacios();
            if (i != s.length()) {
                throw error("Contenido sobrante");
            }
            return v;
        }

        private Object valor() {
            espacios();
            if (i >= s.length()) {
                throw error("Fin inesperado");
            }
            char c = s.charAt(i);
            return switch (c) {
                case '{' -> objeto();
                case '[' -> arreglo();
                case '"' -> cadena();
                case 't' -> literal("true", Boolean.TRUE);
                case 'f' -> literal("false", Boolean.FALSE);
                case 'n' -> literal("null", null);
                default -> numero();
            };
        }

        private Map<String, Object> objeto() {
            Map<String, Object> m = new LinkedHashMap<>();
            i++;
            espacios();
            if (s.charAt(i) == '}') {
                i++;
                return m;
            }
            while (true) {
                espacios();
                String k = cadena();
                espacios();
                esperar(':');
                m.put(k, valor());
                espacios();
                char c = s.charAt(i++);
                if (c == '}') {
                    return m;
                }
                if (c != ',') {
                    throw error("Se esperaba , o }");
                }
            }
        }

        private List<Object> arreglo() {
            List<Object> l = new ArrayList<>();
            i++;
            espacios();
            if (s.charAt(i) == ']') {
                i++;
                return l;
            }
            while (true) {
                l.add(valor());
                espacios();
                char c = s.charAt(i++);
                if (c == ']') {
                    return l;
                }
                if (c != ',') {
                    throw error("Se esperaba , o ]");
                }
            }
        }

        private String cadena() {
            esperar('"');
            StringBuilder sb = new StringBuilder();
            while (true) {
                if (i >= s.length()) {
                    throw error("Cadena sin cerrar");
                }
                char c = s.charAt(i++);
                if (c == '"') {
                    return sb.toString();
                }
                if (c == '\\') {
                    char e = s.charAt(i++);
                    switch (e) {
                        case 'n' -> sb.append('\n');
                        case 'r' -> sb.append('\r');
                        case 't' -> sb.append('\t');
                        case 'b' -> sb.append('\b');
                        case 'f' -> sb.append('\f');
                        case 'u' -> {
                            sb.append((char) Integer.parseInt(s.substring(i, i + 4), 16));
                            i += 4;
                        }
                        default -> sb.append(e);
                    }
                } else {
                    sb.append(c);
                }
            }
        }

        private Object numero() {
            int inicio = i;
            while (i < s.length() && "+-0123456789.eE".indexOf(s.charAt(i)) >= 0) {
                i++;
            }
            String n = s.substring(inicio, i);
            if (n.isEmpty()) {
                throw error("Valor inesperado");
            }
            if (n.contains(".") || n.contains("e") || n.contains("E")) {
                return Double.parseDouble(n);
            }
            return Long.parseLong(n);
        }

        private Object literal(String palabra, Object valor) {
            if (!s.startsWith(palabra, i)) {
                throw error("Valor inesperado");
            }
            i += palabra.length();
            return valor;
        }

        private void esperar(char c) {
            if (i >= s.length() || s.charAt(i) != c) {
                throw error("Se esperaba '" + c + "'");
            }
            i++;
        }

        private void espacios() {
            while (i < s.length() && Character.isWhitespace(s.charAt(i))) {
                i++;
            }
        }

        private IllegalArgumentException error(String msg) {
            return new IllegalArgumentException("JSON invalido (" + msg + ") en posicion " + i);
        }
    }
}
