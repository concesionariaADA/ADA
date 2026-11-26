/*
 * Public home: fills the server status badge from Api.salud().
 * DOM wiring only; no business logic.
 */
(function () {
  "use strict";

  function mostrarEstado(texto, clase) {
    const insignia = document.getElementById("estado-servidor");
    if (!insignia) {
      return;
    }
    insignia.textContent = texto;
    insignia.className = "badge " + clase;
  }

  document.addEventListener("DOMContentLoaded", async function () {
    try {
      const datos = await window.Api.salud();
      if (datos && datos.estado === "ok") {
        mostrarEstado("Servidor en línea", "text-bg-success");
      } else {
        mostrarEstado("Servidor con problemas", "text-bg-danger");
      }
    } catch (error) {
      mostrarEstado("Servidor fuera de línea", "text-bg-danger");
    }
  });
})();
