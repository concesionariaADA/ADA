package com.ada.empleados;

/**
 * Login data of an employee: id, user name and stored password hash. Never holds a
 * plain password.
 */
public record CredencialEmpleado(int id, String nombreUsuario, String hashContrasena) {

    /** Keeps the hash out of logs and error messages. */
    @Override
    public String toString() {
        return "CredencialEmpleado[id=" + id + ", nombreUsuario=" + nombreUsuario + "]";
    }
}
