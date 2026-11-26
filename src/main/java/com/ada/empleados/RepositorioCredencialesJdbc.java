package com.ada.empleados;

import com.ada.persistencia.ConexionBaseDatos;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

/** {@link RepositorioCredenciales} over the {@code empleado} table with plain JDBC. */
public final class RepositorioCredencialesJdbc implements RepositorioCredenciales {

    private static final String CONSULTA = "SELECT id, nombre_usuario, hash_contrasena FROM empleado"
            + " WHERE nombre_usuario = ? AND funcion = 'ADMINISTRATIVA' AND baja = false";

    @Override
    public Optional<CredencialEmpleado> buscarAdministrativoActivo(String nombreUsuario) throws SQLException {
        try (Connection conexion = ConexionBaseDatos.obtenerConexion();
             PreparedStatement sentencia = conexion.prepareStatement(CONSULTA)) {
            sentencia.setString(1, nombreUsuario);
            try (ResultSet fila = sentencia.executeQuery()) {
                if (!fila.next()) {
                    return Optional.empty();
                }
                return Optional.of(new CredencialEmpleado(
                        fila.getInt("id"), fila.getString("nombre_usuario"), fila.getString("hash_contrasena")));
            }
        }
    }
}
