package com.ada.catalogo;

import java.sql.SQLException;
import java.util.List;

/** Reads the brands of the local catalog for the {@code /admin} brand module. */
@FunctionalInterface
public interface RepositorioMarcas {

    /** Every brand, ordered by name; read-only, it never changes any data. */
    List<Marca> listar() throws SQLException;
}
