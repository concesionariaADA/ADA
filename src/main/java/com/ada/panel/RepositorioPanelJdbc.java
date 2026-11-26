package com.ada.panel;

import com.ada.persistencia.ConexionBaseDatos;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/** {@link RepositorioPanel} with plain JDBC: every count comes from one single query. */
public final class RepositorioPanelJdbc implements RepositorioPanel {

    private static final String CONSULTA = "SELECT"
            + " (SELECT count(*) FROM unidad WHERE estado = 'DISPONIBLE' AND baja = false) AS unidades_disponibles,"
            + " (SELECT count(*) FROM unidad WHERE estado = 'RESERVADA' AND baja = false) AS unidades_reservadas,"
            + " (SELECT count(*) FROM unidad WHERE estado = 'VENDIDA' AND baja = false) AS unidades_vendidas,"
            + " (SELECT count(*) FROM venta WHERE estado = 'CONFIRMADA') AS ventas_confirmadas,"
            + " (SELECT count(*) FROM cliente WHERE baja = false) AS clientes,"
            + " (SELECT count(*) FROM empleado WHERE baja = false) AS empleados";

    @Override
    public ResumenPanel obtenerResumen() throws SQLException {
        try (Connection conexion = ConexionBaseDatos.obtenerConexion();
             PreparedStatement sentencia = conexion.prepareStatement(CONSULTA);
             ResultSet fila = sentencia.executeQuery()) {
            if (!fila.next()) {
                throw new SQLException("Dashboard query returned no row.");
            }
            return new ResumenPanel(
                    fila.getLong("unidades_disponibles"),
                    fila.getLong("unidades_reservadas"),
                    fila.getLong("unidades_vendidas"),
                    fila.getLong("ventas_confirmadas"),
                    fila.getLong("clientes"),
                    fila.getLong("empleados"));
        }
    }
}
