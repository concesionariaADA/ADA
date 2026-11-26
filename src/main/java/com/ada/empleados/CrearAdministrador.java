package com.ada.empleados;

import java.io.Console;
import java.io.PrintStream;
import java.sql.SQLException;

/**
 * Console command that creates an administrator (an ADMINISTRATIVA employee), needed
 * for the first {@code /admin} login until the employees screen exists (#4).
 *
 * <p>Usage: {@code CrearAdministrador <legajo> <nombre> <apellido> <usuario>}. The
 * password is typed twice without echo, so it never appears in the arguments, the shell
 * history or the output.
 */
public final class CrearAdministrador {

    /** Reads a password without echo; {@link Console#readPassword} in production. */
    @FunctionalInterface
    interface LectorContrasena {
        char[] leer(String mensaje);
    }

    private static final String USO = "Uso: CrearAdministrador <legajo> <nombre> <apellido> <usuario>";

    private CrearAdministrador() {
    }

    public static void main(String[] args) {
        Console consola = System.console();
        LectorContrasena lector = consola == null ? null : mensaje -> consola.readPassword("%s", mensaje);
        System.exit(ejecutar(args, lector, new AltaAdministradorJdbc(), System.out, System.err));
    }

    /**
     * Runs the command and returns the exit code: 0 when the administrator was created,
     * 1 otherwise. Errors are reported as plain messages, never as stack traces.
     *
     * @param lector password reader, or null when there is no interactive terminal
     */
    static int ejecutar(String[] args, LectorContrasena lector, AltaAdministrador alta,
                        PrintStream salida, PrintStream errores) {
        if (args == null || args.length != 4) {
            errores.println(USO);
            return 1;
        }
        if (lector == null) {
            errores.println("No hay una terminal interactiva para leer la contraseña."
                    + " Ejecutá este comando desde una terminal (no desde el IDE ni con la entrada redirigida).");
            return 1;
        }
        char[] contrasena = lector.leer("Contraseña: ");
        char[] confirmacion = lector.leer("Repetí la contraseña: ");
        try {
            RegistroAdministrador.Resultado resultado = new RegistroAdministrador(alta)
                    .registrar(args[0], args[1], args[2], args[3], contrasena, confirmacion);
            if (resultado.exitoso()) {
                salida.println(resultado.mensaje());
                return 0;
            }
            errores.println(resultado.mensaje());
            return 1;
        } catch (SQLException e) {
            errores.println("No se pudo guardar el administrador en la base de datos (SQLState "
                    + e.getSQLState() + "). Verificá que la base esté levantada y el .env configurado.");
            return 1;
        } catch (IllegalStateException e) {
            // ConexionBaseDatos reports a missing .env setting this way.
            errores.println(e.getMessage());
            return 1;
        }
    }
}
