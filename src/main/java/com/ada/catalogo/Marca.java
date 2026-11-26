package com.ada.catalogo;

import java.util.List;

/**
 * A brand of the local catalog (RN12), as listed in the {@code /admin} brand module.
 *
 * @param id         local id
 * @param makeIdVpic vPIC MakeId, or null for a brand that does not come from vPIC
 * @param nombre     brand name
 * @param tipos      vehicle types the brand is offered in (marca_tipo_vehiculo), ordered by
 *                   type id: AUTOMOVIL, CAMIONETA, MOTOCICLETA
 * @param habilitada enabled for sale by the administrator (RN23)
 * @param baja       logically deleted (RN24)
 */
public record Marca(int id, Integer makeIdVpic, String nombre, List<String> tipos,
                    boolean habilitada, boolean baja) {

    public Marca {
        tipos = List.copyOf(tipos);
    }
}
