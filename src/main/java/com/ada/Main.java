package com.ada;

import com.ada.autenticacion.AlmacenSesiones;
import com.ada.autenticacion.ServicioAutenticacion;
import com.ada.catalogo.RepositorioMarcasJdbc;
import com.ada.empleados.RepositorioCredencialesJdbc;
import com.ada.panel.RepositorioPanelJdbc;
import com.ada.servidor.FiltroSesion;
import com.ada.servidor.ServidorWeb;

import java.io.IOException;
import java.time.Clock;

/** Entry point: only wires and starts the HTTP server (no module logic here). */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) throws IOException {
        ServicioAutenticacion autenticacion = new ServicioAutenticacion(
                new RepositorioCredencialesJdbc(), new AlmacenSesiones(Clock.systemUTC()));
        ServidorWeb servidor = new ServidorWeb(
                ServidorWeb.leerPuertoDelEntorno(), ServidorWeb.FRONTEND_POR_DEFECTO, autenticacion,
                new RepositorioPanelJdbc(), new RepositorioMarcasJdbc());
        servidor.agregarFiltroAdmin(new FiltroSesion(autenticacion));
        servidor.iniciar();
        Runtime.getRuntime().addShutdownHook(new Thread(servidor::detener));
        System.out.println("Servidor iniciado en http://localhost:" + servidor.obtenerPuerto() + "/");
    }
}
