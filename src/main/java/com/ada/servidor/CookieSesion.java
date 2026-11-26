package com.ada.servidor;

import com.sun.net.httpserver.HttpExchange;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/** Reads and writes the admin session cookie {@value #NOMBRE}. */
final class CookieSesion {

    static final String NOMBRE = "ADA_SESION";

    /** Session tokens are 32 random bytes in base64url without padding: 43 characters. */
    private static final Pattern FORMATO_TOKEN = Pattern.compile("[A-Za-z0-9_-]{43}");
    private static final String ATRIBUTOS = "; Path=/; HttpOnly; SameSite=Strict";

    private CookieSesion() {
    }

    /**
     * Token sent by the browser, or empty when the cookie is missing or does not even
     * look like a token (such values are never looked up).
     */
    static Optional<String> leer(HttpExchange intercambio) {
        List<String> cabeceras = intercambio.getRequestHeaders().get("Cookie");
        if (cabeceras == null) {
            return Optional.empty();
        }
        for (String cabecera : cabeceras) {
            for (String par : cabecera.split(";")) {
                int igual = par.indexOf('=');
                if (igual > 0 && NOMBRE.equals(par.substring(0, igual).trim())) {
                    String valor = par.substring(igual + 1).trim();
                    return FORMATO_TOKEN.matcher(valor).matches() ? Optional.of(valor) : Optional.empty();
                }
            }
        }
        return Optional.empty();
    }

    /** Sets the session cookie: not readable from JavaScript and never sent cross-site. */
    static void escribir(HttpExchange intercambio, String token) {
        intercambio.getResponseHeaders().set("Set-Cookie", NOMBRE + "=" + token + ATRIBUTOS);
    }

    /** Tells the browser to delete the session cookie. */
    static void borrar(HttpExchange intercambio) {
        intercambio.getResponseHeaders().set("Set-Cookie", NOMBRE + "=; Path=/; Max-Age=0; HttpOnly; SameSite=Strict");
    }
}
