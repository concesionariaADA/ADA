/*
 * Admin login page: wires the form to Api.iniciarSesion().
 * DOM wiring only; credentials are validated by the Java backend.
 */
(function () {
  "use strict";

  function mostrarError(texto) {
    const alerta = document.getElementById("login-error");
    alerta.textContent = texto;
    alerta.classList.remove("d-none");
  }

  function ocultarError() {
    const alerta = document.getElementById("login-error");
    alerta.textContent = "";
    alerta.classList.add("d-none");
  }

  document.addEventListener("DOMContentLoaded", function () {
    const formulario = document.getElementById("form-login");
    const boton = document.getElementById("boton-ingresar");

    formulario.addEventListener("submit", async function (evento) {
      evento.preventDefault();
      ocultarError();
      boton.disabled = true;

      const usuario = document.getElementById("usuario").value;
      const contrasena = document.getElementById("contrasena").value;

      try {
        const resultado = await window.Api.iniciarSesion(usuario, contrasena);
        if (resultado.ok) {
          window.location.href = "/admin";
          return;
        }
        if (resultado.status === 401) {
          mostrarError("Usuario o contraseña incorrectos.");
        } else {
          mostrarError("No se pudo iniciar sesión. Intentá de nuevo.");
        }
      } catch (error) {
        mostrarError("No se pudo iniciar sesión. Intentá de nuevo.");
      }
      boton.disabled = false;
    });
  });
})();
