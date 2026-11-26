/*
 * Backoffice home: wires the "Cerrar sesión" button to Api.cerrarSesion().
 * DOM wiring only; session handling lives in the Java backend.
 */
(function () {
  "use strict";

  document.addEventListener("DOMContentLoaded", function () {
    const boton = document.getElementById("boton-cerrar-sesion");

    boton.addEventListener("click", async function () {
      boton.disabled = true;
      try {
        await window.Api.cerrarSesion();
      } catch (error) {
        // Redirect anyway: the login page is the safe place to land.
      }
      window.location.href = "/admin/login";
    });
  });
})();
