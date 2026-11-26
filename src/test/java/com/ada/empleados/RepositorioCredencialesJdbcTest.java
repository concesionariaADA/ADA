package com.ada.empleados;

import com.ada.persistencia.ConexionBaseDatos;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Runs the real JDBC lookup against the configured database. Skipped (not failed) when
 * the database is not configured or not reachable. It never writes any data.
 */
class RepositorioCredencialesJdbcTest {

    @Test
    void usuarioInexistenteDevuelveVacio() throws Exception {
        assumeTrue(baseDeDatosDisponible(), "database not reachable: test skipped");
        String usuario = "no-existe-" + UUID.randomUUID();
        assertTrue(new RepositorioCredencialesJdbc().buscarAdministrativoActivo(usuario).isEmpty());
    }

    private static boolean baseDeDatosDisponible() {
        try (Connection conexion = ConexionBaseDatos.obtenerConexion()) {
            return conexion.isValid(2);
        } catch (SQLException | IllegalStateException e) {
            return false;
        }
    }
}
