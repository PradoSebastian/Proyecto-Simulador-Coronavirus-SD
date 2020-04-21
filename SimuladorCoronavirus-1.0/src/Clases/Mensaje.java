/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Clases;

import java.io.Serializable;

/**
 *
 * @author sistemas
 * 
 */
//Todos los mensajes contienen un enumerado con el tipo de mensaje y un objeto que puede variar según sea su tipo
public class Mensaje implements Serializable
{

    public Tipo tipo; 
    public Object contenido;
    
    public Mensaje(Tipo t, Object c)
    {
        this.tipo = t;
        this.contenido = c;
    }
}
