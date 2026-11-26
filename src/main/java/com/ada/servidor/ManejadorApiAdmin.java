package com.ada.servidor;

import com.ada.autenticacion.ServicioAutenticacion;
import com.ada.catalogo.Marca;
import com.ada.catalogo.RepositorioMarcas;
import com.ada.panel.RepositorioPanel;
import com.ada.panel.ResumenPanel;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Admin API ({@code /api/admin/**}). Session endpoints (Issue #3):
 * <ul>
 *   <li>{@code POST /api/admin/login} — form fields {@code usuario} and {@code contrasena};
 *       200 {@code {"ok":true}} plus the session cookie, or 401 {@code {"ok":false}}
 *       for a wrong user or password alike.</li>
 *   <li>{@code POST /api/admin/logout} — closes the session (if any) and deletes the cookie.</li>
 *   <li>{@code GET /api/admin/sesion} — {@code {"usuario":"..."}} of the current session, or 401.</li>
 *   <li>{@code GET /api/admin/dashboard} — dashboard counts as JSON (only when a
 *       {@link RepositorioPanel} is given); 500 {@code {"ok":false}} on a database error.
 *       The session filter guards it like every other {@code /api/admin} path.</li>
 *   <li>{@code GET /api/admin/marcas} — brands of the local catalog with their vehicle types,
 *       as a JSON array ordered by name (only when a {@link RepositorioMarcas} is given);
 *       500 {@code {"ok":false}} on a database error.</li>
 * </ul>
 * Any other path answers 404 JSON. Without a {@link ServicioAutenticacion} every path
 * answers 404. Passwords are never logged.
 */
final class ManejadorApiAdmin implements HttpHandler {

    static final String RUTA_LOGIN = "/api/admin/login";
    static final String RUTA_LOGOUT = "/api/admin/logout";
    static final String RUTA_SESION = "/api/admin/sesion";
    static final String RUTA_DASHBOARD = "/api/admin/dashboard";
    static final String RUTA_MARCAS = "/api/admin/marcas";

    /** Largest login body accepted, in bytes. */
    static final int CUERPO_MAXIMO = 4096;

    private static final String TIPO_FORMULARIO = "application/x-www-form-urlencoded";
    private static final String OK = "{\"ok\":true}";
    private static final String RECHAZO = "{\"ok\":false}";

    private final ServicioAutenticacion autenticacion;
    private final RepositorioPanel repositorioPanel;
    private final RepositorioMarcas repositorioMarcas;

    /** @param autenticacion login service, or null for no endpoints (all 404) */
    ManejadorApiAdmin(ServicioAutenticacion autenticacion) {
        this(autenticacion, null);
    }

    /**
     * @param autenticacion    login service, or null for no endpoints (all 404)
     * @param repositorioPanel dashboard counts, or null for no dashboard endpoint (404)
     */
    ManejadorApiAdmin(ServicioAutenticacion autenticacion, RepositorioPanel repositorioPanel) {
        this(autenticacion, repositorioPanel, null);
    }

    /**
     * @param autenticacion     login service, or null for no endpoints (all 404)
     * @param repositorioPanel  dashboard counts, or null for no dashboard endpoint (404)
     * @param repositorioMarcas brand list, or null for no brands endpoint (404)
     */
    ManejadorApiAdmin(ServicioAutenticacion autenticacion, RepositorioPanel repositorioPanel,
                      RepositorioMarcas repositorioMarcas) {
        this.autenticacion = autenticacion;
        this.repositorioPanel = repositorioPanel;
        this.repositorioMarcas = repositorioMarcas;
    }

    @Override
    public void handle(HttpExchange intercambio) throws IOException {
        try (intercambio) {
            intercambio.getResponseHeaders().set("Cache-Control", "no-store");
            String ruta = intercambio.getRequestURI().getPath();
            if (autenticacion == null) {
                noEncontrado(intercambio);
            } else if (RUTA_LOGIN.equals(ruta)) {
                iniciarSesion(intercambio);
            } else if (RUTA_LOGOUT.equals(ruta)) {
                cerrarSesion(intercambio);
            } else if (RUTA_SESION.equals(ruta)) {
                consultarSesion(intercambio);
            } else if (RUTA_DASHBOARD.equals(ruta) && repositorioPanel != null) {
                consultarDashboard(intercambio);
            } else if (RUTA_MARCAS.equals(ruta) && repositorioMarcas != null) {
                consultarMarcas(intercambio);
            } else {
                noEncontrado(intercambio);
            }
        }
    }

    private void iniciarSesion(HttpExchange intercambio) throws IOException {
        if (!soloMetodo(intercambio, "POST")) {
            return;
        }
        if (!esFormulario(intercambio.getRequestHeaders().getFirst("Content-Type"))) {
            Respuestas.enviarJson(intercambio, 415, RECHAZO);
            return;
        }
        byte[] cuerpo;
        try (InputStream entrada = intercambio.getRequestBody()) {
            cuerpo = entrada.readNBytes(CUERPO_MAXIMO + 1);
        }
        if (cuerpo.length > CUERPO_MAXIMO) {
            Respuestas.enviarJson(intercambio, 413, RECHAZO);
            return;
        }
        Map<String, String> campos = leerFormulario(new String(cuerpo, StandardCharsets.UTF_8));
        String usuario = campos == null ? null : campos.get("usuario");
        String contrasena = campos == null ? null : campos.get("contrasena");
        if (usuario == null || usuario.isEmpty() || contrasena == null || contrasena.isEmpty()) {
            Respuestas.enviarJson(intercambio, 400, RECHAZO);
            return;
        }
        char[] clave = contrasena.toCharArray();
        Optional<String> token;
        try {
            token = autenticacion.iniciarSesion(usuario, clave);
        } catch (SQLException e) {
            // Never log the request: it carries the password.
            System.err.println("Admin login: database error (SQLState " + e.getSQLState() + ")");
            Respuestas.enviarJson(intercambio, 500, RECHAZO);
            return;
        } catch (IllegalStateException e) {
            System.err.println("Admin login: " + e.getMessage());
            Respuestas.enviarJson(intercambio, 500, RECHAZO);
            return;
        } finally {
            Arrays.fill(clave, '\0');
        }
        if (token.isEmpty()) {
            Respuestas.enviarJson(intercambio, 401, RECHAZO);
            return;
        }
        // A fresh token on every login: a session id planted before login is discarded.
        CookieSesion.leer(intercambio).ifPresent(autenticacion::cerrarSesion);
        CookieSesion.escribir(intercambio, token.get());
        Respuestas.enviarJson(intercambio, 200, OK);
    }

    private void cerrarSesion(HttpExchange intercambio) throws IOException {
        if (!soloMetodo(intercambio, "POST")) {
            return;
        }
        CookieSesion.leer(intercambio).ifPresent(autenticacion::cerrarSesion);
        CookieSesion.borrar(intercambio);
        Respuestas.enviarJson(intercambio, 200, OK);
    }

    private void consultarSesion(HttpExchange intercambio) throws IOException {
        if (!soloMetodo(intercambio, "GET")) {
            return;
        }
        // Checked here too, not only in the filter, so this endpoint never trusts its wiring.
        Optional<String> usuario = CookieSesion.leer(intercambio).flatMap(autenticacion::usuarioDeSesion);
        if (usuario.isEmpty()) {
            Respuestas.noAutorizado(intercambio);
            return;
        }
        Respuestas.enviarJson(intercambio, 200, "{\"usuario\":" + Respuestas.textoJson(usuario.get()) + "}");
    }

    private void consultarDashboard(HttpExchange intercambio) throws IOException {
        if (!soloMetodo(intercambio, "GET")) {
            return;
        }
        ResumenPanel resumen;
        try {
            resumen = repositorioPanel.obtenerResumen();
        } catch (SQLException e) {
            System.err.println("Admin dashboard: database error (SQLState " + e.getSQLState() + ")");
            Respuestas.enviarJson(intercambio, 500, RECHAZO);
            return;
        } catch (IllegalStateException e) {
            System.err.println("Admin dashboard: " + e.getMessage());
            Respuestas.enviarJson(intercambio, 500, RECHAZO);
            return;
        }
        Respuestas.enviarJson(intercambio, 200, "{\"unidadesDisponibles\":" + resumen.unidadesDisponibles()
                + ",\"unidadesReservadas\":" + resumen.unidadesReservadas()
                + ",\"unidadesVendidas\":" + resumen.unidadesVendidas()
                + ",\"ventasConfirmadas\":" + resumen.ventasConfirmadas()
                + ",\"clientes\":" + resumen.clientes()
                + ",\"empleados\":" + resumen.empleados() + "}");
    }

    private void consultarMarcas(HttpExchange intercambio) throws IOException {
        if (!soloMetodo(intercambio, "GET")) {
            return;
        }
        List<Marca> marcas;
        try {
            marcas = repositorioMarcas.listar();
        } catch (SQLException e) {
            System.err.println("Admin brands: database error (SQLState " + e.getSQLState() + ")");
            Respuestas.enviarJson(intercambio, 500, RECHAZO);
            return;
        } catch (IllegalStateException e) {
            System.err.println("Admin brands: " + e.getMessage());
            Respuestas.enviarJson(intercambio, 500, RECHAZO);
            return;
        }
        StringBuilder json = new StringBuilder("[");
        for (Marca marca : marcas) {
            if (json.length() > 1) {
                json.append(',');
            }
            json.append("{\"id\":").append(marca.id())
                    .append(",\"makeIdVpic\":").append(marca.makeIdVpic())
                    .append(",\"nombre\":").append(Respuestas.textoJson(marca.nombre()))
                    .append(",\"tipos\":[");
            for (int i = 0; i < marca.tipos().size(); i++) {
                json.append(i == 0 ? "" : ",").append(Respuestas.textoJson(marca.tipos().get(i)));
            }
            json.append("],\"habilitada\":").append(marca.habilitada())
                    .append(",\"baja\":").append(marca.baja()).append('}');
        }
        Respuestas.enviarJson(intercambio, 200, json.append(']').toString());
    }

    private static boolean soloMetodo(HttpExchange intercambio, String metodo) throws IOException {
        if (metodo.equals(intercambio.getRequestMethod())) {
            return true;
        }
        intercambio.getResponseHeaders().set("Allow", metodo);
        Respuestas.enviarJson(intercambio, 405, "{\"error\":\"metodo no permitido\"}");
        return false;
    }

    private static void noEncontrado(HttpExchange intercambio) throws IOException {
        Respuestas.enviarJson(intercambio, 404, "{\"error\":\"no encontrado\"}");
    }

    /** True for {@code application/x-www-form-urlencoded}, with or without parameters. */
    static boolean esFormulario(String tipoContenido) {
        if (tipoContenido == null) {
            return false;
        }
        int puntoYComa = tipoContenido.indexOf(';');
        String tipo = puntoYComa < 0 ? tipoContenido : tipoContenido.substring(0, puntoYComa);
        return TIPO_FORMULARIO.equals(tipo.trim().toLowerCase(Locale.ROOT));
    }

    /**
     * Decodes a UTF-8 form body. Returns null when it is malformed (bad percent
     * encoding) or repeats a field, so ambiguous input is rejected.
     */
    static Map<String, String> leerFormulario(String cuerpo) {
        Map<String, String> campos = new HashMap<>();
        for (String par : cuerpo.split("&")) {
            if (par.isEmpty()) {
                continue;
            }
            int igual = par.indexOf('=');
            String clave = igual < 0 ? par : par.substring(0, igual);
            String valor = igual < 0 ? "" : par.substring(igual + 1);
            try {
                clave = URLDecoder.decode(clave, StandardCharsets.UTF_8);
                valor = URLDecoder.decode(valor, StandardCharsets.UTF_8);
            } catch (IllegalArgumentException e) {
                return null;
            }
            if (campos.putIfAbsent(clave, valor) != null) {
                return null;
            }
        }
        return campos;
    }
}
