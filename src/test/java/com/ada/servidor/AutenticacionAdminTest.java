package com.ada.servidor;

import com.ada.autenticacion.AlmacenSesiones;
import com.ada.autenticacion.HashContrasena;
import com.ada.autenticacion.ServicioAutenticacion;
import com.ada.empleados.CredencialEmpleado;
import com.ada.empleados.RepositorioCredenciales;
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
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Admin login, logout, session endpoint and the session filter, through the real server. */
class AutenticacionAdminTest {

    private static final String CLAVE = "clave-secreta-1";
    private static final String HASH = HashContrasena.generar(CLAVE.toCharArray());
    private static final String USUARIO_RARO = "ad\"m\\in";
    private static final String JSON = "application/json; charset=utf-8";
    private static final Pattern COOKIE_SESION = Pattern.compile(
            "ADA_SESION=([A-Za-z0-9_-]{43}); Path=/; HttpOnly; SameSite=Strict");

    @TempDir
    Path raiz;

    private ServidorWeb servidor;
    private HttpClient cliente;
    private RelojAjustable reloj;

    @BeforeEach
    void iniciar() throws IOException {
        Path frontend = raiz.resolve("frontend");
        escribir(frontend.resolve("templates/frontoffice/index.html"), "<h1>Inicio publico</h1>");
        escribir(frontend.resolve("templates/admin/index.html"), "<h1>Panel admin</h1>");
        escribir(frontend.resolve("templates/admin/login.html"), "<h1>Login admin</h1>");
        escribir(frontend.resolve("css/x.css"), "body { color: #000; }");

        Map<String, CredencialEmpleado> empleados = Map.of(
                "admin", new CredencialEmpleado(1, "admin", HASH),
                USUARIO_RARO, new CredencialEmpleado(2, USUARIO_RARO, HASH));
        RepositorioCredenciales repositorio = usuario -> Optional.ofNullable(empleados.get(usuario));
        reloj = new RelojAjustable(Instant.parse("2026-01-01T10:00:00Z"));
        ServicioAutenticacion servicio = new ServicioAutenticacion(repositorio, new AlmacenSesiones(reloj));

        servidor = new ServidorWeb(0, frontend, servicio);
        servidor.agregarFiltroAdmin(new FiltroSesion(servicio));
        servidor.iniciar();
        cliente = HttpClient.newHttpClient();
    }

    @AfterEach
    void detener() {
        servidor.detener();
    }

    // --- POST /api/admin/login ---

    @Test
    void loginCorrectoDevuelve200YCookieDeSesion() throws Exception {
        HttpResponse<String> respuesta = login("usuario=admin&contrasena=" + CLAVE);
        assertEquals(200, respuesta.statusCode());
        assertEquals("{\"ok\":true}", respuesta.body());
        assertEquals(JSON, tipo(respuesta));
        String cookie = respuesta.headers().firstValue("Set-Cookie").orElse("");
        assertTrue(COOKIE_SESION.matcher(cookie).matches(), cookie);
        assertEquals("no-store", respuesta.headers().firstValue("Cache-Control").orElse(""));
    }

    @Test
    void loginDecodificaElFormulario() throws Exception {
        HttpResponse<String> respuesta = login("usuario=ad%22m%5Cin&contrasena=clave%2Dsecreta%2D1");
        assertEquals(200, respuesta.statusCode());
        assertEquals("{\"usuario\":\"ad\\\"m\\\\in\"}", get("/api/admin/sesion", token(respuesta)).body());
    }

    @Test
    void contrasenaIncorrectaYUsuarioInexistenteResponden401Identico() throws Exception {
        HttpResponse<String> malaClave = login("usuario=admin&contrasena=otra-clave");
        HttpResponse<String> sinUsuario = login("usuario=nadie&contrasena=" + CLAVE);
        for (HttpResponse<String> respuesta : java.util.List.of(malaClave, sinUsuario)) {
            assertEquals(401, respuesta.statusCode());
            assertEquals("{\"ok\":false}", respuesta.body());
            assertEquals(JSON, tipo(respuesta));
            assertTrue(respuesta.headers().firstValue("Set-Cookie").isEmpty());
        }
        assertEquals(malaClave.headers().map().keySet(), sinUsuario.headers().map().keySet());
    }

    @Test
    void camposFaltantesOVaciosDevuelven400() throws Exception {
        for (String cuerpo : new String[] {"", "usuario=admin", "contrasena=" + CLAVE,
                "usuario=&contrasena=" + CLAVE, "usuario=admin&contrasena=",
                "usuario=admin&usuario=otro&contrasena=" + CLAVE, "usuario=%zz&contrasena=x"}) {
            HttpResponse<String> respuesta = login(cuerpo);
            assertEquals(400, respuesta.statusCode(), cuerpo);
            assertEquals("{\"ok\":false}", respuesta.body(), cuerpo);
            assertTrue(respuesta.headers().firstValue("Set-Cookie").isEmpty(), cuerpo);
        }
    }

    @Test
    void cuerpoMayorA4KbSeRechaza() throws Exception {
        HttpResponse<String> respuesta = login("usuario=admin&contrasena=" + "a".repeat(4096));
        assertEquals(413, respuesta.statusCode());
        assertEquals("{\"ok\":false}", respuesta.body());
    }

    @Test
    void tipoDeContenidoDistintoDeFormularioSeRechaza() throws Exception {
        HttpRequest solicitud = solicitud("/api/admin/login", null)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"usuario\":\"admin\"}"))
                .build();
        HttpResponse<String> respuesta = enviar(solicitud);
        assertEquals(415, respuesta.statusCode());
        assertEquals("{\"ok\":false}", respuesta.body());
    }

    @Test
    void loginConOtroMetodoDevuelve405() throws Exception {
        HttpResponse<String> respuesta = get("/api/admin/login", null);
        assertEquals(405, respuesta.statusCode());
        assertEquals("POST", respuesta.headers().firstValue("Allow").orElse(""));
        assertEquals(JSON, tipo(respuesta));
    }

    @Test
    void nuevoLoginInvalidaLaSesionAnterior() throws Exception {
        String anterior = token(login("usuario=admin&contrasena=" + CLAVE));
        HttpRequest solicitud = solicitud("/api/admin/login", anterior)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString("usuario=admin&contrasena=" + CLAVE))
                .build();
        String nuevo = token(enviar(solicitud));
        assertNotEquals(anterior, nuevo);
        assertEquals(401, get("/api/admin/sesion", anterior).statusCode());
        assertEquals(200, get("/api/admin/sesion", nuevo).statusCode());
    }

    // --- Session filter on /admin and /api/admin ---

    @Test
    void adminSinSesionRedirigeAlLogin() throws Exception {
        for (String ruta : new String[] {"/admin", "/admin/", "/admin/index", "/admin/no-existe"}) {
            HttpResponse<String> respuesta = get(ruta, null);
            assertEquals(302, respuesta.statusCode(), ruta);
            assertEquals("/admin/login", respuesta.headers().firstValue("Location").orElse(""), ruta);
        }
    }

    @Test
    void paginaDeLoginSeSirveSinSesion() throws Exception {
        HttpResponse<String> respuesta = get("/admin/login", null);
        assertEquals(200, respuesta.statusCode());
        assertEquals("<h1>Login admin</h1>", respuesta.body());
    }

    @Test
    void adminConSesionSirveElPanel() throws Exception {
        String token = token(login("usuario=admin&contrasena=" + CLAVE));
        HttpResponse<String> respuesta = get("/admin", token);
        assertEquals(200, respuesta.statusCode());
        assertEquals("<h1>Panel admin</h1>", respuesta.body());
        assertEquals("no-store", respuesta.headers().firstValue("Cache-Control").orElse(""));
    }

    @Test
    void sesionSinCookieDevuelve401YConCookieElUsuario() throws Exception {
        HttpResponse<String> sinCookie = get("/api/admin/sesion", null);
        assertEquals(401, sinCookie.statusCode());
        assertEquals(JSON, tipo(sinCookie));

        String token = token(login("usuario=admin&contrasena=" + CLAVE));
        HttpResponse<String> conCookie = get("/api/admin/sesion", token);
        assertEquals(200, conCookie.statusCode());
        assertEquals("{\"usuario\":\"admin\"}", conCookie.body());
        assertEquals(JSON, tipo(conCookie));
    }

    @Test
    void cookieFalsificadaNoDaAcceso() throws Exception {
        for (String falsa : new String[] {"x".repeat(43), "abc", "", "../admin"}) {
            assertEquals(401, get("/api/admin/sesion", falsa).statusCode(), falsa);
            assertEquals(302, get("/admin", falsa).statusCode(), falsa);
        }
    }

    @Test
    void otraRutaDeApiAdminSin401YConSesion404() throws Exception {
        assertEquals(401, get("/api/admin/algo", null).statusCode());
        String token = token(login("usuario=admin&contrasena=" + CLAVE));
        HttpResponse<String> respuesta = get("/api/admin/algo", token);
        assertEquals(404, respuesta.statusCode());
        assertEquals(JSON, tipo(respuesta));
    }

    // --- POST /api/admin/logout ---

    @Test
    void logoutInvalidaLaSesionYVenceLaCookie() throws Exception {
        String token = token(login("usuario=admin&contrasena=" + CLAVE));
        HttpResponse<String> respuesta = enviar(solicitud("/api/admin/logout", token)
                .POST(HttpRequest.BodyPublishers.noBody()).build());
        assertEquals(200, respuesta.statusCode());
        assertEquals("{\"ok\":true}", respuesta.body());
        String cookie = respuesta.headers().firstValue("Set-Cookie").orElse("");
        assertTrue(cookie.startsWith("ADA_SESION=;"), cookie);
        assertTrue(cookie.contains("Max-Age=0"), cookie);
        assertEquals(401, get("/api/admin/sesion", token).statusCode());
        assertEquals(302, get("/admin", token).statusCode());
    }

    @Test
    void logoutSinSesionTambienResponde200() throws Exception {
        HttpResponse<String> respuesta = enviar(solicitud("/api/admin/logout", null)
                .POST(HttpRequest.BodyPublishers.noBody()).build());
        assertEquals(200, respuesta.statusCode());
        assertEquals("{\"ok\":true}", respuesta.body());
    }

    @Test
    void logoutConGetDevuelve405() throws Exception {
        assertEquals(405, get("/api/admin/logout", null).statusCode());
    }

    // --- Expiry ---

    @Test
    void sesionVenceTras30MinutosSinUso() throws Exception {
        String token = token(login("usuario=admin&contrasena=" + CLAVE));
        reloj.avanzar(Duration.ofMinutes(20));
        assertEquals(200, get("/api/admin/sesion", token).statusCode());
        reloj.avanzar(Duration.ofMinutes(20));
        assertEquals(200, get("/api/admin/sesion", token).statusCode());
        reloj.avanzar(Duration.ofMinutes(31));
        assertEquals(401, get("/api/admin/sesion", token).statusCode());
        assertEquals(302, get("/admin", token).statusCode());
    }

    @Test
    void servidorConLoginPeroSinFiltroNoArranca() {
        ServicioAutenticacion servicio = new ServicioAutenticacion(
                usuario -> Optional.empty(), new AlmacenSesiones(reloj));
        ServidorWeb sinFiltro = new ServidorWeb(0, raiz.resolve("frontend"), servicio);
        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class, sinFiltro::iniciar);
    }

    // --- Public area untouched ---

    @Test
    void rutasPublicasNoPidenSesion() throws Exception {
        assertEquals(200, get("/api/salud", null).statusCode());
        assertEquals(200, get("/", null).statusCode());
        assertEquals(200, get("/css/x.css", null).statusCode());
    }

    // --- Helpers ---

    private HttpResponse<String> login(String cuerpo) throws Exception {
        return enviar(solicitud("/api/admin/login", null)
                .header("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(cuerpo))
                .build());
    }

    private HttpResponse<String> get(String ruta, String token) throws Exception {
        return enviar(solicitud(ruta, token).GET().build());
    }

    private HttpRequest.Builder solicitud(String ruta, String token) {
        HttpRequest.Builder constructor = HttpRequest.newBuilder(
                URI.create("http://localhost:" + servidor.obtenerPuerto() + ruta));
        if (token != null) {
            constructor.header("Cookie", "otra=1; ADA_SESION=" + token);
        }
        return constructor;
    }

    private HttpResponse<String> enviar(HttpRequest solicitud) throws Exception {
        return cliente.send(solicitud, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private static String token(HttpResponse<String> respuesta) {
        Matcher coincidencia = COOKIE_SESION.matcher(respuesta.headers().firstValue("Set-Cookie").orElse(""));
        assertTrue(coincidencia.matches(), "no session cookie");
        return coincidencia.group(1);
    }

    private static String tipo(HttpResponse<String> respuesta) {
        return respuesta.headers().firstValue("Content-Type").orElse("");
    }

    private static void escribir(Path archivo, String contenido) throws IOException {
        Files.createDirectories(archivo.getParent());
        Files.writeString(archivo, contenido, StandardCharsets.UTF_8);
    }

    /** Test clock that only moves when told to; read from server threads. */
    private static final class RelojAjustable extends Clock {
        private volatile Instant ahora;

        RelojAjustable(Instant inicio) {
            this.ahora = inicio;
        }

        void avanzar(Duration duracion) {
            ahora = ahora.plus(duracion);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zona) {
            return this;
        }

        @Override
        public Instant instant() {
            return ahora;
        }
    }
}
