package com.ada.autenticacion;

import com.ada.empleados.CredencialEmpleado;
import com.ada.empleados.RepositorioCredenciales;

import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

/**
 * Admin login rules (Issue #3, RN19): only an active ADMINISTRATIVA employee with the
 * right password gets a session. Wrong user and wrong password are indistinguishable
 * to the caller, including in timing.
 */
public final class ServicioAutenticacion {

    /**
     * Hash checked when the user does not exist, so that case costs the same PBKDF2 work
     * as a wrong password. Built once, when the class loads, from a random throwaway
     * password that nobody knows.
     */
    private static final String HASH_FICTICIO = HashContrasena.generar(
            UUID.randomUUID().toString().toCharArray());

    private final RepositorioCredenciales repositorio;
    private final AlmacenSesiones sesiones;

    public ServicioAutenticacion(RepositorioCredenciales repositorio, AlmacenSesiones sesiones) {
        this.repositorio = repositorio;
        this.sesiones = sesiones;
    }

    /**
     * Checks the credentials and, when valid, opens a session.
     *
     * @return the new session token, or empty when the user or password is wrong
     * @throws SQLException when the employee lookup fails
     */
    public Optional<String> iniciarSesion(String usuario, char[] contrasena) throws SQLException {
        Optional<CredencialEmpleado> credencial = repositorio.buscarAdministrativoActivo(usuario);
        String hash = credencial.map(CredencialEmpleado::hashContrasena).orElse(HASH_FICTICIO);
        boolean valida = HashContrasena.verificar(contrasena, hash);
        if (!valida || credencial.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(sesiones.crear(credencial.get().nombreUsuario()));
    }

    /** User of a live session (renewing it), or empty. */
    public Optional<String> usuarioDeSesion(String token) {
        return sesiones.usuarioDe(token);
    }

    /** Closes the session, if any. */
    public void cerrarSesion(String token) {
        sesiones.invalidar(token);
    }
}
