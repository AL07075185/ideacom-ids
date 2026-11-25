package mx.ideacom.ids.api.servicio;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import mx.ideacom.ids.api.repositorio.ConfiguracionRepositorio;

/** Lee parametros de la tabla configuracion con una cache de 60 s. */
@ApplicationScoped
@Transactional(Transactional.TxType.SUPPORTS)
public class ConfiguracionServicio {

    public static final String INTERVALO = "latido.intervalo.segundos";
    public static final String UMBRAL = "latido.umbral";
    public static final String SESION_HORAS = "sesion.horas";

    private static final long VIGENCIA_CACHE_MS = 60_000;

    private record Valor(String texto, long leidoEn) {
    }

    private final Map<String, Valor> cache = new ConcurrentHashMap<>();

    @Inject
    private ConfiguracionRepositorio repositorio;

    public int entero(String clave, int porDefecto) {
        Valor v = cache.get(clave);
        long ahora = System.currentTimeMillis();
        if (v == null || ahora - v.leidoEn() > VIGENCIA_CACHE_MS) {
            String texto = repositorio.buscar(clave).map(c -> c.getValor()).orElse(null);
            v = new Valor(texto, ahora);
            cache.put(clave, v);
        }
        try {
            return v.texto() == null ? porDefecto : Integer.parseInt(v.texto().trim());
        } catch (NumberFormatException e) {
            return porDefecto;
        }
    }

    public int intervaloSegundos() {
        return Math.max(10, entero(INTERVALO, 60));
    }

    public int umbral() {
        return Math.max(1, entero(UMBRAL, 3));
    }

    public int sesionHoras() {
        return Math.max(1, entero(SESION_HORAS, 8));
    }
}
