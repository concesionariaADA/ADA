package com.ada.catalogo;

/**
 * A vehicle type of the catalog (RN16), as listed in the {@code /admin} vehicle type module.
 *
 * @param id       type id (1 AUTOMOVIL, 2 CAMIONETA, 3 MOTOCICLETA)
 * @param nombre   type name
 * @param tipoVpic vPIC vehicle type the brands come from (car, truck, motorcycle), or null
 * @param marcas   brands offered in this type, without logically deleted ones
 * @param modelos  models of this type, without logically deleted ones
 */
public record TipoVehiculo(int id, String nombre, String tipoVpic, long marcas, long modelos) {
}
