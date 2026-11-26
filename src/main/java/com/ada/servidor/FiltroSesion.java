package com.ada.servidor;

import com.ada.autenticacion.ServicioAutenticacion;
import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.util.Set;

/**
 * Admin-area guard (Issue #3, RN19): no Backoffice operation without a valid session.
 * Register it with {@link ServidorWeb#agregarFiltroAdmin}.
 *
 * <p>Only the login page, the login endpoint and the logout endpoint are reachable
 * without a session. Any other request without a valid session is refused: pages under
 * {@code /admin} are redirected (302) to {@code /admin/login} and calls under
 * {@code /api/admin} get 401 JSON. Paths are compared exactly, so anything unknown is
 * refused by default.
 */
public final class FiltroSesion extends Filter {

    /** Request attribute holding the user name of the session, for the admin handlers. */
    static final String ATRIBUTO_USUARIO = "ada.usuario";

    private static final String PAGINA_LOGIN = "/admin/login";
    private static final Set<String> RUTAS_LIBRES = Set.of(
            PAGINA_LOGIN, ManejadorApiAdmin.RUTA_LOGIN, ManejadorApiAdmin.RUTA_LOGOUT);

    private final ServicioAutenticacion autenticacion;

    public FiltroSesion(ServicioAutenticacion autenticacion) {
        this.autenticacion = autenticacion;
    }

    @Override
    public void doFilter(HttpExchange intercambio, Chain cadena) throws IOException {
        // Admin responses must not stay in the browser cache after logout.
        intercambio.getResponseHeaders().set("Cache-Control", "no-store");
        String ruta = intercambio.getRequestURI().getPath();
        if (ruta != null && RUTAS_LIBRES.contains(ruta)) {
            cadena.doFilter(intercambio);
            return;
        }
        String usuario = CookieSesion.leer(intercambio)
                .flatMap(autenticacion::usuarioDeSesion)
                .orElse(null);
        if (usuario != null) {
            intercambio.setAttribute(ATRIBUTO_USUARIO, usuario);
            cadena.doFilter(intercambio);
            return;
        }
        try (intercambio) {
            if (ruta != null && ruta.startsWith("/api/")) {
                Respuestas.noAutorizado(intercambio);
            } else {
                intercambio.getResponseHeaders().set("Location", PAGINA_LOGIN);
                Respuestas.enviar(intercambio, 302, Respuestas.TIPO_TEXTO, new byte[0]);
            }
        }
    }

    @Override
    public String description() {
        return "Requires a valid admin session (cookie " + CookieSesion.NOMBRE + ")";
    }
}
