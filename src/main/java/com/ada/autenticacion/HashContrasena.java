package com.ada.autenticacion;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Password hashing with the JDK's {@code PBKDF2WithHmacSHA256} (no libraries, D03b).
 *
 * <p>Stored format: {@code pbkdf2_sha256$<iterations>$<salt base64>$<hash base64>}.
 * Passwords are never stored in plain text (Issue #3). Verification reads the iteration
 * count from the stored value, so the cost can be raised later without breaking old
 * hashes, and compares in constant time. A malformed stored value never verifies and
 * never throws.
 */
public final class HashContrasena {

    /** Iterations for new hashes (OWASP recommendation for PBKDF2-HMAC-SHA256). */
    public static final int ITERACIONES = 310_000;

    private static final String ALGORITMO = "PBKDF2WithHmacSHA256";
    private static final String PREFIJO = "pbkdf2_sha256";
    private static final int BYTES_SALT = 16;
    private static final int BYTES_HASH = 32;
    /** Upper bound accepted when verifying, so a corrupt value cannot stall a request. */
    private static final int ITERACIONES_MAXIMAS = 10_000_000;
    private static final SecureRandom ALEATORIO = new SecureRandom();

    private HashContrasena() {
    }

    /** Hashes {@code contrasena} with a fresh random salt. The array is not modified. */
    public static String generar(char[] contrasena) {
        if (contrasena == null || contrasena.length == 0) {
            throw new IllegalArgumentException("The password must not be empty.");
        }
        byte[] salt = new byte[BYTES_SALT];
        ALEATORIO.nextBytes(salt);
        byte[] hash = derivar(contrasena, salt, ITERACIONES);
        Base64.Encoder base64 = Base64.getEncoder();
        return PREFIJO + "$" + ITERACIONES + "$" + base64.encodeToString(salt) + "$" + base64.encodeToString(hash);
    }

    /**
     * True only when {@code contrasena} matches {@code almacenado}. Any malformed or
     * missing input yields false.
     */
    public static boolean verificar(char[] contrasena, String almacenado) {
        if (contrasena == null || contrasena.length == 0 || almacenado == null) {
            return false;
        }
        String[] partes = almacenado.split("\\$", -1);
        if (partes.length != 4 || !PREFIJO.equals(partes[0])) {
            return false;
        }
        try {
            int iteraciones = Integer.parseInt(partes[1]);
            byte[] salt = Base64.getDecoder().decode(partes[2]);
            byte[] esperado = Base64.getDecoder().decode(partes[3]);
            if (iteraciones < 1 || iteraciones > ITERACIONES_MAXIMAS
                    || salt.length == 0 || esperado.length != BYTES_HASH) {
                return false;
            }
            return MessageDigest.isEqual(esperado, derivar(contrasena, salt, iteraciones));
        } catch (IllegalArgumentException e) {
            // Bad number or bad base64 (NumberFormatException is an IllegalArgumentException).
            return false;
        }
    }

    private static byte[] derivar(char[] contrasena, byte[] salt, int iteraciones) {
        PBEKeySpec especificacion = new PBEKeySpec(contrasena, salt, iteraciones, BYTES_HASH * 8);
        try {
            return SecretKeyFactory.getInstance(ALGORITMO).generateSecret(especificacion).getEncoded();
        } catch (GeneralSecurityException e) {
            // PBKDF2WithHmacSHA256 is mandatory in every Java 8+ runtime.
            throw new IllegalStateException("PBKDF2 is not available in this Java runtime.", e);
        } finally {
            especificacion.clearPassword();
        }
    }
}
