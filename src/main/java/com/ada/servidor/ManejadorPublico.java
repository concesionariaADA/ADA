package com.ada.servidor;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Public area: {@code /} is the frontoffice home and {@code /css/**}, {@code /js/**}
 * and {@code /img/**} map to the same folders under {@code frontend/}. Each prefix is
 * confined to its own folder, so the public area can never reach the admin pages.
 */
final class ManejadorPublico implements HttpHandler {

    private static final List<String> CARPETAS = List.of("css", "js", "img");

    private final Path directorioFrontend;

    ManejadorPublico(Path directorioFrontend) {
        this.directorioFrontend = directorioFrontend;
    }

    @Override
    public void handle(HttpExchange intercambio) throws IOException {
        try (intercambio) {
            if (!ArchivosEstaticos.esMetodoDeLectura(intercambio)) {
                Respuestas.metodoNoPermitido(intercambio, ArchivosEstaticos.METODOS_PERMITIDOS);
                return;
            }
            String ruta = intercambio.getRequestURI().getPath();
            if ("/".equals(ruta)) {
                ArchivosEstaticos.servir(intercambio,
                        directorioFrontend.resolve("templates").resolve("frontoffice"), "index.html");
                return;
            }
            for (String carpeta : CARPETAS) {
                String prefijo = "/" + carpeta + "/";
                if (ruta.startsWith(prefijo)) {
                    ArchivosEstaticos.servir(intercambio, directorioFrontend.resolve(carpeta),
                            ruta.substring(prefijo.length()));
                    return;
                }
            }
            Respuestas.noEncontrado(intercambio);
        }
    }
}
