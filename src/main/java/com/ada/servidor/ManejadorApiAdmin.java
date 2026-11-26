package com.ada.servidor;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;

/**
 * Placeholder for the admin API ({@code /api/admin/**}). It owns its own context so
 * the authentication filter covers it; it has no endpoints yet and answers 404.
 */
final class ManejadorApiAdmin implements HttpHandler {

    @Override
    public void handle(HttpExchange intercambio) throws IOException {
        try (intercambio) {
            Respuestas.enviarJson(intercambio, 404, "{\"error\":\"no encontrado\"}");
        }
    }
}
