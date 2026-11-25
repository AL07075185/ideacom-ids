package mx.ideacom.ids.api.dto;

import java.util.List;

/** Nodos y aristas en el formato que espera vis-network. */
public record MapaDTO(List<Nodo> nodos, List<Arista> aristas) {

    public record Nodo(Integer id, String nombre, String tipo, String ip, String estado, String sitio) {
    }

    public record Arista(Integer desde, Integer hacia) {
    }
}
