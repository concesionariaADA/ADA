package com.ada;

import com.ada.servidor.ServidorWeb;

import java.io.IOException;

/** Entry point: only builds and starts the HTTP server (no module logic here). */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) throws IOException {
        ServidorWeb servidor = new ServidorWeb(ServidorWeb.leerPuertoDelEntorno());
        servidor.iniciar();
        Runtime.getRuntime().addShutdownHook(new Thread(servidor::detener));
        System.out.println("Servidor iniciado en http://localhost:" + servidor.obtenerPuerto() + "/");
    }
}
