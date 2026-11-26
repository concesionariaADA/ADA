package com.ada.catalogo;

import com.ada.persistencia.ConexionBaseDatos;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Runs the real brand query against the configured database. Skipped (not failed)
 * when the database is not configured or not reachable. It only reads.
 */
class RepositorioMarcasJdbcTest {

    private static final List<String> TIPOS = List.of("AUTOMOVIL", "CAMIONETA", "MOTOCICLETA");

    @Test
    void cadaMarcaApareceUnaVezConTiposValidosYOrdenados() throws Exception {
        assumeTrue(baseDeDatosDisponible(), "database not reachable: test skipped");
        List<Marca> marcas = new RepositorioMarcasJdbc().listar();
        Set<Integer> ids = new HashSet<>();
        for (Marca marca : marcas) {
            assertTrue(ids.add(marca.id()), "repeated brand id " + marca.id());
            assertFalse(marca.nombre().isBlank(), "blank name for brand " + marca.id());
            assertTrue(TIPOS.containsAll(marca.tipos()), marca.nombre() + ": " + marca.tipos());
            List<String> ordenados = TIPOS.stream().filter(marca.tipos()::contains).toList();
            assertEquals(ordenados, marca.tipos(), marca.nombre());
        }
    }

    private static boolean baseDeDatosDisponible() {
        try (Connection conexion = ConexionBaseDatos.obtenerConexion()) {
            return conexion.isValid(2);
        } catch (SQLException | IllegalStateException e) {
            return false;
        }
    }
}
