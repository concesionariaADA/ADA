package com.ada.catalogo;

import com.ada.persistencia.ConexionBaseDatos;

import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** {@link RepositorioMarcas} with plain JDBC: brands and their types in one single query. */
public final class RepositorioMarcasJdbc implements RepositorioMarcas {

    private static final String CONSULTA = "SELECT m.id, m.make_id_vpic, m.nombre, m.habilitada, m.baja,"
            + " COALESCE(array_agg(t.nombre ORDER BY t.id) FILTER (WHERE t.id IS NOT NULL),"
            + " ARRAY[]::varchar[]) AS tipos"
            + " FROM marca m"
            + " LEFT JOIN marca_tipo_vehiculo mt ON mt.marca_id = m.id"
            + " LEFT JOIN tipo_vehiculo t ON t.id = mt.tipo_vehiculo_id"
            + " GROUP BY m.id"
            + " ORDER BY m.nombre, m.id";

    @Override
    public List<Marca> listar() throws SQLException {
        List<Marca> marcas = new ArrayList<>();
        try (Connection conexion = ConexionBaseDatos.obtenerConexion();
             PreparedStatement sentencia = conexion.prepareStatement(CONSULTA);
             ResultSet fila = sentencia.executeQuery()) {
            while (fila.next()) {
                int makeId = fila.getInt("make_id_vpic");
                Integer makeIdVpic = fila.wasNull() ? null : makeId;
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
}
