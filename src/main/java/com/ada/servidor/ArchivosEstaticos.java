package com.ada.servidor;

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;

/**
 * Serves files from a fixed folder without ever leaving it.
 *
 * <p>The relative path comes from the already decoded URL, so encoded tricks such as
 * {@code %2e%2e} or {@code %2f} arrive here as plain {@code ..} or {@code /} and are
 * rejected like any other traversal attempt.
 */
final class ArchivosEstaticos {

    static final String METODOS_PERMITIDOS = "GET, HEAD";

    private static final Map<String, String> TIPOS = Map.of(
            "html", "text/html; charset=utf-8",
            "css", "text/css; charset=utf-8",
            "js", "text/javascript; charset=utf-8",
            "svg", "image/svg+xml; charset=utf-8",
            "png", "image/png",
            "jpg", "image/jpeg",
            "jpeg", "image/jpeg",
            "ico", "image/x-icon");
    private static final String TIPO_POR_DEFECTO = "application/octet-stream";

    private ArchivosEstaticos() {
    }

    /** True for the only methods static files accept. */
    static boolean esMetodoDeLectura(HttpExchange intercambio) {
        String metodo = intercambio.getRequestMethod();
        return "GET".equals(metodo) || "HEAD".equals(metodo);
    }

    /**
     * Sends {@code carpeta/rutaRelativa}, or 404 when the path is unsafe, escapes
     * {@code carpeta} or does not name a regular file.
     */
    static void servir(HttpExchange intercambio, Path carpeta, String rutaRelativa) throws IOException {
        Path archivo = resolverDentro(carpeta, rutaRelativa);
        if (archivo == null) {
            Respuestas.noEncontrado(intercambio);
            return;
        }
        Respuestas.enviar(intercambio, 200, tipoDeContenido(archivo), Files.readAllBytes(archivo));
    }

    /** Returns the file inside {@code carpeta}, or null when it is unsafe or missing. */
    static Path resolverDentro(Path carpeta, String rutaRelativa) {
        if (!esRutaRelativaSegura(rutaRelativa)) {
            return null;
        }
        try {
            Path base = carpeta.toAbsolutePath().normalize();
            Path candidato = base.resolve(rutaRelativa).normalize();
            if (!candidato.startsWith(base) || !Files.isRegularFile(candidato)) {
                return null;
            }
            // Also follow links: the real file must stay inside the real folder.
            Path real = candidato.toRealPath();
            return real.startsWith(base.toRealPath()) ? real : null;
        } catch (InvalidPathException | IOException e) {
            return null;
        }
    }

    /**
     * Accepts only plain relative paths: no backslashes, drive letters, NUL, leading
     * slash, empty segments, {@code .} or {@code ..}.
     */
    static boolean esRutaRelativaSegura(String ruta) {
        if (ruta == null || ruta.isEmpty() || ruta.startsWith("/")
                || ruta.indexOf('\\') >= 0 || ruta.indexOf(':') >= 0 || ruta.indexOf('\0') >= 0) {
            return false;
        }
        for (String segmento : ruta.split("/", -1)) {
            if (segmento.isEmpty() || ".".equals(segmento) || "..".equals(segmento)) {
                return false;
            }
        }
        return true;
    }

    static String tipoDeContenido(Path archivo) {
        String nombre = archivo.getFileName().toString();
        int punto = nombre.lastIndexOf('.');
        if (punto < 0) {
            return TIPO_POR_DEFECTO;
        }
        String extension = nombre.substring(punto + 1).toLowerCase(Locale.ROOT);
        return TIPOS.getOrDefault(extension, TIPO_POR_DEFECTO);
    }
}
