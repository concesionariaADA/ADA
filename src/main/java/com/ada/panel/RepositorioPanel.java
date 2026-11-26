package com.ada.panel;

import java.sql.SQLException;

/** Reads the counts of the {@code /admin} dashboard. */
@FunctionalInterface
public interface RepositorioPanel {

    /** Current counts; read-only, it never changes any data. */
    ResumenPanel obtenerResumen() throws SQLException;
}
