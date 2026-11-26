package com.ada.catalogo;

import java.sql.SQLException;
import java.util.List;

/**
 * Reads the local catalog (brands, vehicle types and models) for the {@code /admin}
 * modules. Every method is read-only: it never changes any data.
 */
public interface RepositorioCatalogo {

    /** Every brand with its vehicle types, ordered by name. */
    List<Marca> listarMarcas() throws SQLException;

    /** The vehicle types with their brand and model counts, ordered by id. */
    List<TipoVehiculo> listarTiposVehiculo() throws SQLException;

    /** Every model with its brand and type, ordered by brand, model name and type. */
    List<Modelo> listarModelos() throws SQLException;
}
