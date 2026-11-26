/*
 * ADA Concesionaria - shared /admin layout.
 * Single source of the backoffice menu (sections, labels and URLs). It renders
 * the top bar and the left sidebar into <div id="menu-admin"></div>, present in
 * every admin page, so the menu is not repeated in each HTML file.
 * DOM wiring only: no business logic; session handling lives in the backend.
 */
(function () {
  "use strict";

  // Menu definition (odd/tasks/admin-panel.md, S3). A null title means the
  // items are shown without a section heading.
  const MENU = [
    {
      titulo: null,
      items: [{ texto: "Dashboard", url: "/admin" }]
    },
    {
      titulo: "VEHÍCULOS",
      items: [
        { texto: "Automóviles", url: "/admin/automoviles" },
        { texto: "Camionetas", url: "/admin/camionetas" },
        { texto: "Motocicletas", url: "/admin/motocicletas" },
        { texto: "Tipos de vehículo", url: "/admin/tipos-vehiculo" }
      ]
    },
    {
      titulo: "EMPLEADOS",
      items: [
        { texto: "Vendedores", url: "/admin/vendedores" },
        { texto: "Administrativos", url: "/admin/administrativos" }
      ]
    },
    {
      titulo: "CLIENTES",
      items: [{ texto: "Clientes", url: "/admin/clientes" }]
    },
    {
      titulo: "VENTAS",
      items: [
        { texto: "Ventas", url: "/admin/ventas" },
        { texto: "Resumen", url: "/admin/ventas-resumen" },
        { texto: "Auditoría", url: "/admin/auditoria" }
      ]
    },
    {
      titulo: "PARÁMETROS",
      items: [
        { texto: "Marcas", url: "/admin/marcas" },
        { texto: "Modelos", url: "/admin/modelos" }
      ]
    }
  ];

  const ID_MENU = "menu-lateral";
  const CLAVE_COLAPSADO = "ada.admin.menuColapsado";
  const CLASE_COLAPSADO = "menu-colapsado";
  // Same breakpoint as Bootstrap's offcanvas-lg (lg = 992px).
  const ESCRITORIO = window.matchMedia("(min-width: 992px)");

  // "/admin/", "/admin/index" and "/admin/index.html" all mean the Dashboard.
  function rutaActual() {
    let ruta = window.location.pathname.replace(/\/+$/, "").replace(/\.html$/, "");
    if (ruta === "/admin/index") {
      ruta = "/admin";
    }
    return ruta;
  }

  function crear(etiqueta, clases, texto) {
    const elemento = document.createElement(etiqueta);
    if (clases) {
      elemento.className = clases;
    }
    if (texto !== undefined) {
      elemento.textContent = texto;
    }
    return elemento;
  }

  function crearBarraSuperior() {
    const barra = crear("header", "navbar navbar-dark navbar-ada barra-admin fixed-top");
    const contenedor = crear("div", "container-fluid flex-nowrap");

    const hamburguesa = crear("button", "navbar-toggler boton-hamburguesa");
    hamburguesa.type = "button";
    hamburguesa.id = "boton-menu";
    hamburguesa.setAttribute("aria-controls", ID_MENU);
    hamburguesa.setAttribute("aria-label", "Abrir o cerrar el menú");
    hamburguesa.appendChild(crear("span", "navbar-toggler-icon"));

    const marca = crear("a", "navbar-brand ms-3 me-auto", "ADA ");
    marca.href = "/admin";
    marca.appendChild(crear("span", null, "Admin"));

    const usuario = crear("span", "usuario-admin text-truncate");
    usuario.id = "usuario-admin";

    contenedor.append(hamburguesa, marca, usuario);
    barra.appendChild(contenedor);
    return barra;
  }

  function crearMenuLateral(actual) {
    const lateral = crear("aside", "offcanvas-lg offcanvas-start sidebar-ada");
    lateral.id = ID_MENU;
    lateral.tabIndex = -1;
    lateral.setAttribute("aria-labelledby", "titulo-menu-lateral");

    // Header only visible on mobile (Bootstrap hides it from lg up).
    const cabecera = crear("div", "offcanvas-header");
    const titulo = crear("h2", "offcanvas-title h6 mb-0", "ADA Admin");
    titulo.id = "titulo-menu-lateral";
    const cerrar = crear("button", "btn-close btn-close-white");
    cerrar.type = "button";
    cerrar.setAttribute("data-bs-dismiss", "offcanvas");
    cerrar.setAttribute("data-bs-target", "#" + ID_MENU);
    cerrar.setAttribute("aria-label", "Cerrar menú");
    cabecera.append(titulo, cerrar);

    const navegacion = crear("nav", "sidebar-ada-cuerpo");
    navegacion.setAttribute("aria-label", "Menú del Backoffice");
    MENU.forEach(function (seccion) {
      if (seccion.titulo) {
        navegacion.appendChild(crear("div", "sidebar-ada-titulo", seccion.titulo));
      }
      const lista = crear("ul", "nav flex-column");
      seccion.items.forEach(function (item) {
        const elemento = crear("li", "nav-item");
        const enlace = crear("a", "nav-link", item.texto);
        enlace.href = item.url;
        if (item.url === actual) {
          enlace.classList.add("active");
          enlace.setAttribute("aria-current", "page");
        }
        elemento.appendChild(enlace);
        lista.appendChild(elemento);
      });
      navegacion.appendChild(lista);
    });

    const pie = crear("div", "sidebar-ada-pie");
    const salir = crear("button", "btn btn-outline-light btn-sm w-100", "Cerrar sesión");
    salir.type = "button";
    salir.id = "boton-cerrar-sesion";
    pie.appendChild(salir);

    lateral.append(cabecera, navegacion, pie);
    return lateral;
  }

  function leerColapsado() {
    try {
      return window.localStorage.getItem(CLAVE_COLAPSADO) === "1";
    } catch (error) {
      return false;
    }
  }

  function guardarColapsado(colapsado) {
    try {
      window.localStorage.setItem(CLAVE_COLAPSADO, colapsado ? "1" : "0");
    } catch (error) {
      // Storage unavailable (e.g. private mode): the state just is not remembered.
    }
  }

  function actualizarAriaHamburguesa(boton, lateral) {
    const abierto = ESCRITORIO.matches
      ? !document.body.classList.contains(CLASE_COLAPSADO)
      : lateral.classList.contains("show");
    boton.setAttribute("aria-expanded", abierto ? "true" : "false");
  }

  function conectarHamburguesa(boton, lateral) {
    boton.addEventListener("click", function () {
      if (ESCRITORIO.matches) {
        // Desktop: collapse/expand the fixed sidebar and remember it.
        const colapsado = document.body.classList.toggle(CLASE_COLAPSADO);
        guardarColapsado(colapsado);
      } else {
        // Mobile: the sidebar is a Bootstrap offcanvas overlay.
        window.bootstrap.Offcanvas.getOrCreateInstance(lateral).toggle();
      }
      actualizarAriaHamburguesa(boton, lateral);
    });
    lateral.addEventListener("shown.bs.offcanvas", function () {
      actualizarAriaHamburguesa(boton, lateral);
    });
    lateral.addEventListener("hidden.bs.offcanvas", function () {
      actualizarAriaHamburguesa(boton, lateral);
    });
    ESCRITORIO.addEventListener("change", function () {
      actualizarAriaHamburguesa(boton, lateral);
    });
    actualizarAriaHamburguesa(boton, lateral);
  }

  function conectarCerrarSesion(boton) {
    boton.addEventListener("click", async function () {
      boton.disabled = true;
      try {
        await window.Api.cerrarSesion();
      } catch (error) {
        // Redirect anyway: the login page is the safe place to land.
      }
      window.location.href = "/admin/login";
    });
  }

  function mostrarUsuario() {
    window.Api.sesion()
      .then(function (datos) {
        const usuario = document.getElementById("usuario-admin");
        if (usuario && datos && datos.usuario) {
          usuario.textContent = datos.usuario;
          usuario.title = datos.usuario;
        }
      })
      .catch(function () {
        // 401 is redirected by Api; any other failure just leaves the name empty.
      });
  }

  document.addEventListener("DOMContentLoaded", function () {
    const contenedor = document.getElementById("menu-admin");
    if (!contenedor) {
      return;
    }

    // Apply the remembered desktop state before rendering to avoid a flash.
    if (leerColapsado()) {
      document.body.classList.add(CLASE_COLAPSADO);
    }

    const barra = crearBarraSuperior();
    const lateral = crearMenuLateral(rutaActual());
    contenedor.append(barra, lateral);

    conectarHamburguesa(document.getElementById("boton-menu"), lateral);
    conectarCerrarSesion(document.getElementById("boton-cerrar-sesion"));
    mostrarUsuario();

    // Enable the slide animation only after the first paint.
    window.requestAnimationFrame(function () {
      document.body.classList.add("menu-listo");
    });
  });
})();
