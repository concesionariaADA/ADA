package com.ada.servidor;

import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpServer;
import io.github.cdimascio.dotenv.Dotenv;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * HTTP server of the application, built only on the JDK {@link HttpServer} (D03b).
 *
 * <p>The same server delivers the pages of {@code frontend/} and the API, so no CORS
 * setup is needed. Routes are split into two areas:
 * <ul>
 *   <li>Public area: {@code /}, {@code /css/**}, {@code /js/**}, {@code /img/**} and
 *       {@code /api/salud}.</li>
 *   <li>Admin area: {@code /admin} and {@code /api/admin}, registered only in
 *       {@link #registrarContextosAdmin(HttpServer)}.</li>
 * </ul>
 *
 * <p><b>Extension point for authentication:</b> every {@link Filter} passed to
 * {@link #agregarFiltroAdmin(Filter)} before {@link #iniciar()} is attached to every
 * admin context (and to none of the public ones). The admin login task plugs its
 * session filter in there; this class implements no authentication itself.
 */
public final class ServidorWeb {

    /** Key of the {@code .env} file that holds the server port. */
    public static final String CLAVE_PUERTO = "SERVIDOR_PUERTO";
    /** Port used when {@value #CLAVE_PUERTO} is not set. */
    public static final int PUERTO_POR_DEFECTO = 8080;
    /** Default location of the frontend, relative to the working directory. */
    public static final Path FRONTEND_POR_DEFECTO = Path.of("frontend");

    private static final String RUTA_ADMIN = "/admin";
    private static final String RUTA_API_ADMIN = "/api/admin";
    /** Requests handled in parallel; enough for a development server. */
    private static final int HILOS = 8;

    private final int puerto;
    private final Path directorioFrontend;
    private final List<Filter> filtrosAdmin = new ArrayList<>();
    private HttpServer servidor;
    private ExecutorService ejecutor;

    /** Server on the given port serving the default {@code frontend/} directory. */
    public ServidorWeb(int puerto) {
        this(puerto, FRONTEND_POR_DEFECTO);
    }

    /**
     * @param puerto             port to listen on; {@code 0} picks a free port
     * @param directorioFrontend base directory of the frontend files
     */
    public ServidorWeb(int puerto, Path directorioFrontend) {
        this.puerto = puerto;
        this.directorioFrontend = directorioFrontend.toAbsolutePath().normalize();
    }

    /**
     * Adds a filter that runs before every request of the admin area
     * ({@code /admin} and {@code /api/admin}). Public routes are never filtered.
     * Must be called before {@link #iniciar()} so no admin route is ever exposed
     * without its filters.
     */
    public synchronized void agregarFiltroAdmin(Filter filtro) {
        if (servidor != null) {
            throw new IllegalStateException("Admin filters must be added before starting the server.");
        }
        filtrosAdmin.add(filtro);
    }

    /** Binds the port and starts serving requests. */
    public synchronized void iniciar() throws IOException {
        if (servidor != null) {
            throw new IllegalStateException("The server is already running.");
        }
        // Loopback only: the development server must not be reachable from the local network.
        HttpServer nuevo = HttpServer.create(
                new InetSocketAddress(InetAddress.getLoopbackAddress(), puerto), 0);
        registrarContextosPublicos(nuevo);
        registrarContextosAdmin(nuevo);
        ejecutor = Executors.newFixedThreadPool(HILOS);
        nuevo.setExecutor(ejecutor);
        nuevo.start();
        servidor = nuevo;
    }

    /** Stops the server; does nothing if it was not started. */
    public synchronized void detener() {
        if (servidor != null) {
            servidor.stop(0);
            servidor = null;
            ejecutor.shutdown();
            ejecutor = null;
        }
    }

    /** Actual listening port once started (useful with port 0), else the configured one. */
    public synchronized int obtenerPuerto() {
        return servidor != null ? servidor.getAddress().getPort() : puerto;
    }

    private void registrarContextosPublicos(HttpServer destino) {
        destino.createContext("/api/salud", new ManejadorSalud());
        destino.createContext("/", new ManejadorPublico(directorioFrontend));
    }

    /**
     * Registers the admin area. Every context created here receives the filters
     * from {@link #agregarFiltroAdmin(Filter)}; new admin routes belong here.
     */
    private List<HttpContext> registrarContextosAdmin(HttpServer destino) {
        List<HttpContext> contextos = List.of(
                destino.createContext(RUTA_ADMIN, new ManejadorAdmin(directorioFrontend)),
                destino.createContext(RUTA_API_ADMIN, new ManejadorApiAdmin()));
        for (HttpContext contexto : contextos) {
            contexto.getFilters().addAll(filtrosAdmin);
        }
        return contextos;
    }

    /** Reads {@value #CLAVE_PUERTO} from {@code .env} (or the environment). */
    public static int leerPuertoDelEntorno() {
        Dotenv entorno = Dotenv.configure().ignoreIfMissing().load();
        return interpretarPuerto(entorno.get(CLAVE_PUERTO));
    }

    /**
     * Turns the raw {@value #CLAVE_PUERTO} value into a port number.
     *
     * @return {@link #PUERTO_POR_DEFECTO} when the value is missing or blank
     * @throws IllegalArgumentException when the value is not a number in 0..65535
     */
    public static int interpretarPuerto(String valor) {
        if (valor == null || valor.isBlank()) {
            return PUERTO_POR_DEFECTO;
        }
        String limpio = valor.trim();
        try {
            int numero = Integer.parseInt(limpio);
            if (numero >= 0 && numero <= 65535) {
                return numero;
            }
        } catch (NumberFormatException e) {
            // Falls through to the error below.
        }
        throw new IllegalArgumentException("Invalid setting " + CLAVE_PUERTO + ": '" + limpio
                + "' is not a port number between 0 and 65535. Fix it in .env.");
    }
}
