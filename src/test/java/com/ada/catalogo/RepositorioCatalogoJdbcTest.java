package com.ada.catalogo;

import com.ada.persistencia.ConexionBaseDatos;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Runs the real catalog queries against the configured database. Skipped (not failed)
 * when the database is not configured or not reachable. They only read.
 */
class RepositorioCatalogoJdbcTest {

    private static final List<String> TIPOS = List.of("AUTOMOVIL", "CAMIONETA", "MOTOCICLETA");

    @Test
    void cadaMarcaApareceUnaVezConTiposValidosYOrdenados() throws Exception {
        assumeTrue(baseDeDatosDisponible(), "database not reachable: test skipped");
        List<Marca> marcas = new RepositorioCatalogoJdbc().listarMarcas();
        Set<Integer> ids = new HashSet<>();
        for (Marca marca : marcas) {
            assertTrue(ids.add(marca.id()), "repeated brand id " + marca.id());
            assertFalse(marca.nombre().isBlank(), "blank name for brand " + marca.id());
            assertTrue(TIPOS.containsAll(marca.tipos()), marca.nombre() + ": " + marca.tipos());
            List<String> ordenados = TIPOS.stream().filter(marca.tipos()::contains).toList();
            assertEquals(ordenados, marca.tipos(), marca.nombre());
        }
    }

    @Test
    void losTresTiposConConteosQueCoincidenConLosListados() throws Exception {
        assumeTrue(baseDeDatosDisponible(), "database not reachable: test skipped");
        RepositorioCatalogoJdbc repositorio = new RepositorioCatalogoJdbc();
        List<TipoVehiculo> tipos = repositorio.listarTiposVehiculo();
        assertEquals(TIPOS, tipos.stream().map(TipoVehiculo::nombre).toList());
        assertEquals(List.of(1, 2, 3), tipos.stream().map(TipoVehiculo::id).toList());

        Map<String, Long> modelosPorTipo = repositorio.listarModelos().stream()
                .filter(modelo -> !modelo.baja())
                .collect(Collectors.groupingBy(Modelo::tipo, Collectors.counting()));
        Map<String, Long> marcasPorTipo = repositorio.listarMarcas().stream()
                .filter(marca -> !marca.baja())
                .flatMap(marca -> marca.tipos().stream())
                .collect(Collectors.groupingBy(tipo -> tipo, Collectors.counting()));
        for (TipoVehiculo tipo : tipos) {
            assertEquals(modelosPorTipo.getOrDefault(tipo.nombre(), 0L), tipo.modelos(), tipo.nombre());
            assertEquals(marcasPorTipo.getOrDefault(tipo.nombre(), 0L), tipo.marcas(), tipo.nombre());
        }
    }

    @Test
    void cadaModeloTieneMarcaYTipoValido() throws Exception {
        assumeTrue(baseDeDatosDisponible(), "database not reachable: test skipped");
        Set<Integer> ids = new HashSet<>();
        for (Modelo modelo : new RepositorioCatalogoJdbc().listarModelos()) {
            assertTrue(ids.add(modelo.id()), "repeated model id " + modelo.id());
            assertFalse(modelo.marca().isBlank(), "model without brand " + modelo.id());
            assertTrue(TIPOS.contains(modelo.tipo()), modelo.nombre() + ": " + modelo.tipo());
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
