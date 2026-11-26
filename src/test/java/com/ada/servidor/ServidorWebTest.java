package com.ada.servidor;

import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.HttpExchange;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServidorWebTest {

    private static final String SECRETO = "contenido-secreto-fuera-de-frontend";

    @TempDir
    Path raiz;

    private ServidorWeb servidor;
    private HttpClient cliente;

    @BeforeEach
    void prepararFrontend() throws IOException {
        Path frontend = raiz.resolve("frontend");
        escribir(frontend.resolve("templates/frontoffice/index.html"), "<h1>Inicio publico</h1>");
        escribir(frontend.resolve("templates/admin/index.html"), "<h1>Panel admin</h1>");
        escribir(frontend.resolve("templates/admin/login.html"), "<h1>Login admin</h1>");
        escribir(frontend.resolve("css/x.css"), "body { color: #000; }");
        escribir(frontend.resolve("js/api.js"), "export const a = 1;");
        Files.createDirectories(frontend.resolve("img"));
        Files.write(frontend.resolve("img/logo.png"), new byte[] {(byte) 0x89, 'P', 'N', 'G'});
        // Secret placed outside the frontend base directory: must never be served.
        escribir(raiz.resolve("secret.txt"), SECRETO);
        escribir(raiz.resolve("secret.html"), SECRETO);
        escribir(raiz.resolve("secret"), SECRETO);

        servidor = new ServidorWeb(0, frontend);
        cliente = HttpClient.newHttpClient();
    }

    @AfterEach
    void detenerServidor() {
        if (servidor != null) {
            servidor.detener();
        }
    }

    // --- /api/salud ---

    @Test
    void saludRespondeOkEnJson() throws Exception {
        servidor.iniciar();
        HttpResponse<String> respuesta = get("/api/salud");
        assertEquals(200, respuesta.statusCode());
        assertEquals("{\"estado\":\"ok\"}", respuesta.body());
        assertEquals("application/json; charset=utf-8",
                respuesta.headers().firstValue("Content-Type").orElse(""));
    }

    @Test
    void saludRechazaPostCon405() throws Exception {
        servidor.iniciar();
        HttpResponse<String> respuesta = enviar("POST", "/api/salud");
        assertEquals(405, respuesta.statusCode());
        assertEquals("GET", respuesta.headers().firstValue("Allow").orElse(""));
    }

    // --- Public static files ---

    @Test
    void raizSirveIndexDelFrontoffice() throws Exception {
        servidor.iniciar();
        HttpResponse<String> respuesta = get("/");
        assertEquals(200, respuesta.statusCode());
        assertEquals("<h1>Inicio publico</h1>", respuesta.body());
        assertEquals("text/html; charset=utf-8",
                respuesta.headers().firstValue("Content-Type").orElse(""));
    }

    @Test
    void cssSeSirveConContentTypeCss() throws Exception {
        servidor.iniciar();
        HttpResponse<String> respuesta = get("/css/x.css");
        assertEquals(200, respuesta.statusCode());
        assertEquals("body { color: #000; }", respuesta.body());
        assertEquals("text/css; charset=utf-8",
                respuesta.headers().firstValue("Content-Type").orElse(""));
    }

    @Test
    void jsEImagenesUsanSuContentType() throws Exception {
        servidor.iniciar();
        assertEquals("text/javascript; charset=utf-8",
                get("/js/api.js").headers().firstValue("Content-Type").orElse(""));
        HttpResponse<String> imagen = get("/img/logo.png");
        assertEquals(200, imagen.statusCode());
        assertEquals("image/png", imagen.headers().firstValue("Content-Type").orElse(""));
    }

    @Test
    void headDevuelve200SinCuerpo() throws Exception {
        servidor.iniciar();
        HttpResponse<String> respuesta = enviar("HEAD", "/");
        assertEquals(200, respuesta.statusCode());
        assertEquals("", respuesta.body());
    }

    @Test
    void archivosEstaticosRechazanPostCon405() throws Exception {
        servidor.iniciar();
        assertEquals(405, enviar("POST", "/").statusCode());
        assertEquals(405, enviar("POST", "/css/x.css").statusCode());
        assertEquals(405, enviar("POST", "/admin/login").statusCode());
    }

    @Test
    void archivoInexistenteDevuelve404() throws Exception {
        servidor.iniciar();
        assertEquals(404, get("/css/no-existe.css").statusCode());
        assertEquals(404, get("/otra-ruta").statusCode());
        assertEquals(404, get("/admin/no-existe").statusCode());
    }

    // --- Admin area ---

    @Test
    void adminSirveSuIndexYSusPaginas() throws Exception {
        servidor.iniciar();
        HttpResponse<String> login = get("/admin/login");
        assertEquals(200, login.statusCode());
        assertEquals("<h1>Login admin</h1>", login.body());
        assertEquals("text/html; charset=utf-8",
                login.headers().firstValue("Content-Type").orElse(""));
        HttpResponse<String> indice = get("/admin");
        assertEquals(200, indice.statusCode());
        assertEquals("<h1>Panel admin</h1>", indice.body());
    }

    @Test
    void filtroAdminSoloAfectaAdminYApiAdmin() throws Exception {
        servidor.agregarFiltroAdmin(filtroQueRechaza());
        servidor.iniciar();
        assertEquals(401, get("/admin/login").statusCode());
        assertEquals(401, get("/admin").statusCode());
        assertEquals(401, get("/api/admin/algo").statusCode());
        assertEquals(200, get("/").statusCode());
        assertEquals(200, get("/css/x.css").statusCode());
        assertEquals(200, get("/api/salud").statusCode());
    }

    @Test
    void filtroAdminNoSePuedeAgregarConElServidorIniciado() throws Exception {
        servidor.iniciar();
        assertThrows(IllegalStateException.class, () -> servidor.agregarFiltroAdmin(filtroQueRechaza()));
    }

    // --- Path traversal ---

    @Test
    void traversalConPuntosNoEscapaDelFrontend() throws Exception {
        servidor.iniciar();
        assertNoSirveSecreto(get("/css/../../secret.txt"));
    }

    @Test
    void traversalCodificadoNoEscapaDelFrontend() throws Exception {
        servidor.iniciar();
        assertNoSirveSecreto(get("/css/%2e%2e/%2e%2e/secret.txt"));
        assertNoSirveSecreto(get("/css/%2E%2E%2F%2E%2E%2Fsecret.txt"));
    }

    @Test
    void traversalEnAdminNoEscapaDelFrontend() throws Exception {
        servidor.iniciar();
        assertNoSirveSecreto(get("/admin/..%2f..%2fsecret"));
        assertNoSirveSecreto(get("/admin/..%2f..%2f..%2fsecret"));
    }

    @Test
    void barrasInvertidasYRutasAbsolutasDevuelven404() throws Exception {
        servidor.iniciar();
        assertNoSirveSecreto(get("/css/..%5c..%5csecret.txt"));
        String absoluta = raiz.resolve("secret.txt").toAbsolutePath().toString().replace('\\', '/');
        assertNoSirveSecreto(get("/css/" + absoluta.replace(":", "%3a")));
        assertNoSirveSecreto(get("/css//" + absoluta.replace(":", "%3a")));
    }

    @Test
    void rutaPublicaNoAlcanzaPaginasDelAdmin() throws Exception {
        servidor.iniciar();
        assertEquals(404, get("/css/../templates/admin/login.html").statusCode());
        assertEquals(404, get("/css/%2e%2e/templates/admin/login.html").statusCode());
    }

    // --- Port from .env ---

    @Test
    void puertoPorDefectoCuandoFalta() {
        assertEquals(8080, ServidorWeb.interpretarPuerto(null));
        assertEquals(8080, ServidorWeb.interpretarPuerto(""));
        assertEquals(8080, ServidorWeb.interpretarPuerto("   "));
    }

    @Test
    void puertoNumericoSeRespeta() {
        assertEquals(9090, ServidorWeb.interpretarPuerto("9090"));
        assertEquals(9090, ServidorWeb.interpretarPuerto(" 9090 "));
    }

    @Test
    void puertoNoNumericoDaErrorClaro() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> ServidorWeb.interpretarPuerto("ochenta"));
        assertTrue(error.getMessage().contains("SERVIDOR_PUERTO"), error.getMessage());
        assertThrows(IllegalArgumentException.class, () -> ServidorWeb.interpretarPuerto("70000"));
        assertThrows(IllegalArgumentException.class, () -> ServidorWeb.interpretarPuerto("-1"));
    }

    // --- Helpers ---

    private HttpResponse<String> get(String ruta) throws Exception {
        return enviar("GET", ruta);
    }

    private HttpResponse<String> enviar(String metodo, String ruta) throws Exception {
        HttpRequest solicitud = HttpRequest.newBuilder(
                        URI.create("http://localhost:" + servidor.obtenerPuerto() + ruta))
                .method(metodo, HttpRequest.BodyPublishers.noBody())
                .build();
        return cliente.send(solicitud, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private static void assertNoSirveSecreto(HttpResponse<String> respuesta) {
        assertEquals(404, respuesta.statusCode());
        assertFalse(respuesta.body().contains(SECRETO));
    }

    private static Filter filtroQueRechaza() {
        return new Filter() {
            @Override
            public void doFilter(HttpExchange intercambio, Chain cadena) throws IOException {
                intercambio.sendResponseHeaders(401, -1);
                intercambio.close();
            }

            @Override
            public String description() {
                return "rejects every request (test only)";
            }
        };
    }

    private static void escribir(Path archivo, String contenido) throws IOException {
        Files.createDirectories(archivo.getParent());
        Files.writeString(archivo, contenido, StandardCharsets.UTF_8);
    }
}
