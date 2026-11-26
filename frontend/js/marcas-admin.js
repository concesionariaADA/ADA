/*
 * Backoffice "Marcas" page: lists the brands of the local catalog with Api.marcas()
 * and filters them in the browser by name and vehicle type.
 * DOM wiring only; the data comes from the Java backend. Names are inserted with
 * textContent, never as HTML.
 */
(function () {
  "use strict";

  const NOMBRES_TIPO = {
    AUTOMOVIL: "Automóvil",
    CAMIONETA: "Camioneta",
    MOTOCICLETA: "Motocicleta"
  };

  let marcas = [];

  function normalizar(texto) {
    return texto.normalize("NFD").replace(/[̀-ͯ]/g, "").toLowerCase().trim();
  }

  function estado(marca) {
    if (marca.baja) {
      return { texto: "Dada de baja", clase: "text-bg-secondary" };
    }
    return marca.habilitada
      ? { texto: "Habilitada", clase: "text-bg-success" }
      : { texto: "Deshabilitada", clase: "text-bg-light border" };
  }

  function celda(texto) {
    const td = document.createElement("td");
    td.textContent = texto;
    return td;
  }

  function fila(marca) {
    const tr = document.createElement("tr");
    tr.appendChild(celda(marca.nombre));
    tr.appendChild(celda(marca.makeIdVpic === null ? "—" : String(marca.makeIdVpic)));
    tr.appendChild(celda(marca.tipos.map(function (tipo) {
      return NOMBRES_TIPO[tipo] || tipo;
    }).join(", ") || "—"));
    const td = document.createElement("td");
    const insignia = document.createElement("span");
    const datos = estado(marca);
    insignia.className = "badge " + datos.clase;
    insignia.textContent = datos.texto;
    td.appendChild(insignia);
    tr.appendChild(td);
    return tr;
  }

  function mostrar() {
    const texto = normalizar(document.getElementById("marcas-buscar").value);
    const tipo = document.getElementById("marcas-tipo").value;
    const visibles = marcas.filter(function (marca) {
      return (texto === "" || normalizar(marca.nombre).includes(texto))
        && (tipo === "" || marca.tipos.includes(tipo));
    });
    const cuerpo = document.getElementById("marcas-filas");
    const fragmento = document.createDocumentFragment();
    visibles.forEach(function (marca) {
      fragmento.appendChild(fila(marca));
    });
    if (visibles.length === 0) {
      const tr = document.createElement("tr");
      const td = celda(marcas.length === 0 ? "No hay marcas cargadas." : "Ninguna marca coincide con el filtro.");
      td.colSpan = 4;
      td.className = "text-muted";
      tr.appendChild(td);
      fragmento.appendChild(tr);
    }
    cuerpo.replaceChildren(fragmento);
    document.getElementById("marcas-total").textContent =
      visibles.length.toLocaleString("es-AR") + " de " + marcas.length.toLocaleString("es-AR") + " marcas";
  }

  document.addEventListener("DOMContentLoaded", async function () {
    const formulario = document.getElementById("marcas-filtros");
    formulario.addEventListener("submit", function (evento) {
      evento.preventDefault();
    });
    document.getElementById("marcas-buscar").addEventListener("input", mostrar);
    document.getElementById("marcas-tipo").addEventListener("change", mostrar);

    try {
      marcas = await window.Api.marcas();
      mostrar();
    } catch (error) {
      if (error && error.sesionVencida) {
        return; // Api is already redirecting to /admin/login.
      }
      document.getElementById("marcas-total").textContent = "";
      const alerta = document.getElementById("marcas-error");
      alerta.textContent = "No se pudieron cargar las marcas.";
      alerta.classList.remove("d-none");
    }
  });
})();
