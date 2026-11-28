/*
 * ADA Concesionaria - /admin day/night theme (Issue #33).
 * Loaded in the <head> of every admin page so the saved theme is applied
 * before the first paint (no light flash). Night mode is the default.
 * It uses Bootstrap 5.3 color modes (data-bs-theme on <html>); the ADA
 * overrides for each mode live in /css/estilos.css.
 * Exposes window.TemaAdmin.crearBoton() for the admin top bar (menu-admin.js)
 * and fills any element marked with data-ranura-tema (e.g. the login page).
 */
(function () {
  "use strict";

  const CLAVE_TEMA = "ada.admin.tema";
  const NOCHE = "dark";
  const DIA = "light";
  const POR_DEFECTO = NOCHE;

  function leerTema() {
    try {
      const guardado = window.localStorage.getItem(CLAVE_TEMA);
      return guardado === DIA || guardado === NOCHE ? guardado : POR_DEFECTO;
    } catch (error) {
      return POR_DEFECTO;
    }
  }

  function guardarTema(tema) {
    try {
      window.localStorage.setItem(CLAVE_TEMA, tema);
    } catch (error) {
      // Storage unavailable (e.g. private mode): the choice just is not remembered.
    }
  }

  function aplicarTema(tema) {
    document.documentElement.setAttribute("data-bs-theme", tema);
  }

  const botones = [];

  // The button shows the mode it switches to, so its label states the action.
  function actualizarBoton(boton, tema) {
    const esNoche = tema === NOCHE;
    boton.textContent = esNoche ? "☀ Modo día" : "☾ Modo noche";
    boton.setAttribute("aria-pressed", esNoche ? "true" : "false");
    boton.setAttribute("aria-label", esNoche ? "Cambiar a modo día" : "Cambiar a modo noche");
    boton.title = boton.getAttribute("aria-label");
  }

  function alternar() {
    const nuevo = leerTema() === NOCHE ? DIA : NOCHE;
    aplicarTema(nuevo);
    guardarTema(nuevo);
    botones.forEach(function (boton) {
      actualizarBoton(boton, nuevo);
    });
  }

  function crearBoton() {
    const boton = document.createElement("button");
    boton.type = "button";
    boton.className = "btn btn-outline-light btn-sm boton-tema";
    boton.addEventListener("click", alternar);
    actualizarBoton(boton, leerTema());
    botones.push(boton);
    return boton;
  }

  // Another open admin tab changed the theme: follow it.
  window.addEventListener("storage", function (evento) {
    if (evento.key === CLAVE_TEMA) {
      const tema = leerTema();
      aplicarTema(tema);
      botones.forEach(function (boton) {
        actualizarBoton(boton, tema);
      });
    }
  });

  aplicarTema(leerTema());

  document.addEventListener("DOMContentLoaded", function () {
    document.querySelectorAll("[data-ranura-tema]").forEach(function (ranura) {
      ranura.appendChild(crearBoton());
    });
  });

  window.TemaAdmin = { crearBoton: crearBoton };
})();
