package com.ada.servidor;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.nio.file.Path;
import java.util.regex.Pattern;

/**
 * Admin pages: {@code /admin} serves {@code templates/admin/index.html} and
 * {@code /admin/<name>} serves {@code templates/admin/<name>.html}. Only simple names
 * (letters, digits, {@code -} and {@code _}) are accepted.
 *
 * <p>This handler does not check sessions: authentication is added as a filter on its
 * context (see {@link ServidorWeb#agregarFiltroAdmin}).
 */
final class ManejadorAdmin implements HttpHandler {

    private static final String PREFIJO = "/admin/";
    private static final Pattern NOMBRE_PAGINA = Pattern.compile("[A-Za-z0-9_-]+");

    private final Path carpetaAdmin;

    ManejadorAdmin(Path directorioFrontend) {
        this.carpetaAdmin = directorioFrontend.resolve("templates").resolve("admin");
    }

    @Override
    public void handle(HttpExchange intercambio) throws IOException {
        try (intercambio) {
            if (!ArchivosEstaticos.esMetodoDeLectura(intercambio)) {
                Respuestas.metodoNoPermitido(intercambio, ArchivosEstaticos.METODOS_PERMITIDOS);
                return;
            }
            String ruta = intercambio.getRequestURI().getPath();
            String nombre;
            if ("/admin".equals(ruta) || PREFIJO.equals(ruta)) {
                nombre = "index";
            } else if (ruta.startsWith(PREFIJO)) {
                nombre = ruta.substring(PREFIJO.length());
            } else {
                // e.g. "/administrar": the JDK matches contexts by plain prefix.
                Respuestas.noEncontrado(intercambio);
                return;
            }
            if (!NOMBRE_PAGINA.matcher(nombre).matches()) {
                Respuestas.noEncontrado(intercambio);
                return;
            }
            ArchivosEstaticos.servir(intercambio, carpetaAdmin, nombre + ".html");
        }
    }
}
