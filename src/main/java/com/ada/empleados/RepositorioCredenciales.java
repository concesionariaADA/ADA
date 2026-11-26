package com.ada.empleados;

import java.sql.SQLException;
import java.util.Optional;

/** Looks up the employees allowed to log in to the Backoffice ({@code /admin}). */
@FunctionalInterface
public interface RepositorioCredenciales {

    /**
     * Credentials of the active ({@code baja = false}) ADMINISTRATIVA employee with this
     * exact user name (RN19, RN04), or empty. Sellers (VENTA) are never returned: their
     * login is pending (D05).
     */
    Optional<CredencialEmpleado> buscarAdministrativoActivo(String nombreUsuario) throws SQLException;
}
