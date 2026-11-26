package com.ada.catalogo;

import com.ada.persistencia.ConexionBaseDatos;

import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** {@link RepositorioCatalogo} with plain JDBC: each list comes from one single query. */
public final class RepositorioCatalogoJdbc implements RepositorioCatalogo {

    private static final String CONSULTA_MARCAS = "SELECT m.id, m.make_id_vpic, m.nombre, m.habilitada, m.baja,"
            + " COALESCE(array_agg(t.nombre ORDER BY t.id) FILTER (WHERE t.id IS NOT NULL),"
            + " ARRAY[]::varchar[]) AS tipos"
            + " FROM marca m"
            + " LEFT JOIN marca_tipo_vehiculo mt ON mt.marca_id = m.id"
            + " LEFT JOIN tipo_vehiculo t ON t.id = mt.tipo_vehiculo_id"
            + " GROUP BY m.id"
            + " ORDER BY m.nombre, m.id";

    private static final String CONSULTA_TIPOS = "SELECT t.id, t.nombre, t.tipo_vpic,"
            + " (SELECT count(*) FROM marca_tipo_vehiculo mt JOIN marca m ON m.id = mt.marca_id"
            + "   WHERE mt.tipo_vehiculo_id = t.id AND NOT m.baja) AS marcas,"
            + " (SELECT count(*) FROM modelo mo WHERE mo.tipo_vehiculo_id = t.id AND NOT mo.baja) AS modelos"
            + " FROM tipo_vehiculo t"
            + " ORDER BY t.id";

    private static final String CONSULTA_MODELOS = "SELECT mo.id, mo.model_id_vpic, mo.nombre, mo.baja,"
            + " m.nombre AS marca, t.nombre AS tipo"
            + " FROM modelo mo"
            + " JOIN marca m ON m.id = mo.marca_id"
            + " JOIN tipo_vehiculo t ON t.id = mo.tipo_vehiculo_id"
            + " ORDER BY m.nombre, mo.nombre, t.id, mo.id";

    @Override
    public List<Marca> listarMarcas() throws SQLException {
        List<Marca> marcas = new ArrayList<>();
        try (Connection conexion = ConexionBaseDatos.obtenerConexion();
             PreparedStatement sentencia = conexion.prepareStatement(CONSULTA_MARCAS);
             ResultSet fila = sentencia.executeQuery()) {
            while (fila.next()) {
                Integer makeIdVpic = enteroONulo(fila, "make_id_vpic");
                Array tipos = fila.getArray("tipos");
                List<String> nombresTipos;
                try {
                    nombresTipos = List.of((String[]) tipos.getArray());
                } finally {
                    tipos.free();
                }
                marcas.add(new Marca(fila.getInt("id"), makeIdVpic, fila.getString("nombre"),
                        nombresTipos, fila.getBoolean("habilitada"), fila.getBoolean("baja")));
            }
        }
        return marcas;
    }

    @Override
    public List<TipoVehiculo> listarTiposVehiculo() throws SQLException {
        List<TipoVehiculo> tipos = new ArrayList<>();
        try (Connection conexion = ConexionBaseDatos.obtenerConexion();
             PreparedStatement sentencia = conexion.prepareStatement(CONSULTA_TIPOS);
             ResultSet fila = sentencia.executeQuery()) {
            while (fila.next()) {
                tipos.add(new TipoVehiculo(fila.getInt("id"), fila.getString("nombre"),
                        fila.getString("tipo_vpic"), fila.getLong("marcas"), fila.getLong("modelos")));
            }
        }
        return tipos;
    }

    @Override
    public List<Modelo> listarModelos() throws SQLException {
        List<Modelo> modelos = new ArrayList<>();
        try (Connection conexion = ConexionBaseDatos.obtenerConexion();
             PreparedStatement sentencia = conexion.prepareStatement(CONSULTA_MODELOS);
             ResultSet fila = sentencia.executeQuery()) {
            while (fila.next()) {
                modelos.add(new Modelo(fila.getInt("id"), enteroONulo(fila, "model_id_vpic"),
                        fila.getString("nombre"), fila.getString("marca"), fila.getString("tipo"),
                        fila.getBoolean("baja")));
            }
        }
        return modelos;
    }

    private static Integer enteroONulo(ResultSet fila, String columna) throws SQLException {
        int valor = fila.getInt(columna);
        return fila.wasNull() ? null : valor;
    }
}
