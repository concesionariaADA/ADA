package com.ada.servidor;

import com.ada.autenticacion.AlmacenSesiones;
import com.ada.autenticacion.HashContrasena;
import com.ada.autenticacion.ServicioAutenticacion;
import com.ada.catalogo.Marca;
import com.ada.catalogo.Modelo;
import com.ada.catalogo.RepositorioCatalogo;
import com.ada.catalogo.TipoVehiculo;
import com.ada.empleados.CredencialEmpleado;
import com.ada.empleados.RepositorioCredenciales;
import com.ada.panel.ResumenPanel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Catalog lists of the admin API ({@code /api/admin/marcas}, {@code /tipos-vehiculo} and
 * {@code /modelos}) through the real server, with a fake repository.
 */
class CatalogoAdminTest {

    private static final String CLAVE = "clave-secreta-1";
    private static final String HASH = HashContrasena.generar(CLAVE.toCharArray());
    private static final String JSON = "application/json; charset=utf-8";
    private static final String[] RUTAS = {"/api/admin/marcas", "/api/admin/tipos-vehiculo", "/api/admin/modelos"};
    private static final Pattern COOKIE_SESION = Pattern.compile(
            "ADA_SESION=([A-Za-z0-9_-]{43}); Path=/; HttpOnly; SameSite=Strict");

    @TempDir
    Path raiz;

    private ServidorWeb servidor;
    private final HttpClient cliente = HttpClient.newHttpClient();
    private final AtomicInteger consultas = new AtomicInteger();

    @AfterEach
    void detener() {
        if (servidor != null) {
            servidor.detener();
        }
    }

    @Test
    void marcasConSesionDevuelveLasMarcasConSusTiposEnJson() throws Exception {
        iniciar(new CatalogoFalso(List.of(
                new Marca(7, 474, "HONDA", List.of("AUTOMOVIL", "CAMIONETA", "MOTOCICLETA"), true, false),
                new Marca(9, null, "D'ARTAGNAN \"MOTO\"", List.of(), false, true)), List.of(), List.of()));
        HttpResponse<String> respuesta = get("/api/admin/marcas", sesion());
        assertEquals(200, respuesta.statusCode());
        assertEquals("[{\"id\":7,\"makeIdVpic\":474,\"nombre\":\"HONDA\","
                + "\"tipos\":[\"AUTOMOVIL\",\"CAMIONETA\",\"MOTOCICLETA\"],\"habilitada\":true,\"baja\":false},"
                + "{\"id\":9,\"makeIdVpic\":null,\"nombre\":\"D'ARTAGNAN \\\"MOTO\\\"\","
                + "\"tipos\":[],\"habilitada\":false,\"baja\":true}]", respuesta.body());
        assertEquals(JSON, tipo(respuesta));
        assertEquals("no-store", respuesta.headers().firstValue("Cache-Control").orElse(""));
        assertEquals(1, consultas.get());
    }

    @Test
    void tiposConSesionDevuelveLosTiposConSusConteosEnJson() throws Exception {
        iniciar(new CatalogoFalso(List.of(), List.of(
                new TipoVehiculo(1, "AUTOMOVIL", "car", 195, 1385),
                new TipoVehiculo(2, "CAMIONETA", null, 0, 0)), List.of()));
        HttpResponse<String> respuesta = get("/api/admin/tipos-vehiculo", sesion());
        assertEquals(200, respuesta.statusCode());
        assertEquals("[{\"id\":1,\"nombre\":\"AUTOMOVIL\",\"tipoVpic\":\"car\",\"marcas\":195,\"modelos\":1385},"
                + "{\"id\":2,\"nombre\":\"CAMIONETA\",\"tipoVpic\":null,\"marcas\":0,\"modelos\":0}]",
                respuesta.body());
        assertEquals(JSON, tipo(respuesta));
        assertEquals("no-store", respuesta.headers().firstValue("Cache-Control").orElse(""));
        assertEquals(1, consultas.get());
    }

    @Test
    void modelosConSesionDevuelveLosModelosConMarcaYTipoEnJson() throws Exception {
        iniciar(new CatalogoFalso(List.of(), List.of(), List.of(
                new Modelo(3, 2471, "Hilux", "TOYOTA", "CAMIONETA", false),
                new Modelo(4, null, "<Gol>", "VOLKSWAGEN", "AUTOMOVIL", true))));
        HttpResponse<String> respuesta = get("/api/admin/modelos", sesion());
        assertEquals(200, respuesta.statusCode());
        assertEquals("[{\"id\":3,\"modelIdVpic\":2471,\"nombre\":\"Hilux\",\"marca\":\"TOYOTA\","
                + "\"tipo\":\"CAMIONETA\",\"baja\":false},"
                + "{\"id\":4,\"modelIdVpic\":null,\"nombre\":\"\\u003cGol\\u003e\",\"marca\":\"VOLKSWAGEN\","
                + "\"tipo\":\"AUTOMOVIL\",\"baja\":true}]", respuesta.body());
        assertEquals(JSON, tipo(respuesta));
        assertEquals("no-store", respuesta.headers().firstValue("Cache-Control").orElse(""));
        assertEquals(1, consultas.get());
    }

    @Test
    void conCatalogoVacioCadaRutaDevuelveListaVacia() throws Exception {
        iniciar(new CatalogoFalso(List.of(), List.of(), List.of()));
        String token = sesion();
        for (String ruta : RUTAS) {
            HttpResponse<String> respuesta = get(ruta, token);
            assertEquals(200, respuesta.statusCode(), ruta);
            assertEquals("[]", respuesta.body(), ruta);
        }
    }

    @Test
    void sinSesionDevuelve401SinConsultarLaBase() throws Exception {
        iniciar(new CatalogoFalso(List.of(), List.of(), List.of()));
        for (String ruta : RUTAS) {
            for (String token : new String[] {null, "x".repeat(43)}) {
                HttpResponse<String> respuesta = get(ruta, token);
                assertEquals(401, respuesta.statusCode(), ruta + " " + token);
                assertEquals("{\"error\":\"no autorizado\"}", respuesta.body(), ruta);
                assertEquals(JSON, tipo(respuesta), ruta);
            }
        }
        assertEquals(0, consultas.get());
    }

    @Test
    void otroMetodoDevuelve405ConAllowGet() throws Exception {
        iniciar(new CatalogoFalso(List.of(), List.of(), List.of()));
        String token = sesion();
        for (String ruta : RUTAS) {
            for (String metodo : new String[] {"POST", "PUT", "DELETE"}) {
                HttpResponse<String> respuesta = enviar(solicitud(ruta, token)
                        .method(metodo, HttpRequest.BodyPublishers.noBody()).build());
                assertEquals(405, respuesta.statusCode(), ruta + " " + metodo);
                assertEquals("GET", respuesta.headers().firstValue("Allow").orElse(""), ruta + " " + metodo);
                assertEquals(JSON, tipo(respuesta), ruta + " " + metodo);
            }
        }
        assertEquals(0, consultas.get());
    }

    @Test
    void errorDeBaseDevuelve500OkFalse() throws Exception {
        iniciar(new CatalogoQueFalla(new SQLException("connection refused", "08001")));
        String token = sesion();
        for (String ruta : RUTAS) {
            HttpResponse<String> respuesta = get(ruta, token);
            assertEquals(500, respuesta.statusCode(), ruta);
            assertEquals("{\"ok\":false}", respuesta.body(), ruta);
            assertEquals(JSON, tipo(respuesta), ruta);
        }
    }

    @Test
    void baseSinConfigurarDevuelve500OkFalse() throws Exception {
        iniciar(new CatalogoQueFalla(new IllegalStateException("Missing setting POSTGRES_HOST")));
        String token = sesion();
        for (String ruta : RUTAS) {
            HttpResponse<String> respuesta = get(ruta, token);
            assertEquals(500, respuesta.statusCode(), ruta);
            assertEquals("{\"ok\":false}", respuesta.body(), ruta);
        }
    }

    @Test
    void servidorSinRepositorioDelCatalogoMantiene404YElDashboardSigue() throws Exception {
        ServicioAutenticacion servicio = servicio();
        servidor = new ServidorWeb(0, raiz.resolve("frontend"), servicio,
                () -> new ResumenPanel(1, 2, 3, 4, 5, 6));
        servidor.agregarFiltroAdmin(new FiltroSesion(servicio));
        servidor.iniciar();
        String token = sesion();
        for (String ruta : RUTAS) {
            HttpResponse<String> respuesta = get(ruta, token);
            assertEquals(404, respuesta.statusCode(), ruta);
            assertEquals("{\"error\":\"no encontrado\"}", respuesta.body(), ruta);
        }
        assertEquals(200, get("/api/admin/dashboard", token).statusCode());
    }

    // --- Fakes ---

    /** Returns fixed lists and counts every query. */
    private final class CatalogoFalso implements RepositorioCatalogo {
        private final List<Marca> marcas;
        private final List<TipoVehiculo> tipos;
        private final List<Modelo> modelos;

        CatalogoFalso(List<Marca> marcas, List<TipoVehiculo> tipos, List<Modelo> modelos) {
            this.marcas = marcas;
            this.tipos = tipos;
            this.modelos = modelos;
        }

        @Override
        public List<Marca> listarMarcas() {
            consultas.incrementAndGet();
            return marcas;
        }

        @Override
        public List<TipoVehiculo> listarTiposVehiculo() {
            consultas.incrementAndGet();
            return tipos;
        }

        @Override
        public List<Modelo> listarModelos() {
            consultas.incrementAndGet();
            return modelos;
        }
    }

    /** Every query fails with the given SQLException or IllegalStateException. */
    private static final class CatalogoQueFalla implements RepositorioCatalogo {
        private final Exception error;

        CatalogoQueFalla(Exception error) {
            this.error = error;
        }

        private <T> T fallar() throws SQLException {
            if (error instanceof SQLException sql) {
                throw sql;
            }
            throw (IllegalStateException) error;
        }

        @Override
        public List<Marca> listarMarcas() throws SQLException {
            return fallar();
        }

        @Override
        public List<TipoVehiculo> listarTiposVehiculo() throws SQLException {
            return fallar();
        }

        @Override
        public List<Modelo> listarModelos() throws SQLException {
            return fallar();
        }
    }

    // --- Helpers ---

    private void iniciar(RepositorioCatalogo repositorio) throws Exception {
        ServicioAutenticacion servicio = servicio();
        servidor = new ServidorWeb(0, raiz.resolve("frontend"), servicio,
                () -> new ResumenPanel(0, 0, 0, 0, 0, 0), repositorio);
        servidor.agregarFiltroAdmin(new FiltroSesion(servicio));
        servidor.iniciar();
    }

    private static ServicioAutenticacion servicio() {
        CredencialEmpleado admin = new CredencialEmpleado(1, "admin", HASH);
        RepositorioCredenciales credenciales =
                usuario -> "admin".equals(usuario) ? Optional.of(admin) : Optional.empty();
        return new ServicioAutenticacion(credenciales, new AlmacenSesiones(Clock.systemUTC()));
    }

    private String sesion() throws Exception {
        HttpResponse<String> respuesta = enviar(solicitud("/api/admin/login", null)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString("usuario=admin&contrasena=" + CLAVE))
                .build());
        Matcher coincidencia = COOKIE_SESION.matcher(respuesta.headers().firstValue("Set-Cookie").orElse(""));
        assertTrue(coincidencia.matches(), "no session cookie");
        return coincidencia.group(1);
    }

    private HttpResponse<String> get(String ruta, String token) throws Exception {
        return enviar(solicitud(ruta, token).GET().build());
    }

    private HttpRequest.Builder solicitud(String ruta, String token) {
        HttpRequest.Builder constructor = HttpRequest.newBuilder(
                URI.create("http://localhost:" + servidor.obtenerPuerto() + ruta));
        if (token != null) {
            constructor.header("Cookie", "ADA_SESION=" + token);
        }
        return constructor;
    }

    private HttpResponse<String> enviar(HttpRequest solicitud) throws Exception {
        return cliente.send(solicitud, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private static String tipo(HttpResponse<String> respuesta) {
        return respuesta.headers().firstValue("Content-Type").orElse("");
    }
}
