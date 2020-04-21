/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Clases;

/**
 *
 * @author sistemas
 */
public enum Tipo 
{
    OkRequest, //Mensaje para protocolo de sincronización de paises, es el que se utiliza inicialmente para comprobar que un vecino broker está activo
    OkReply,  //Mensaje para protocolo de sincronización de paises, es el que se utiliza para responder a un OkRequest de un vecino broker
    BalanceRequest, //Mensaje para protocolo de balanceo de países, solicita a un broker hacer un balanceo y envía información para que este pueda responder
    BalanceReply, //Mensaje para responder si se puede o no hacer un balanceo, respuesta a un balanceRequest con null si no se puede ralizar, y un país en caso contrario
    BalanceLoad, //Mensaje para enviar el país que va a cambiar de host
    OkRequestPais,//Mensaje inicial de sincronización entre los países vecinos
    OkreplyPais, //Mensaje de confirmación para los OkRequestPais de los vecinos países
    getPoblacion, //Mensaje para pedir la población actualizada total de un vecino broker
    poblacionReturn, //Mensaje para responder a un getPoblación de un broker vecino que con tiene mi poblacion total actual
    hostChange, // Mensaje que realiza un pais a sus vecinos para informar sobre su nueva dirección ip
    notifyCurrentState // Mensaje para notificar a los otros países sobre mi estado actual
}