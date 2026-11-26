package com.ada.empleados;

import com.ada.persistencia.ConexionBaseDatos;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/** {@link AltaAdministrador} over the {@code empleado} table with plain JDBC. */
public final class AltaAdministradorJdbc implements AltaAdministrador {

    private static final String INSERCION = "INSERT INTO empleado"
            + " (legajo, nombre, apellido, funcion, nombre_usuario, hash_contrasena)"
            + " VALUES (?, ?, ?, 'ADMINISTRATIVA', ?, ?)";

    @Override
    public void insertar(String legajo, String nombre, String apellido, String usuario, String hash)
            throws SQLException {
        try (Connection conexion = ConexionBaseDatos.obtenerConexion();
             PreparedStatement sentencia = conexion.prepareStatement(INSERCION)) {
            sentencia.setString(1, legajo);
            sentencia.setString(2, nombre);
            sentencia.setString(3, apellido);
            sentencia.setString(4, usuario);
            sentencia.setString(5, hash);
            sentencia.executeUpdate();
        }
    }
}
