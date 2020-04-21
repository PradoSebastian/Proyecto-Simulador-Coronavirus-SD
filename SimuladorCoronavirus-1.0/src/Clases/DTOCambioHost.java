/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Clases;

import java.io.Serializable;

/**
 *
 * @author ncp43
 */
// Objeto DTO que contiene el nombre de un país y la dirección ip
//Se utiliza para notificar a los vecinos cuando se va a realizar un cambio de host
public class DTOCambioHost implements Serializable {
    public String pNuevo;
    public String ipNueva;

    DTOCambioHost(String ip, String nomPais) {
        pNuevo=new String(nomPais);
        ipNueva = new String(ip);
    }
    
}
