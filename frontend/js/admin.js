/*
 * Backoffice home (Dashboard): fills the cards with Api.dashboard().
 * DOM wiring only; the counts are computed by the Java backend.
 * The menu and "Cerrar sesión" live in menu-admin.js.
 */
(function () {
  "use strict";

  const SIN_DATO = "—";

  function formatear(valor) {
    return typeof valor === "number" ? valor.toLocaleString("es-AR") : SIN_DATO;
  }

  document.addEventListener("DOMContentLoaded", async function () {
    const valores = document.querySelectorAll("[data-dato]");
    const alerta = document.getElementById("dashboard-error");

    valores.forEach(function (elemento) {
      elemento.textContent = "…";
    });

    try {
      const datos = await window.Api.dashboard();
      valores.forEach(function (elemento) {
        elemento.textContent = formatear(datos[elemento.dataset.dato]);
      });
    } catch (error) {
      if (error && error.sesionVencida) {
        return; // Api is already redirecting to /admin/login.
      }
      valores.forEach(function (elemento) {
        elemento.textContent = SIN_DATO;
      });
      alerta.textContent = "No se pudieron cargar los datos del panel.";
      alerta.classList.remove("d-none");
    }
  });
})();
