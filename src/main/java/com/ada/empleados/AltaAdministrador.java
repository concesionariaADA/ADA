package com.ada.empleados;

import java.sql.SQLException;

/** Stores a new ADMINISTRATIVA employee; separated so the rules can be tested without a database. */
@FunctionalInterface
public interface AltaAdministrador {

    /**
     * Inserts the employee with function ADMINISTRATIVA.
     *
     * @param hash password hash produced by {@code HashContrasena}; never a plain password
     * @throws SQLException on database errors, including unique violations (SQLState 23505)
     */
    void insertar(String legajo, String nombre, String apellido, String usuario, String hash) throws SQLException;
}
