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

    /** 405 with the {@code Allow} header listing the accepted methods. */
    static void metodoNoPermitido(HttpExchange intercambio, String permitidos) throws IOException {
        intercambio.getResponseHeaders().set("Allow", permitidos);
        enviar(intercambio, 405, TIPO_TEXTO, "Método no permitido".getBytes(StandardCharsets.UTF_8));
    }
}
