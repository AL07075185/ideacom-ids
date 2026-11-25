package mx.ideacom.ids.api.web;

import jakarta.servlet.jsp.JspException;
import jakarta.servlet.jsp.tagext.SimpleTagSupport;
import java.io.IOException;
import java.util.Map;

/**
 * Etiqueta personalizada {@code <ids:estado valor="EN_LINEA"/>}: dibuja el
 * estado de un dispositivo como una etiqueta de color con su texto en espanol.
 * Declarada en /WEB-INF/ids.tld.
 */
public class EstadoTag extends SimpleTagSupport {

    private static final Map<String, String> TEXTOS = Map.of(
            "EN_LINEA", "En línea",
            "SIN_INTERNET", "Sin internet",
            "APAGADO", "Apagado",
            "DESCONECTADO", "Desconectado",
            "SIN_AGENTE", "Sin agente",
            "DESCONOCIDO", "Sin reportar");

    private String valor;

    public void setValor(String valor) {
        this.valor = valor;
    }

    @Override
    public void doTag() throws JspException, IOException {
        if (valor == null || valor.isBlank()) {
            return;
        }
        String clave = valor.replaceAll("[^A-Z_]", "");
        String texto = TEXTOS.getOrDefault(clave, clave);
        getJspContext().getOut().write("<span class=\"estado estado-" + clave + "\">" + texto + "</span>");
    }
}
