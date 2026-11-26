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

  // Shared GET for authenticated /api/admin/* endpoints -> parsed JSON body.
  // A 401 means the admin session expired: redirect to the login page here,
  // centrally, and throw an error flagged with sesionVencida so callers can
  // skip their own error message while the browser navigates away.
  async function obtenerJsonAdmin(ruta) {
    const respuesta = await fetch(ruta, {
      method: "GET",
      headers: { Accept: "application/json" },
      credentials: "same-origin"
    });
    if (respuesta.status === 401) {
      window.location.href = "/admin/login";
      const error = new Error("HTTP 401");
      error.sesionVencida = true;
      throw error;
    }
    if (!respuesta.ok) {
      throw new Error("HTTP " + respuesta.status);
    }
    return respuesta.json();
  }

  // GET /api/admin/sesion -> {"usuario": "..."} of the current session.
  function sesion() {
    return obtenerJsonAdmin("/api/admin/sesion");
  }

  // GET /api/admin/dashboard -> {"unidadesDisponibles": n, "unidadesReservadas": n,
  // "unidadesVendidas": n, "ventasConfirmadas": n, "clientes": n, "empleados": n}.
  function dashboard() {
    return obtenerJsonAdmin("/api/admin/dashboard");
  }

  // GET /api/admin/marcas -> [{"id": n, "makeIdVpic": n|null, "nombre": "...",
  // "tipos": ["AUTOMOVIL", ...], "habilitada": bool, "baja": bool}], ordered by name.
  function marcas() {
    return obtenerJsonAdmin("/api/admin/marcas");
  }

  window.Api = Object.freeze({
    salud: salud,
    iniciarSesion: iniciarSesion,
    cerrarSesion: cerrarSesion,
    sesion: sesion,
    dashboard: dashboard,
    marcas: marcas
  });
})();
