package com.ada.panel;

/**
 * Counts shown on the {@code /admin} dashboard. Units, clients and employees only count
 * rows without a logical delete ({@code baja = false}).
 *
 * @param unidadesDisponibles units in state DISPONIBLE
 * @param unidadesReservadas  units in state RESERVADA
 * @param unidadesVendidas    units in state VENDIDA
 * @param ventasConfirmadas   sales in state CONFIRMADA
 * @param clientes            active clients
 * @param empleados           active employees
 */
public record ResumenPanel(
        long unidadesDisponibles,
        long unidadesReservadas,
        long unidadesVendidas,
        long ventasConfirmadas,
        long clientes,
        long empleados) {
}
