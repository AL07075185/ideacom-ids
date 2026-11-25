package mx.ideacom.ids.api.web;

import jakarta.servlet.jsp.JspException;
import jakarta.servlet.jsp.tagext.SimpleTagSupport;
import java.io.IOException;
import java.util.Locale;

/**
 * Etiqueta personalizada {@code <ids:porcentaje valor="${fila.disponibilidad}"/>}:
 * muestra la disponibilidad con color segun el nivel (>= 99 % bueno, >= 95 % aviso,
 * menor critico) o un guion si no hubo tiempo medible.
 */
public class PorcentajeTag extends SimpleTagSupport {

    private Double valor;

    public void setValor(Double valor) {
        this.valor = valor;
    }

    @Override
    public void doTag() throws JspException, IOException {
        if (valor == null) {
            getJspContext().getOut().write("<span class=\"pct pct-nd\">—</span>");
            return;
        }
        String nivel = valor >= 99.0 ? "bien" : (valor >= 95.0 ? "aviso" : "mal");
        getJspContext().getOut().write(String.format(Locale.ROOT,
                "<span class=\"pct pct-%s\">%.1f %%</span>", nivel, valor));
    }
}
