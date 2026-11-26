/*
 * ADA Concesionaria - API client.
 * This is the ONLY file that talks to the backend with fetch (Issue #24).
 * It holds no business logic: RN validations, prices and commissions live
 * in the Java backend.
 */
(function () {
  "use strict";

  // GET /api/salud -> parsed JSON body (e.g. {"estado":"ok"}).
  async function salud() {
    const respuesta = await fetch("/api/salud", {
      method: "GET",
      headers: { Accept: "application/json" },
      credentials: "same-origin"
    });
    if (!respuesta.ok) {
      throw new Error("HTTP " + respuesta.status);
    }
    return respuesta.json();
  }

  // POST /api/admin/login (form-urlencoded) -> {ok, status}.
  async function iniciarSesion(usuario, contrasena) {
    const respuesta = await fetch("/api/admin/login", {
      method: "POST",
      body: new URLSearchParams({ usuario: usuario, contrasena: contrasena }),
      credentials: "same-origin"
    });
    return { ok: respuesta.ok, status: respuesta.status };
  }

  // POST /api/admin/logout -> {ok, status}.
  async function cerrarSesion() {
    const respuesta = await fetch("/api/admin/logout", {
      method: "POST",
      credentials: "same-origin"
    });
    return { ok: respuesta.ok, status: respuesta.status };
  }

  window.Api = Object.freeze({
    salud: salud,
    iniciarSesion: iniciarSesion,
    cerrarSesion: cerrarSesion
  });
})();
