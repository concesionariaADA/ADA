package com.ada.catalogo;

/**
 * A model of the local catalog (RN03 c, RN12), as listed in the {@code /admin} model module.
 *
 * @param id          local id
 * @param modelIdVpic vPIC Model_ID, or null for a model that does not come from vPIC
 * @param nombre      model name
 * @param marca       name of its brand
 * @param tipo        name of its vehicle type (AUTOMOVIL, CAMIONETA, MOTOCICLETA)
 * @param baja        logically deleted (RN24)
 */
public record Modelo(int id, Integer modelIdVpic, String nombre, String marca, String tipo, boolean baja) {
}
