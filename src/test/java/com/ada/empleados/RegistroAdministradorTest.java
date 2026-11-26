package com.ada.empleados;

import com.ada.autenticacion.HashContrasena;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegistroAdministradorTest {

    /** In-memory stand-in for the database insert. */
    private static final class AltaFalsa implements AltaAdministrador {
        final List<String[]> insertados = new ArrayList<>();
        SQLException error;

        @Override
        public void insertar(String legajo, String nombre, String apellido, String usuario, String hash)
                throws SQLException {
            if (error != null) {
                throw error;
            }
            insertados.add(new String[] {legajo, nombre, apellido, usuario, hash});
        }
    }

    private final AltaFalsa alta = new AltaFalsa();
    private final RegistroAdministrador registro = new RegistroAdministrador(alta);

    @Test
    void registraAlAdministradorConLaContrasenaHasheada() throws Exception {
        char[] contrasena = "clave-123".toCharArray();
        char[] confirmacion = "clave-123".toCharArray();
        RegistroAdministrador.Resultado resultado =
                registro.registrar("L-1", "Ana", "Paz", "apaz", contrasena, confirmacion);
        assertEquals(RegistroAdministrador.Resultado.CREADO, resultado);
        assertTrue(resultado.exitoso());
        assertEquals(1, alta.insertados.size());
        String[] fila = alta.insertados.get(0);
        assertEquals(List.of("L-1", "Ana", "Paz", "apaz"), List.of(fila[0], fila[1], fila[2], fila[3]));
        assertTrue(fila[4].startsWith("pbkdf2_sha256$310000$"), fila[4]);
        assertTrue(HashContrasena.verificar("clave-123".toCharArray(), fila[4]));
        // The plain password never stays in memory after use.
        assertArrayEquals(new char[9], contrasena);
        assertArrayEquals(new char[9], confirmacion);
    }

    @Test
    void contrasenasDistintasNoRegistran() throws Exception {
        assertEquals(RegistroAdministrador.Resultado.CONTRASENAS_DISTINTAS,
                registro.registrar("L-1", "Ana", "Paz", "apaz", "clave-123".toCharArray(), "clave-124".toCharArray()));
        assertTrue(alta.insertados.isEmpty());
    }

    @Test
    void contrasenaDeMenosDe8CaracteresNoRegistra() throws Exception {
        assertEquals(RegistroAdministrador.Resultado.CONTRASENA_CORTA,
                registro.registrar("L-1", "Ana", "Paz", "apaz", "1234567".toCharArray(), "1234567".toCharArray()));
        assertTrue(alta.insertados.isEmpty());
        assertEquals(RegistroAdministrador.Resultado.CREADO,
                registro.registrar("L-1", "Ana", "Paz", "apaz", "12345678".toCharArray(), "12345678".toCharArray()));
    }

    @Test
    void datosVaciosOUsuarioConEspaciosNoRegistran() throws Exception {
        String[][] casos = {
            {"", "Ana", "Paz", "apaz"},
            {"L-1", " ", "Paz", "apaz"},
            {"L-1", "Ana", null, "apaz"},
            {"L-1", "Ana", "Paz", ""},
            {"L-1", "Ana", "Paz", "a paz"},
        };
        for (String[] caso : casos) {
            assertEquals(RegistroAdministrador.Resultado.DATOS_INVALIDOS,
                    registro.registrar(caso[0], caso[1], caso[2], caso[3],
                            "clave-123".toCharArray(), "clave-123".toCharArray()), java.util.Arrays.toString(caso));
        }
        assertTrue(alta.insertados.isEmpty());
    }

    @Test
    void legajoOUsuarioDuplicadoDaUnMensajeClaro() throws Exception {
        alta.error = new SQLException("ERROR: duplicate key value violates unique constraint \"uk_empleado_legajo\"",
                "23505");
        assertEquals(RegistroAdministrador.Resultado.LEGAJO_DUPLICADO,
                registro.registrar("L-1", "Ana", "Paz", "apaz", "clave-123".toCharArray(), "clave-123".toCharArray()));
        alta.error = new SQLException(
                "ERROR: duplicate key value violates unique constraint \"uk_empleado_nombre_usuario\"", "23505");
        assertEquals(RegistroAdministrador.Resultado.USUARIO_DUPLICADO,
                registro.registrar("L-1", "Ana", "Paz", "apaz", "clave-123".toCharArray(), "clave-123".toCharArray()));
        alta.error = new SQLException("ERROR: duplicate key", "23505");
        assertEquals(RegistroAdministrador.Resultado.DUPLICADO,
                registro.registrar("L-1", "Ana", "Paz", "apaz", "clave-123".toCharArray(), "clave-123".toCharArray()));
        assertTrue(RegistroAdministrador.Resultado.LEGAJO_DUPLICADO.mensaje().contains("legajo"));
        assertTrue(RegistroAdministrador.Resultado.USUARIO_DUPLICADO.mensaje().contains("usuario"));
        assertFalse(RegistroAdministrador.Resultado.DUPLICADO.exitoso());
    }

    @Test
    void otroErrorDeBaseDeDatosSePropaga() {
        alta.error = new SQLException("connection refused", "08001");
        assertThrows(SQLException.class, () -> registro.registrar(
                "L-1", "Ana", "Paz", "apaz", "clave-123".toCharArray(), "clave-123".toCharArray()));
    }

    // --- Console command (no real console in tests) ---

    @Test
    void comandoSinConsolaPideUsarUnaTerminal() {
        ByteArrayOutputStream errores = new ByteArrayOutputStream();
        int codigo = CrearAdministrador.ejecutar(new String[] {"L-1", "Ana", "Paz", "apaz"}, null, alta,
                new PrintStream(new ByteArrayOutputStream(), true, StandardCharsets.UTF_8),
                new PrintStream(errores, true, StandardCharsets.UTF_8));
        assertEquals(1, codigo);
        assertTrue(errores.toString(StandardCharsets.UTF_8).contains("terminal"), errores.toString(StandardCharsets.UTF_8));
        assertTrue(alta.insertados.isEmpty());
    }

    @Test
    void comandoConArgumentosIncorrectosMuestraElUso() {
        ByteArrayOutputStream errores = new ByteArrayOutputStream();
        int codigo = CrearAdministrador.ejecutar(new String[] {"L-1"}, mensaje -> "clave-123".toCharArray(), alta,
                new PrintStream(new ByteArrayOutputStream(), true, StandardCharsets.UTF_8),
                new PrintStream(errores, true, StandardCharsets.UTF_8));
        assertEquals(1, codigo);
        assertTrue(errores.toString(StandardCharsets.UTF_8).contains("Uso:"), errores.toString(StandardCharsets.UTF_8));
        assertTrue(alta.insertados.isEmpty());
    }

    @Test
    void comandoCreaElAdministradorYMuestraElResultado() {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        int codigo = CrearAdministrador.ejecutar(new String[] {"L-1", "Ana", "Paz", "apaz"},
                mensaje -> "clave-123".toCharArray(), alta,
                new PrintStream(salida, true, StandardCharsets.UTF_8),
                new PrintStream(new ByteArrayOutputStream(), true, StandardCharsets.UTF_8));
        assertEquals(0, codigo);
        assertEquals(1, alta.insertados.size());
        assertTrue(salida.toString(StandardCharsets.UTF_8).contains(RegistroAdministrador.Resultado.CREADO.mensaje()));
    }

    @Test
    void comandoInformaElDuplicadoSinTrazaDePila() {
        alta.error = new SQLException("duplicate key value violates unique constraint \"uk_empleado_legajo\"", "23505");
        ByteArrayOutputStream errores = new ByteArrayOutputStream();
        int codigo = CrearAdministrador.ejecutar(new String[] {"L-1", "Ana", "Paz", "apaz"},
                mensaje -> "clave-123".toCharArray(), alta,
                new PrintStream(new ByteArrayOutputStream(), true, StandardCharsets.UTF_8),
                new PrintStream(errores, true, StandardCharsets.UTF_8));
        assertEquals(1, codigo);
        String texto = errores.toString(StandardCharsets.UTF_8);
        assertTrue(texto.contains(RegistroAdministrador.Resultado.LEGAJO_DUPLICADO.mensaje()), texto);
        assertFalse(texto.contains("SQLException"), texto);
        assertFalse(texto.contains("\tat "), texto);
    }

    @Test
    void comandoInformaErrorDeConexionSinTrazaDePila() {
        alta.error = new SQLException("connection refused", "08001");
        ByteArrayOutputStream errores = new ByteArrayOutputStream();
        int codigo = CrearAdministrador.ejecutar(new String[] {"L-1", "Ana", "Paz", "apaz"},
                mensaje -> "clave-123".toCharArray(), alta,
                new PrintStream(new ByteArrayOutputStream(), true, StandardCharsets.UTF_8),
                new PrintStream(errores, true, StandardCharsets.UTF_8));
        assertEquals(1, codigo);
        String texto = errores.toString(StandardCharsets.UTF_8);
        assertTrue(texto.contains("08001"), texto);
        assertFalse(texto.contains("\tat "), texto);
    }
}
