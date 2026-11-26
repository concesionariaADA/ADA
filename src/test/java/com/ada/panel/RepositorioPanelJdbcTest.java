package com.ada.panel;

import com.ada.persistencia.ConexionBaseDatos;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Runs the real dashboard query against the configured database. Skipped (not failed)
 * when the database is not configured or not reachable. It only reads counts.
 */
class RepositorioPanelJdbcTest {

    @Test
    void conteosRealesNoSonNegativos() throws Exception {
        assumeTrue(baseDeDatosDisponible(), "database not reachable: test skipped");
        ResumenPanel resumen = new RepositorioPanelJdbc().obtenerResumen();
        assertTrue(resumen.unidadesDisponibles() >= 0);
        assertTrue(resumen.unidadesReservadas() >= 0);
        assertTrue(resumen.unidadesVendidas() >= 0);
        assertTrue(resumen.ventasConfirmadas() >= 0);
        assertTrue(resumen.clientes() >= 0);
        assertTrue(resumen.empleados() >= 0);
    }

    private static boolean baseDeDatosDisponible() {
        try (Connection conexion = ConexionBaseDatos.obtenerConexion()) {
            return conexion.isValid(2);
        } catch (SQLException | IllegalStateException e) {
            return false;
        }
    }
}
