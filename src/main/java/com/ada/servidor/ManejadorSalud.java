package com.ada.servidor;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;

/** {@code GET /api/salud}: tells the frontend the backend is up. */
final class ManejadorSalud implements HttpHandler {

    @Override
    public void handle(HttpExchange intercambio) throws IOException {
        try (intercambio) {
            if (!"/api/salud".equals(intercambio.getRequestURI().getPath())) {
                Respuestas.enviarJson(intercambio, 404, "{\"error\":\"no encontrado\"}");
            } else if (!"GET".equals(intercambio.getRequestMethod())) {
                intercambio.getResponseHeaders().set("Allow", "GET");
                Respuestas.enviarJson(intercambio, 405, "{\"error\":\"metodo no permitido\"}");
            } else {
                Respuestas.enviarJson(intercambio, 200, "{\"estado\":\"ok\"}");
            }
        }
    }
}
