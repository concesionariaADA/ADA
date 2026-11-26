/*
 * Backoffice "Modelos" page: lists the models of the local catalog with Api.modelos()
 * and filters them in the browser by brand, model name and vehicle type. The type
 * filter can come preselected from the URL (?tipo=CAMIONETA), as linked from the
 * "Tipos de vehículo" page. Names are inserted with textContent, never as HTML.
 */
(function () {
  "use strict";

  const NOMBRES_TIPO = {
    AUTOMOVIL: "Automóvil",
    CAMIONETA: "Camioneta",
    MOTOCICLETA: "Motocicleta"
  };

  let modelos = [];

  function normalizar(texto) {
    return texto.normalize("NFD").replace(/[̀-ͯ]/g, "").toLowerCase().trim();
  }

  function celda(texto) {
    const td = document.createElement("td");
    td.textContent = texto;
    return td;
  }

  function fila(modelo) {
    const tr = document.createElement("tr");
    tr.appendChild(celda(modelo.marca));
    tr.appendChild(celda(modelo.nombre));
    tr.appendChild(celda(NOMBRES_TIPO[modelo.tipo] || modelo.tipo));
    tr.appendChild(celda(modelo.modelIdVpic === null ? "—" : String(modelo.modelIdVpic)));
    if (modelo.baja) {
      tr.className = "text-muted";
    }
    return tr;
  }

  function mostrar() {
    const marca = normalizar(document.getElementById("modelos-marca").value);
    const texto = normalizar(document.getElementById("modelos-buscar").value);
    const tipo = document.getElementById("modelos-tipo").value;
    const visibles = modelos.filter(function (modelo) {
      return (marca === "" || normalizar(modelo.marca).includes(marca))
        && (texto === "" || normalizar(modelo.nombre).includes(texto))
        && (tipo === "" || modelo.tipo === tipo);
    });
    const fragmento = document.createDocumentFragment();
    visibles.forEach(function (modelo) {
      fragmento.appendChild(fila(modelo));
    });
    if (visibles.length === 0) {
      const tr = document.createElement("tr");
      const td = celda(modelos.length === 0 ? "No hay modelos cargados." : "Ningún modelo coincide con el filtro.");
      td.colSpan = 4;
      td.className = "text-muted";
      tr.appendChild(td);
      fragmento.appendChild(tr);
    }
    document.getElementById("modelos-filas").replaceChildren(fragmento);
    document.getElementById("modelos-total").textContent =
      visibles.length.toLocaleString("es-AR") + " de " + modelos.length.toLocaleString("es-AR") + " modelos";
  }

  document.addEventListener("DOMContentLoaded", async function () {
    const selectorTipo = document.getElementById("modelos-tipo");
    const tipoInicial = new URLSearchParams(window.location.search).get("tipo");
    if (tipoInicial && Object.prototype.hasOwnProperty.call(NOMBRES_TIPO, tipoInicial)) {
      selectorTipo.value = tipoInicial;
    }
    document.getElementById("modelos-filtros").addEventListener("submit", function (evento) {
      evento.preventDefault();
    });
    document.getElementById("modelos-marca").addEventListener("input", mostrar);
    document.getElementById("modelos-buscar").addEventListener("input", mostrar);
    selectorTipo.addEventListener("change", mostrar);

    try {
      modelos = await window.Api.modelos();
      mostrar();
    } catch (error) {
      if (error && error.sesionVencida) {
        return; // Api is already redirecting to /admin/login.
      }
      document.getElementById("modelos-total").textContent = "";
      const alerta = document.getElementById("modelos-error");
      alerta.textContent = "No se pudieron cargar los modelos.";
      alerta.classList.remove("d-none");
    }
  });
})();
