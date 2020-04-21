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
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author aulasingenieria
 */
class ConnectionB extends Thread {

    ObjectInputStream in;
    ObjectOutputStream out;
    Socket clientSocket;
    Broker broker;

    public ConnectionB(Socket aClientSocket, Broker b) 
    {
        //Creo un hilo que va a vigilar el socket recibio del broker recibido
        try 
        {
            broker = b;
            clientSocket = aClientSocket;
            in = new ObjectInputStream(clientSocket.getInputStream());
            out = new ObjectOutputStream(clientSocket.getOutputStream());
            //Vigilo constantemente los mensajes
            this.start();
        } 
        catch (IOException e) 
        {
            System.out.println("Connection:" + e.getMessage());
        }
    } // end Connection

    public void run() 
    {
        try 
        {	   
            Mensaje m = (Mensaje) in.readObject();
            sleep(100);
            //Si es un OkRequest debo responder con un OkReply a mi vecino broker
            if(m.tipo == Tipo.OkRequest)
            {
                m = new Mensaje(Tipo.OkReply, null);
                out.writeObject(m);
            }
            //El BalanceRequest es una solicitud de balanceo de un vecino broker 
            else if(m.tipo == Tipo.BalanceRequest)                                          
            {
                System.out.println("BalanceRequest recibido...");
                //Antes de proceder a atender la soliciutd debo vigilar que no esté ocupado atendiendo otra solicitud de balanceo y que los países asociados estén activos e inicializados
                while((!broker.siPaisesListos) || broker.ocupado)
                {
                    System.out.print("");
                }
                //verifico que pueda aceptar la solicitud de balanceo
                Pais p = broker.verificacionBalanceRequest((String)m.contenido);
                if(p != null){
                    //Si p es distinto de null es porque si puedo aceptar la solicitud, se activa el estado ocupado mientras se caba en protocolo
                    broker.ocupado = true;
                }
                //Envío el mensaje BalanceReply con la respuesta
                m = new Mensaje(Tipo.BalanceReply, p);
                out.writeObject(m);
                System.out.println("Respuesta de balanceo enviada...");
            }
            //En caso de haber respondido de forma afirmativa en mi balanceReply, el pais solicitante envia el pais que debo correr ahora
            else if(m.tipo == Tipo.BalanceLoad){
                System.out.println("Pais para cambio recibido");
                //Inicializo y corro el nuevo pais
                Pais aux = (Pais) m.contenido;
                broker.gui.agregarPais(aux);
                
                broker.inicializarNuevoPais(aux);
                //Desactivo mi estado de ocupado para estár disponible
                broker.ocupado = false;
            }
            else if(m.tipo == Tipo.getPoblacion){
                broker.sumarPoblacionTotal();
                long poblacion = broker.cargaActual;
                m = new Mensaje(Tipo.poblacionReturn,poblacion );
                out.writeObject(m);
                System.out.println("Poblacion retornada!!!");
            }

            
        } 
        catch (EOFException e) 
        {
            System.out.println("EOF:" + e.getMessage());
        } 
        catch (IOException e) 
        {
            System.out.println("Breadline:" + e.getMessage());
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(ConnectionB.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InterruptedException ex) {
            Logger.getLogger(ConnectionB.class.getName()).log(Level.SEVERE, null, ex);
        }
        finally 
        {
            try 
            {
                clientSocket.close();
            } 
            catch (IOException e) 
            {
                /*close failed*/
            }
        }
    } // end run

} // end class Connection 

