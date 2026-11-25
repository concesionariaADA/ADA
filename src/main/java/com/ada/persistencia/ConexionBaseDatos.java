package com.ada.persistencia;

import io.github.cdimascio.dotenv.Dotenv;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Opens JDBC connections to the PostgreSQL database.
 *
 * <p>Connection settings are read from the project's {@code .env} file
 * (copied from {@code .env.example}); operating-system environment variables
 * with the same name are also honored. No value is hard-coded.
 */
public final class ConexionBaseDatos {

    private static final Dotenv ENTORNO = Dotenv.configure()
            .ignoreIfMissing()
            .load();

    private ConexionBaseDatos() {
    }

    /** Returns a new connection; the caller must close it. */
    public static Connection obtenerConexion() throws SQLException {
        return DriverManager.getConnection(obtenerUrl(), leer("POSTGRES_USER"), leer("POSTGRES_PASSWORD"));
    }

    static String obtenerUrl() {
        return "jdbc:postgresql://" + leer("POSTGRES_HOST") + ":" + leer("POSTGRES_PORT")
                + "/" + leer("POSTGRES_DB");
    }

    private static String leer(String clave) {
        String valor = ENTORNO.get(clave);
        if (valor == null || valor.isBlank()) {
            throw new IllegalStateException(
                    "Missing setting " + clave + ": copy .env.example to .env and fill it in.");
        }
        return valor;
    }
}
