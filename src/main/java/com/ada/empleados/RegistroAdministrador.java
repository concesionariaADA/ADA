package com.ada.empleados;

import com.ada.autenticacion.HashContrasena;

import java.sql.SQLException;
import java.util.Arrays;

/**
 * Rules for creating an administrator (RN04, RN19), independent of the console and of
 * the database: required data, password confirmation and minimum length, PBKDF2 hash,
 * and clear results for duplicate legajo or user name.
 */
public final class RegistroAdministrador {

    /** Minimum password length, in characters. */
    public static final int LONGITUD_MINIMA = 8;

    /** SQLState of a unique-constraint violation. */
    private static final String VIOLACION_UNICA = "23505";

    /** Outcome of {@link #registrar}, with the message shown to the operator. */
    public enum Resultado {
        CREADO("Administrador creado."),
        DATOS_INVALIDOS("Legajo, nombre, apellido y usuario son obligatorios; el usuario no puede tener espacios."),
        CONTRASENAS_DISTINTAS("Las contraseñas no coinciden."),
        CONTRASENA_CORTA("La contraseña debe tener al menos " + LONGITUD_MINIMA + " caracteres."),
        LEGAJO_DUPLICADO("Ya existe un empleado con ese legajo."),
        USUARIO_DUPLICADO("Ya existe un empleado con ese nombre de usuario."),
        DUPLICADO("Ya existe un empleado con ese legajo o nombre de usuario.");

        private final String mensaje;

        Resultado(String mensaje) {
            this.mensaje = mensaje;
        }

        public String mensaje() {
            return mensaje;
        }

        public boolean exitoso() {
            return this == CREADO;
        }
    }

    private final AltaAdministrador alta;

    public RegistroAdministrador(AltaAdministrador alta) {
        this.alta = alta;
    }

    /**
     * Validates the data and inserts the administrator. Both password arrays are wiped
     * before returning, whatever the outcome.
     *
     * @throws SQLException on database errors other than a duplicate legajo or user
     */
    public Resultado registrar(String legajo, String nombre, String apellido, String usuario,
                               char[] contrasena, char[] confirmacion) throws SQLException {
        try {
            if (vacio(legajo) || vacio(nombre) || vacio(apellido) || vacio(usuario)
                    || usuario.codePoints().anyMatch(Character::isWhitespace)) {
                return Resultado.DATOS_INVALIDOS;
            }
            if (contrasena == null || confirmacion == null || !Arrays.equals(contrasena, confirmacion)) {
                return Resultado.CONTRASENAS_DISTINTAS;
            }
            if (contrasena.length < LONGITUD_MINIMA) {
                return Resultado.CONTRASENA_CORTA;
            }
            String hash = HashContrasena.generar(contrasena);
            try {
                alta.insertar(legajo.trim(), nombre.trim(), apellido.trim(), usuario, hash);
            } catch (SQLException e) {
                if (!VIOLACION_UNICA.equals(e.getSQLState())) {
                    throw e;
                }
                return duplicado(e.getMessage());
            }
            return Resultado.CREADO;
        } finally {
            borrar(contrasena);
            borrar(confirmacion);
        }
    }

    /** Tells which unique constraint was violated, by its name in the schema. */
    private static Resultado duplicado(String mensaje) {
        if (mensaje != null && mensaje.contains("uk_empleado_legajo")) {
            return Resultado.LEGAJO_DUPLICADO;
        }
        if (mensaje != null && mensaje.contains("uk_empleado_nombre_usuario")) {
            return Resultado.USUARIO_DUPLICADO;
        }
        return Resultado.DUPLICADO;
    }

    private static boolean vacio(String valor) {
        return valor == null || valor.isBlank();
    }

    private static void borrar(char[] valor) {
        if (valor != null) {
            Arrays.fill(valor, '\0');
        }
    }
}
