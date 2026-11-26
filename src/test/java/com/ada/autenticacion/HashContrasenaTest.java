package com.ada.autenticacion;

import org.junit.jupiter.api.Test;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HashContrasenaTest {

    private static final String CLAVE = "clave-secreta-1";
    private static final String ALMACENADO = HashContrasena.generar(CLAVE.toCharArray());

    @Test
    void contrasenaCorrectaVerifica() {
        assertTrue(HashContrasena.verificar(CLAVE.toCharArray(), ALMACENADO));
    }

    @Test
    void contrasenaIncorrectaNoVerifica() {
        assertFalse(HashContrasena.verificar("clave-secreta-2".toCharArray(), ALMACENADO));
        assertFalse(HashContrasena.verificar("".toCharArray(), ALMACENADO));
        assertFalse(HashContrasena.verificar(null, ALMACENADO));
    }

    @Test
    void dosHashesDeLaMismaContrasenaDifierenPorElSalt() {
        String otro = HashContrasena.generar(CLAVE.toCharArray());
        assertNotEquals(ALMACENADO, otro);
        assertTrue(HashContrasena.verificar(CLAVE.toCharArray(), otro));
    }

    @Test
    void formatoAlmacenadoEsElEsperado() {
        assertTrue(ALMACENADO.matches(
                "pbkdf2_sha256\\$310000\\$[A-Za-z0-9+/]{22}==\\$[A-Za-z0-9+/]{43}="), ALMACENADO);
    }

    @Test
    void lasIteracionesSeLeenDelValorAlmacenado() throws Exception {
        byte[] salt = new byte[16];
        for (int i = 0; i < salt.length; i++) {
            salt[i] = (byte) i;
        }
        PBEKeySpec spec = new PBEKeySpec(CLAVE.toCharArray(), salt, 1000, 256);
        byte[] hash = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        String almacenado = "pbkdf2_sha256$1000$" + Base64.getEncoder().encodeToString(salt)
                + "$" + Base64.getEncoder().encodeToString(hash);
        assertTrue(HashContrasena.verificar(CLAVE.toCharArray(), almacenado));
        assertFalse(HashContrasena.verificar(CLAVE.toCharArray(), almacenado.replace("$1000$", "$1001$")));
    }

    @Test
    void valorAlmacenadoMalformadoDevuelveFalse() {
        String[] partes = ALMACENADO.split("\\$");
        String[] malformados = {
            null,
            "",
            "texto-plano",
            "pbkdf2_sha256$310000$" + partes[2],
            "pbkdf2_sha1$310000$" + partes[2] + "$" + partes[3],
            "pbkdf2_sha256$abc$" + partes[2] + "$" + partes[3],
            "pbkdf2_sha256$0$" + partes[2] + "$" + partes[3],
            "pbkdf2_sha256$-5$" + partes[2] + "$" + partes[3],
            "pbkdf2_sha256$999999999999$" + partes[2] + "$" + partes[3],
            "pbkdf2_sha256$310000$%%%$" + partes[3],
            "pbkdf2_sha256$310000$" + partes[2] + "$%%%",
            "pbkdf2_sha256$310000$$" + partes[3],
            "pbkdf2_sha256$310000$" + partes[2] + "$" + Base64.getEncoder().encodeToString(new byte[8]),
            ALMACENADO + "$extra",
        };
        for (String malformado : malformados) {
            assertFalse(HashContrasena.verificar(CLAVE.toCharArray(), malformado), String.valueOf(malformado));
        }
    }
}
