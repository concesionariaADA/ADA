package com.ada.servidor;

import com.ada.autenticacion.AlmacenSesiones;
import com.ada.autenticacion.HashContrasena;
import com.ada.autenticacion.ServicioAutenticacion;
import com.ada.empleados.CredencialEmpleado;
import com.ada.empleados.RepositorioCredenciales;
import com.ada.panel.RepositorioPanel;
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
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** {@code GET /api/admin/dashboard} through the real server, with a fake repository. */
class DashboardAdminTest {

    private static final String CLAVE = "clave-secreta-1";
    private static final String HASH = HashContrasena.generar(CLAVE.toCharArray());
    private static final String JSON = "application/json; charset=utf-8";
    private static final String RUTA = "/api/admin/dashboard";
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
    void conSesionDevuelveLosConteosEnJson() throws Exception {
        iniciar(contando(() -> new ResumenPanel(12, 3, 7, 5, 40, 6)));
        HttpResponse<String> respuesta = get(RUTA, sesion());
        assertEquals(200, respuesta.statusCode());
        assertEquals("{\"unidadesDisponibles\":12,\"unidadesReservadas\":3,\"unidadesVendidas\":7,"
                + "\"ventasConfirmadas\":5,\"clientes\":40,\"empleados\":6}", respuesta.body());
        assertEquals(JSON, tipo(respuesta));
        assertEquals("no-store", respuesta.headers().firstValue("Cache-Control").orElse(""));
        assertEquals(1, consultas.get());
    }

    @Test
    void conBaseVaciaDevuelveCeros() throws Exception {
        iniciar(contando(() -> new ResumenPanel(0, 0, 0, 0, 0, 0)));
        HttpResponse<String> respuesta = get(RUTA, sesion());
        assertEquals(200, respuesta.statusCode());
        assertEquals("{\"unidadesDisponibles\":0,\"unidadesReservadas\":0,\"unidadesVendidas\":0,"
                + "\"ventasConfirmadas\":0,\"clientes\":0,\"empleados\":0}", respuesta.body());
    }

    @Test
    void sinSesionDevuelve401SinConsultarLaBase() throws Exception {
        iniciar(contando(() -> new ResumenPanel(1, 1, 1, 1, 1, 1)));
        for (String token : new String[] {null, "x".repeat(43)}) {
            HttpResponse<String> respuesta = get(RUTA, token);
            assertEquals(401, respuesta.statusCode(), String.valueOf(token));
            assertEquals("{\"error\":\"no autorizado\"}", respuesta.body());
            assertEquals(JSON, tipo(respuesta));
        }
        assertEquals(0, consultas.get());
    }

    @Test
    void otroMetodoDevuelve405ConAllowGet() throws Exception {
        iniciar(contando(() -> new ResumenPanel(1, 1, 1, 1, 1, 1)));
        String token = sesion();
        for (String metodo : new String[] {"POST", "PUT", "DELETE"}) {
            HttpResponse<String> respuesta = enviar(solicitud(RUTA, token)
                    .method(metodo, HttpRequest.BodyPublishers.noBody()).build());
            assertEquals(405, respuesta.statusCode(), metodo);
            assertEquals("GET", respuesta.headers().firstValue("Allow").orElse(""), metodo);
            assertEquals(JSON, tipo(respuesta), metodo);
        }
        assertEquals(0, consultas.get());
    }

    @Test
    void errorDeBaseDevuelve500OkFalse() throws Exception {
        iniciar(() -> {
            throw new SQLException("connection refused", "08001");
        });
        HttpResponse<String> respuesta = get(RUTA, sesion());
        assertEquals(500, respuesta.statusCode());
        assertEquals("{\"ok\":false}", respuesta.body());
        assertEquals(JSON, tipo(respuesta));
    }

    @Test
    void baseSinConfigurarDevuelve500OkFalse() throws Exception {
        iniciar(() -> {
            throw new IllegalStateException("Missing setting POSTGRES_HOST");
        });
        HttpResponse<String> respuesta = get(RUTA, sesion());
        assertEquals(500, respuesta.statusCode());
        assertEquals("{\"ok\":false}", respuesta.body());
    }

    @Test
    void servidorSinRepositorioDelPanelMantiene404() throws Exception {
        ServicioAutenticacion servicio = servicio();
        servidor = new ServidorWeb(0, raiz.resolve("frontend"), servicio);
        servidor.agregarFiltroAdmin(new FiltroSesion(servicio));
        servidor.iniciar();
        HttpResponse<String> respuesta = get(RUTA, sesion());
        assertEquals(404, respuesta.statusCode());
        assertEquals("{\"error\":\"no encontrado\"}", respuesta.body());
    }

    // --- Helpers ---

    private RepositorioPanel contando(RepositorioPanel repositorio) {
        return () -> {
            consultas.incrementAndGet();
            return repositorio.obtenerResumen();
        };
    }

    private void iniciar(RepositorioPanel repositorio) throws Exception {
        ServicioAutenticacion servicio = servicio();
        servidor = new ServidorWeb(0, raiz.resolve("frontend"), servicio, repositorio);
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
