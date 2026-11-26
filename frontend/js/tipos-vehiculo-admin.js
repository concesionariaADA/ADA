/*
 * Backoffice "Tipos de vehículo" page: lists the vehicle types with Api.tiposVehiculo(),
 * with how many brands and models each one has, and links to its models.
 * DOM wiring only; the counts are computed by the Java backend.
 */
(function () {
  "use strict";

  const NOMBRES_TIPO = {
    AUTOMOVIL: "Automóvil",
    CAMIONETA: "Camioneta",
    MOTOCICLETA: "Motocicleta"
  };

  function celda(texto, clase) {
    const td = document.createElement("td");
    td.textContent = texto;
    if (clase) {
      td.className = clase;
    }
    return td;
  }

  function fila(tipo) {
    const tr = document.createElement("tr");
    tr.appendChild(celda(NOMBRES_TIPO[tipo.nombre] || tipo.nombre, "fw-semibold"));
    tr.appendChild(celda(tipo.tipoVpic === null ? "—" : tipo.tipoVpic));
    tr.appendChild(celda(tipo.marcas.toLocaleString("es-AR"), "text-end"));
    tr.appendChild(celda(tipo.modelos.toLocaleString("es-AR"), "text-end"));
    const acciones = document.createElement("td");
    acciones.className = "text-end";
    const enlace = document.createElement("a");
    enlace.className = "btn btn-sm btn-outline-dark";
    enlace.href = "/admin/modelos?tipo=" + encodeURIComponent(tipo.nombre);
    enlace.textContent = "Ver modelos";
    acciones.appendChild(enlace);
    tr.appendChild(acciones);
    return tr;
  }

  document.addEventListener("DOMContentLoaded", async function () {
    const cuerpo = document.getElementById("tipos-filas");
    try {
      const tipos = await window.Api.tiposVehiculo();
      const fragmento = document.createDocumentFragment();
      tipos.forEach(function (tipo) {
        fragmento.appendChild(fila(tipo));
      });
      cuerpo.replaceChildren(fragmento);
    } catch (error) {
      if (error && error.sesionVencida) {
        return; // Api is already redirecting to /admin/login.
      }
      cuerpo.replaceChildren();
      const alerta = document.getElementById("tipos-error");
      alerta.textContent = "No se pudieron cargar los tipos de vehículo.";
      alerta.classList.remove("d-none");
    }
  });
})();
