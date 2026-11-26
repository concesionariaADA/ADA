package com.ada.autenticacion;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AlmacenSesionesTest {

    private final RelojAjustable reloj = new RelojAjustable(Instant.parse("2026-01-01T10:00:00Z"));
    private final AlmacenSesiones sesiones = new AlmacenSesiones(reloj);

    @Test
    void tokenEsAleatorioYBase64UrlSinRelleno() {
        String uno = sesiones.crear("admin");
        String dos = sesiones.crear("admin");
        assertTrue(uno.matches("[A-Za-z0-9_-]{43}"), uno);
        assertNotEquals(uno, dos);
        assertEquals(Optional.of("admin"), sesiones.usuarioDe(uno));
        assertEquals(Optional.of("admin"), sesiones.usuarioDe(dos));
    }

    @Test
    void tokenDesconocidoONuloNoTieneSesion() {
        assertEquals(Optional.empty(), sesiones.usuarioDe(null));
        assertEquals(Optional.empty(), sesiones.usuarioDe(""));
        assertEquals(Optional.empty(), sesiones.usuarioDe("x".repeat(43)));
    }

    @Test
    void sesionVenceTras30MinutosSinUso() {
        String token = sesiones.crear("admin");
        reloj.avanzar(Duration.ofMinutes(29));
        assertEquals(Optional.of("admin"), sesiones.usuarioDe(token));
        reloj.avanzar(Duration.ofMinutes(31));
        assertEquals(Optional.empty(), sesiones.usuarioDe(token));
        // Removed on access: going back in time does not bring it back.
        reloj.avanzar(Duration.ofMinutes(-2));
        assertEquals(Optional.empty(), sesiones.usuarioDe(token));
    }

    @Test
    void cadaUsoRenuevaElVencimiento() {
        String token = sesiones.crear("admin");
        reloj.avanzar(Duration.ofMinutes(20));
        assertEquals(Optional.of("admin"), sesiones.usuarioDe(token));
        reloj.avanzar(Duration.ofMinutes(20));
        assertEquals(Optional.of("admin"), sesiones.usuarioDe(token));
    }

    @Test
    void crearDescartaSesionesVencidas() {
        String vieja = sesiones.crear("admin");
        reloj.avanzar(Duration.ofMinutes(31));
        sesiones.crear("otro");
        reloj.avanzar(Duration.ofMinutes(-2));
        assertEquals(Optional.empty(), sesiones.usuarioDe(vieja));
    }

    @Test
    void invalidarCierraLaSesion() {
        String token = sesiones.crear("admin");
        sesiones.invalidar(token);
        sesiones.invalidar(null);
        assertEquals(Optional.empty(), sesiones.usuarioDe(token));
    }

    /** Test clock that only moves when told to. */
    static final class RelojAjustable extends Clock {
        private volatile Instant ahora;

        RelojAjustable(Instant inicio) {
            this.ahora = inicio;
        }

        void avanzar(Duration duracion) {
            ahora = ahora.plus(duracion);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zona) {
            return this;
        }

        @Override
        public Instant instant() {
            return ahora;
        }
    }
}
