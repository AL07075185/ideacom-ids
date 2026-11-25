package mx.ideacom.ids.agente;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * Punto de entrada: {@code java -jar ids-agente.jar [ruta/agente.properties]}.
 * Si no se indica ruta, busca agente.properties en la carpeta actual.
 */
public final class AgenteMain {

    private AgenteMain() {
    }

    public static void main(String[] args) {
        Path archivo = Path.of(args.length > 0 ? args[0] : "agente.properties");
        if (!Files.exists(archivo)) {
            System.err.println("No se encontro " + archivo.toAbsolutePath()
                    + ". Copia agente.properties.ejemplo como agente.properties y ajusta api.url y codigo.alta.");
            System.exit(2);
        }
        try {
            ConfigAgente config = ConfigAgente.cargar(archivo);
            configurarLog(config.datosDir());
            new Agente(config).iniciar();
        } catch (IllegalArgumentException e) {
            System.err.println("Configuracion invalida: " + e.getMessage());
            System.exit(2);
        } catch (IOException e) {
            System.err.println("No se pudo iniciar el agente: " + e.getMessage());
            System.exit(1);
        }
    }

    /** Log en consola y en datos.dir/agente.log (rotativo, 3 archivos de 1 MB). */
    private static void configurarLog(Path dir) throws IOException {
        System.setProperty("java.util.logging.SimpleFormatter.format",
                "%1$tF %1$tT %4$-7s %5$s%6$s%n");
        Files.createDirectories(dir);
        Logger raiz = Logger.getLogger("");
        for (Handler h : raiz.getHandlers()) {
            raiz.removeHandler(h);
        }
        ConsoleHandler consola = new ConsoleHandler();
        consola.setFormatter(new SimpleFormatter());
        consola.setLevel(Level.INFO);
        FileHandler archivo = new FileHandler(dir.resolve("agente.%g.log").toString(), 1_000_000, 3, true);
        archivo.setFormatter(new SimpleFormatter());
        archivo.setLevel(Level.INFO);
        raiz.addHandler(consola);
        raiz.addHandler(archivo);
        raiz.setLevel(Level.INFO);
    }
}
