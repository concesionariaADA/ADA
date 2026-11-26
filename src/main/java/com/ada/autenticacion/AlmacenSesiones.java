package com.ada.autenticacion;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Admin sessions kept in server memory (they are lost on restart).
 *
 * <p>Each session is identified by a token of 32 {@link SecureRandom} bytes encoded as
 * base64url without padding. A session expires after {@link #INACTIVIDAD_MAXIMA} without
 * use; every successful lookup renews it (sliding expiry). Expired sessions are removed
 * when they are looked up and whenever a new session is created.
 */
public final class AlmacenSesiones {

    /** Idle time after which a session is no longer valid. */
    public static final Duration INACTIVIDAD_MAXIMA = Duration.ofMinutes(30);

    private static final int BYTES_TOKEN = 32;
    private static final SecureRandom ALEATORIO = new SecureRandom();
    private static final Base64.Encoder BASE64_URL = Base64.getUrlEncoder().withoutPadding();

    private record Sesion(String usuario, Instant ultimoUso) {
    }

    private final Map<String, Sesion> sesiones = new ConcurrentHashMap<>();
    private final Clock reloj;

    /** @param reloj time source; tests pass an adjustable clock */
    public AlmacenSesiones(Clock reloj) {
        this.reloj = reloj;
    }

    /** Opens a session for {@code usuario} and returns its token. */
    public String crear(String usuario) {
        Instant ahora = reloj.instant();
        sesiones.values().removeIf(sesion -> vencida(sesion, ahora));
        while (true) {
            byte[] bytes = new byte[BYTES_TOKEN];
            ALEATORIO.nextBytes(bytes);
            String token = BASE64_URL.encodeToString(bytes);
            if (sesiones.putIfAbsent(token, new Sesion(usuario, ahora)) == null) {
                return token;
            }
        }
    }

    /**
     * Returns the user of a live session and renews it; an expired session is removed
     * and yields empty, as does an unknown or null token.
     */
    public Optional<String> usuarioDe(String token) {
        if (token == null || token.isEmpty()) {
            return Optional.empty();
        }
        Instant ahora = reloj.instant();
        Sesion vigente = sesiones.computeIfPresent(token,
                (clave, sesion) -> vencida(sesion, ahora) ? null : new Sesion(sesion.usuario(), ahora));
        return vigente == null ? Optional.empty() : Optional.of(vigente.usuario());
    }

    /** Closes the session; does nothing for an unknown or null token. */
    public void invalidar(String token) {
        if (token != null) {
            sesiones.remove(token);
        }
    }

    private static boolean vencida(Sesion sesion, Instant ahora) {
        return !ahora.isBefore(sesion.ultimoUso().plus(INACTIVIDAD_MAXIMA));
    }
}
