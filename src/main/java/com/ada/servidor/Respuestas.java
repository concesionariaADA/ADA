package com.ada.servidor;

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/** Small helpers to write HTTP responses; JSON is written by hand (no libraries). */
final class Respuestas {

    static final String TIPO_JSON = "application/json; charset=utf-8";
    static final String TIPO_TEXTO = "text/plain; charset=utf-8";

    private Respuestas() {
    }

    /** Sends a full response; for HEAD requests only the headers are sent. */
    static void enviar(HttpExchange intercambio, int codigo, String tipoContenido, byte[] cuerpo)
            throws IOException {
        intercambio.getResponseHeaders().set("Content-Type", tipoContenido);
        intercambio.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        if ("HEAD".equals(intercambio.getRequestMethod()) || cuerpo.length == 0) {
            intercambio.sendResponseHeaders(codigo, -1);
            return;
        }
        intercambio.sendResponseHeaders(codigo, cuerpo.length);
        try (OutputStream salida = intercambio.getResponseBody()) {
            salida.write(cuerpo);
        }
    }

    static void enviarJson(HttpExchange intercambio, int codigo, String json) throws IOException {
        enviar(intercambio, codigo, TIPO_JSON, json.getBytes(StandardCharsets.UTF_8));
    }

    static void noEncontrado(HttpExchange intercambio) throws IOException {
        enviar(intercambio, 404, TIPO_TEXTO, "No encontrado".getBytes(StandardCharsets.UTF_8));
    }

    /** 401 for admin API calls without a valid session. */
    static void noAutorizado(HttpExchange intercambio) throws IOException {
        enviarJson(intercambio, 401, "{\"error\":\"no autorizado\"}");
    }

    /**
     * Quotes {@code valor} as a JSON string. Besides the mandatory escapes, {@code <},
     * {@code >}, {@code &} and the JavaScript line separators are escaped too, so the
     * value is also safe if a page ever embeds it.
     */
    static String textoJson(String valor) {
        StringBuilder texto = new StringBuilder(valor.length() + 2).append('"');
        for (int i = 0; i < valor.length(); i++) {
            char c = valor.charAt(i);
            switch (c) {
                case '"' -> texto.append("\\\"");
                case '\\' -> texto.append("\\\\");
                case '\n' -> texto.append("\\n");
                case '\r' -> texto.append("\\r");
                case '\t' -> texto.append("\\t");
                default -> {
                    if (c < 0x20 || c == '<' || c == '>' || c == '&' || c == '\u2028' || c == '\u2029') {
                        texto.append(String.format("\\u%04x", (int) c));
                    } else {
                        texto.append(c);
                    }
                }
            }
        }
        return texto.append('"').toString();
    }

    /** 405 with the {@code Allow} header listing the accepted methods. */
    static void metodoNoPermitido(HttpExchange intercambio, String permitidos) throws IOException {
        intercambio.getResponseHeaders().set("Allow", permitidos);
        enviar(intercambio, 405, TIPO_TEXTO, "Método no permitido".getBytes(StandardCharsets.UTF_8));
    }
}
