/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Clases;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import static java.lang.Thread.sleep;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author aulasingenieria
 */
class ConnectionP extends Thread {

    ObjectInputStream in;
    ObjectOutputStream out;
    Socket clientSocket;
    Pais pais;

    public ConnectionP(Socket aClientSocket, Pais p) {
        //Se guarda el país asociado y creo un socker con el número recibido 
        try {
            pais = p;
            clientSocket = aClientSocket;
            in = new ObjectInputStream(clientSocket.getInputStream());
            out = new ObjectOutputStream(clientSocket.getOutputStream());
            //Comienzo a escuchar el socket
            this.start();
        } catch (IOException e) {
            System.out.println("Connection:" + e.getMessage());
        }
    } // end Connection

    public void run() {
        try {
            Mensaje m = (Mensaje) in.readObject();
            sleep(100);
            //Se responden todas las solicitudes OkRequest de los países
            if (m.tipo == Tipo.OkRequestPais) {
                Pais auxP = new Pais();
                auxP.setNomPais(pais.getNomPais());
                auxP.setPoblacion(pais.getPoblacion());
                auxP.setPorcentAislamiento(pais.getPorcentAislamiento());
                auxP.setPorcentPoblaVulne(pais.getPorcentPoblaVulne());
                auxP.setPorcentajePoblaInfec(pais.getPorcentajePoblaInfec());
                
                auxP.poblacionInfectada = pais.poblacionInfectada;
                auxP.poblacionInfectadaVulnerable = pais.poblacionInfectadaVulnerable;
                auxP.poblacionSana = pais.poblacionSana;
                auxP.poblacionSanaVulnerable = pais.poblacionSanaVulnerable;
                auxP.poblacionAislamiento = pais.poblacionAislamiento;
                auxP.poblacionVulnerable = pais.poblacionVulnerable;
                
                System.out.println("Solicitud recibida ConnectionP " + m.tipo + " en: " + pais.getNomPais());
                m = new Mensaje(Tipo.OkreplyPais, auxP);
                out.writeObject(m);
                System.out.println("Mensaje replay enviado desde " + pais.getNomPais());
            } else if (m.tipo == Tipo.hostChange) {
                //Si un vecino ha cambiado de host, me envía un mensaje notificando cuál es su nueva dirección
                DTOCambioHost dtoCambio = (DTOCambioHost) m.contenido;
                System.out.println("Un vecino de " + pais.getNomPais() + " ha cambiado su dirección ip a: " + dtoCambio.ipNueva);
                pais.ipVecinos.replace(dtoCambio.pNuevo, dtoCambio.ipNueva);
                System.out.println("Ip de " + dtoCambio.pNuevo + " actualizada en " + pais.getNomPais());
            } else if (m.tipo == Tipo.notifyCurrentState) {
                //Un vecino desea notificarme su estado actual, en el mensaje está un País con los nuevos datos.
                pais.actualizarEstadoVecino((Pais)m.contenido);
            }
        } catch (EOFException e) {
            System.out.println("EOF:" + e.getMessage());
        } catch (IOException e) {
            System.out.println("readline en ConnectionP de " + pais.getNomPais() + " readline:" + e.getMessage());
            Logger.getLogger(ConnectionP.class.getName()).log(Level.SEVERE, null, e);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(ConnectionP.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InterruptedException ex) {
            Logger.getLogger(ConnectionP.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                /*close failed*/
            }
        }
    } // end run

} // end class Connection 

