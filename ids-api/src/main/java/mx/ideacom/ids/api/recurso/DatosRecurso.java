package mx.ideacom.ids.api.recurso;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.time.LocalDate;
import java.util.List;
import mx.ideacom.ids.api.dto.EventoDTO;
import mx.ideacom.ids.api.dto.FilaDispositivoDTO;
import mx.ideacom.ids.api.dto.IncidenciaDTO;
import mx.ideacom.ids.api.dto.RespaldoDTO;
import mx.ideacom.ids.api.dto.ResultadoImportacionDTO;
import mx.ideacom.ids.api.dto.ResultadoRestauracionDTO;
import mx.ideacom.ids.api.excepcion.ApiException;
import mx.ideacom.ids.api.seguridad.Acceso;
import mx.ideacom.ids.api.seguridad.Sujeto;
import mx.ideacom.ids.api.servicio.Csv;
import mx.ideacom.ids.api.servicio.ExportacionServicio;
import mx.ideacom.ids.api.servicio.ImportacionServicio;
import mx.ideacom.ids.api.servicio.RespaldoServicio;

/**
 * Exportar, importar y respaldar datos desde el panel.
 * Las descargas llevan Content-Disposition para que el navegador las guarde como archivo.
 */
@Path("datos")
@RequestScoped
public class DatosRecurso {

    private static final String CSV = "text/csv; charset=UTF-8";
    private static final String JSON = "application/json; charset=UTF-8";

    @Inject
    private ExportacionServicio exportacion;

    @Inject
    private ImportacionServicio importacion;

    @Inject
    private RespaldoServicio respaldo;

    @Context
    private HttpServletRequest req;

    // ------------------------------------------------------------ exportar

    @GET
    @Path("dispositivos")
    @Produces({CSV, JSON})
    public Response exportarDispositivos(@QueryParam("formato") @DefaultValue("csv") String formato,
                                         @QueryParam("registro") String registro) {
        List<FilaDispositivoDTO> filas = exportacion.dispositivos(Acceso.usuario(req), registro);
        return esJson(formato)
                ? descarga(filas, JSON, "dispositivos", "json")
                : descarga(exportacion.dispositivosCsv(filas), CSV, "dispositivos", "csv");
    }

    @GET
    @Path("incidencias")
    @Produces({CSV, JSON})
    public Response exportarIncidencias(@QueryParam("formato") @DefaultValue("csv") String formato,
                                        @QueryParam("estado") String estado) {
        List<IncidenciaDTO> lista = exportacion.incidencias(Acceso.usuario(req), estado);
        return esJson(formato)
                ? descarga(lista, JSON, "incidencias", "json")
                : descarga(exportacion.incidenciasCsv(lista), CSV, "incidencias", "csv");
    }

    @GET
    @Path("dispositivos/{id: \\d+}/eventos")
    @Produces({CSV, JSON})
    public Response exportarEventos(@PathParam("id") Integer id,
                                    @QueryParam("formato") @DefaultValue("csv") String formato,
                                    @QueryParam("dias") @DefaultValue("30") int dias) {
        List<EventoDTO> lista = exportacion.eventos(Acceso.usuario(req), id, dias);
        return esJson(formato)
                ? descarga(lista, JSON, "historial-" + id, "json")
                : descarga(exportacion.eventosCsv(lista), CSV, "historial-" + id, "csv");
    }

    // ------------------------------------------------------------ importar

    /** CSV en el cuerpo (texto). simular=true devuelve la vista previa sin guardar. */
    @POST
    @Path("dispositivos/importar")
    @Consumes({"text/csv", MediaType.TEXT_PLAIN})
    @Produces(MediaType.APPLICATION_JSON)
    public ResultadoImportacionDTO importarCsv(@QueryParam("simular") @DefaultValue("true") boolean simular,
                                               String cuerpo) {
        Acceso.admin(req);
        List<FilaDispositivoDTO> filas;
        try {
            filas = Csv.leer(cuerpo).stream().map(FilaDispositivoDTO::desdeCsv).toList();
        } catch (IllegalArgumentException e) {
            throw ApiException.peticionInvalida(e.getMessage());
        }
        return importacion.importar(filas, 2, !simular, false);
    }

    /** JSON: el mismo formato que produce la exportacion de dispositivos. */
    @POST
    @Path("dispositivos/importar")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ResultadoImportacionDTO importarJson(@QueryParam("simular") @DefaultValue("true") boolean simular,
                                                List<FilaDispositivoDTO> filas) {
        Acceso.admin(req);
        return importacion.importar(filas, 1, !simular, false);
    }

    // ------------------------------------------------------------ respaldo

    @GET
    @Path("respaldo")
    @Produces(MediaType.APPLICATION_JSON)
    public Response descargarRespaldo(@QueryParam("eventos") @DefaultValue("false") boolean conEventos) {
        Acceso.admin(req);
        return descarga(respaldo.generar(conEventos), JSON, "respaldo", "json");
    }

    @POST
    @Path("respaldo/restaurar")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ResultadoRestauracionDTO restaurar(RespaldoDTO cuerpo) {
        Sujeto admin = Acceso.admin(req);
        return respaldo.restaurar(cuerpo, admin);
    }

    // ------------------------------------------------------------ auxiliares

    private static boolean esJson(String formato) {
        return "json".equalsIgnoreCase(formato);
    }

    private static Response descarga(Object contenido, String tipo, String nombre, String extension) {
        String archivo = "ids-" + nombre + "-" + LocalDate.now() + "." + extension;
        return Response.ok(contenido, tipo)
                .header("Content-Disposition", "attachment; filename=\"" + archivo + "\"")
                .build();
    }
}
