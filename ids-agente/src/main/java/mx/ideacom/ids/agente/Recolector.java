package mx.ideacom.ids.agente;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Recolecta los datos del equipo usando solo Java estandar (sin librerias):
 * nombre, SO, CPU, RAM, disco, IP y MAC de la interfaz que sale hacia la API.
 */
public final class Recolector {

    private static final Logger LOG = Logger.getLogger(Recolector.class.getName());

    /** Datos tecnicos del equipo. */
    public record InfoEquipo(String hostname, String ip, String ipv6, String mac, String soNombre, String soVersion,
                             String arquitectura, String cpuModelo, Long ramMb, Long discoGb) {
    }

    private final URI apiUrl;

    public Recolector(URI apiUrl) {
        this.apiUrl = apiUrl;
    }

    public InfoEquipo recolectar() {
        InetAddress local = direccionHaciaApi();
        return new InfoEquipo(
                hostname(),
                local == null ? null : local.getHostAddress(),
                ipv6(local),
                mac(local),
                System.getProperty("os.name"),
                versionSo(),
                System.getProperty("os.arch"),
                cpu(),
                ramMb(),
                discoGb());
    }

    /** IPv6 actual de la interfaz que sale hacia la API, o null. */
    public String ipv6Actual() {
        return ipv6(direccionHaciaApi());
    }

    /**
     * Indica si hay al menos una interfaz de red activa con direccion valida
     * (sin contar loopback ni direcciones de enlace local). Si es false, el
     * equipo esta sin red local: cable desconectado, WiFi apagado, etc.
     */
    public boolean hayRedLocal() {
        try {
            for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!ni.isUp() || ni.isLoopback()) {
                    continue;
                }
                for (InetAddress a : Collections.list(ni.getInetAddresses())) {
                    if (!a.isLinkLocalAddress() && !a.isLoopbackAddress()) {
                        return true;
                    }
                }
            }
        } catch (SocketException e) {
            LOG.fine("No se pudieron revisar las interfaces: " + e.getMessage());
        }
        return false;
    }

    /** IP local actual (para el latido; detecta cambios de IP). */
    public String ipActual() {
        InetAddress a = direccionHaciaApi();
        return a == null ? null : a.getHostAddress();
    }

    // ------------------------------------------------------------ red

    /**
     * Direccion local que el sistema usaria para llegar a la API. Un socket UDP
     * "conectado" no envia paquetes: solo consulta la tabla de rutas.
     */
    InetAddress direccionHaciaApi() {
        String host = apiUrl.getHost();
        int puerto = apiUrl.getPort() > 0 ? apiUrl.getPort() : 80;
        try (DatagramSocket s = new DatagramSocket()) {
            s.connect(new InetSocketAddress(host, puerto));
            InetAddress a = s.getLocalAddress();
            if (a != null && !a.isAnyLocalAddress()) {
                return a;
            }
        } catch (IOException | RuntimeException e) {
            LOG.fine("No se pudo resolver la ruta hacia la API: " + e.getMessage());
        }
        return primeraInterfazActiva();
    }

    private static InetAddress primeraInterfazActiva() {
        try {
            for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!ni.isUp() || ni.isLoopback() || ni.isVirtual()) {
                    continue;
                }
                for (InetAddress a : Collections.list(ni.getInetAddresses())) {
                    if (a instanceof Inet4Address && !a.isLinkLocalAddress()) {
                        return a;
                    }
                }
            }
        } catch (SocketException e) {
            LOG.fine("No se pudieron listar las interfaces: " + e.getMessage());
        }
        return null;
    }

    /**
     * Primera IPv6 global (o de sitio) de la misma interfaz que la IPv4 local;
     * se omiten las de enlace local (fe80::). Sin el sufijo de zona (%eth0).
     */
    static String ipv6(InetAddress local) {
        try {
            NetworkInterface ni = local == null ? null : NetworkInterface.getByInetAddress(local);
            if (ni == null) {
                return null;
            }
            for (InetAddress a : Collections.list(ni.getInetAddresses())) {
                if (a instanceof Inet6Address && !a.isLinkLocalAddress() && !a.isLoopbackAddress()) {
                    return limpiarIpv6(a.getHostAddress());
                }
            }
        } catch (SocketException e) {
            LOG.fine("Sin IPv6: " + e.getMessage());
        }
        return null;
    }

    static String limpiarIpv6(String texto) {
        if (texto == null) {
            return null;
        }
        int zona = texto.indexOf('%');
        return zona >= 0 ? texto.substring(0, zona) : texto;
    }

    static String mac(InetAddress local) {
        try {
            NetworkInterface ni = local == null ? null : NetworkInterface.getByInetAddress(local);
            byte[] hw = ni == null ? null : ni.getHardwareAddress();
            if (hw == null) {
                for (NetworkInterface otra : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                    if (otra.isUp() && !otra.isLoopback() && otra.getHardwareAddress() != null) {
                        hw = otra.getHardwareAddress();
                        break;
                    }
                }
            }
            return hw == null ? null : formatoMac(hw);
        } catch (SocketException e) {
            return null;
        }
    }

    static String formatoMac(byte[] hw) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < hw.length; i++) {
            if (i > 0) {
                sb.append(':');
            }
            sb.append(String.format("%02X", hw[i]));
        }
        return sb.toString();
    }

    private static String hostname() {
        try {
            String h = InetAddress.getLocalHost().getHostName();
            if (h != null && !h.isBlank() && !h.equals("localhost")) {
                return h;
            }
        } catch (IOException e) {
            LOG.fine("Sin hostname por DNS local");
        }
        String env = System.getenv("COMPUTERNAME");
        if (env == null) {
            env = System.getenv("HOSTNAME");
        }
        return env != null ? env : "equipo-sin-nombre";
    }

    // ------------------------------------------------------------ sistema

    private static boolean esWindows() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }

    private static boolean esMac() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("mac");
    }

    /** En Linux agrega el nombre de la distribucion (/etc/os-release). */
    private static String versionSo() {
        String version = System.getProperty("os.version");
        Path osRelease = Path.of("/etc/os-release");
        if (!esWindows() && Files.isReadable(osRelease)) {
            try {
                for (String linea : Files.readAllLines(osRelease, StandardCharsets.UTF_8)) {
                    if (linea.startsWith("PRETTY_NAME=")) {
                        String nombre = linea.substring(12).replace("\"", "").trim();
                        return nombre + " (kernel " + version + ")";
                    }
                }
            } catch (IOException e) {
                LOG.fine("No se pudo leer /etc/os-release");
            }
        }
        return version;
    }

    private static String cpu() {
        String modelo = null;
        try {
            if (esWindows()) {
                modelo = valorComando(List.of("reg", "query",
                        "HKLM\\HARDWARE\\DESCRIPTION\\System\\CentralProcessor\\0", "/v", "ProcessorNameString"),
                        "ProcessorNameString");
            } else if (esMac()) {
                modelo = primeraLinea(List.of("sysctl", "-n", "machdep.cpu.brand_string"));
            } else {
                Path info = Path.of("/proc/cpuinfo");
                if (Files.isReadable(info)) {
                    for (String l : Files.readAllLines(info, StandardCharsets.UTF_8)) {
                        if (l.startsWith("model name")) {
                            modelo = l.substring(l.indexOf(':') + 1).trim();
                            break;
                        }
                    }
                }
            }
        } catch (IOException | RuntimeException e) {
            LOG.fine("No se pudo leer el modelo de CPU: " + e.getMessage());
        }
        if (modelo == null || modelo.isBlank()) {
            modelo = System.getenv("PROCESSOR_IDENTIFIER");
        }
        int nucleos = Runtime.getRuntime().availableProcessors();
        return (modelo == null || modelo.isBlank() ? "CPU" : modelo.trim()) + " (" + nucleos + " núcleos)";
    }

    private static Long ramMb() {
        try {
            var os = ManagementFactory.getOperatingSystemMXBean();
            if (os instanceof com.sun.management.OperatingSystemMXBean sun) {
                return sun.getTotalMemorySize() / (1024L * 1024L);
            }
        } catch (RuntimeException | LinkageError e) {
            LOG.fine("No se pudo leer la RAM: " + e.getMessage());
        }
        return null;
    }

    /** Suma de las unidades raiz (C:, D:... en Windows; / en Linux). */
    private static Long discoGb() {
        long total = 0;
        File[] raices = File.listRoots();
        if (raices != null) {
            for (File r : raices) {
                total += r.getTotalSpace();
            }
        }
        return total == 0 ? null : total / (1024L * 1024L * 1024L);
    }

    private static String primeraLinea(List<String> comando) throws IOException {
        Process p = new ProcessBuilder(comando).redirectErrorStream(true).start();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
            String linea = r.readLine();
            esperar(p);
            return linea;
        }
    }

    private static String valorComando(List<String> comando, String clave) throws IOException {
        Process p = new ProcessBuilder(comando).redirectErrorStream(true).start();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
            String linea;
            while ((linea = r.readLine()) != null) {
                int i = linea.indexOf(clave);
                if (i >= 0) {
                    String resto = linea.substring(i + clave.length()).trim();
                    // Formato: ProcessorNameString    REG_SZ    Intel(R) Core(TM) ...
                    int sz = resto.indexOf("REG_SZ");
                    return sz >= 0 ? resto.substring(sz + 6).trim() : resto;
                }
            }
            esperar(p);
        }
        return null;
    }

    private static void esperar(Process p) {
        try {
            if (!p.waitFor(3, TimeUnit.SECONDS)) {
                p.destroyForcibly();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            p.destroyForcibly();
        }
    }
}
