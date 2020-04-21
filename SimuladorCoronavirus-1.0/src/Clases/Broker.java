package Clases;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import static java.lang.Thread.sleep;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Scanner;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author sistemas
 * @version 1.0
 * @created 07-mar.-2020 9:26:29
 */
public class Broker extends Thread {

    public static final String NAMEFILE = "brokerFile.txt";
    public ArrayList<String> vecinosBrokers;
    public ArrayList<Long> poblacionBrokers;
    public ArrayList<String> archivosPaises;
    public ArrayList<Pais> paisesRegistrados;
    public int puertoBrokers;
    public long cargaActual;
    public ServerSocket serverS;
    public int paisesNecesarios;
    public int paisesConectados;
    public ObjectOutputStream out;
    public ObjectInputStream in;
    public String ip;
    boolean siBrokersListos = false;
    boolean siPaisesListos = false;
    boolean balanceoCompletado = true;
    boolean ocupado = false;
    public Thread balanceador;
    public ArrayList<String> vecinosBalanceo;
    GUI gui;

    public Broker(GUI gui) {
        this.gui = gui;
        archivosPaises = new ArrayList<>();
        paisesRegistrados = new ArrayList<>();
        vecinosBalanceo = new ArrayList<>();
        try {
            ip = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException ex) {
            Logger.getLogger(Broker.class.getName()).log(Level.SEVERE, null, ex);
        }
        cargaActual = 0;
        vecinosBrokers = new ArrayList<>();
        poblacionBrokers = new ArrayList<>();
        paisesConectados = 0;
        //Se inicia el método que lee el archivo de configuración del broker
        leerArchivo();
        //Se inicia el funcionamiento del Broker
        this.start();
        //Se crea el hilo de escucha que va a escuchar y procesas todos los mensajes que lleguen al puerto del broker
        crearHiloEscucha();
    }

//    public static void main(String[] args) throws IOException {
//        try {
//            System.out.println("Mi ip:  " + InetAddress.getLocalHost().getHostAddress());
//        } catch (UnknownHostException ex) {
//            Logger.getLogger(Broker.class.getName()).log(Level.SEVERE, null, ex);
//        }
//        //Creo un broker para inciciar el programa
//        Broker br = new Broker();
//        
//    }

    public void run() {
        //Este es el funcionamiento principal del broker y se inicia cuando se llama start()
        //Primero se debe realizar el protocolo que sincroniza a todos los brokers vecinos
        if (iniciarOK()) {
            //Una vez se haya realizado la sincronizacion de los brokers se procede a inicializar los países 
            if (inicializarPaises()) {
                //Cuando haya iniciado y leído todos los países de mi archivo de configuración, activo el hilo que vigila el balanceo
                System.out.println("Se va a activar el hilo balanceador");
                activarBalanceador();
            }
        }
    }

    public void sumarPoblacionTotal() {
        this.cargaActual = 0;
        for (Pais paiseRegistrado : paisesRegistrados) {
            this.cargaActual += paiseRegistrado.getPoblacion();
        }
    }

    private boolean iniciarOK() {
        gui.escribirEstado("Se inició protocolo OK de Brokers...");
        //Se envían mensajes de tipo OkRequest a todos los brokers vecinos que detallaba el archivo
        boolean bandera = false;
        for (String v : vecinosBrokers) {
            bandera = false;
            while (bandera == false) {
                try {
                    Socket s = new Socket(v, puertoBrokers);
                    out = new ObjectOutputStream(s.getOutputStream());
                    //Envío el OkRequest
                    out.writeObject(new Mensaje(Tipo.OkRequest, null));
                    in = new ObjectInputStream(s.getInputStream());
                    Mensaje m = (Mensaje) in.readObject();
                    //Compruebo que el vecino haya respondido con OkReply
                    if (m.tipo == Tipo.OkReply) {
                        bandera = true;
                        System.out.println("Ip: " + v + " - Registrado ...");
                    }
                } catch (IOException e) {
                    System.out.println("Ip: " + v + " - Esperando ...");
                } catch (ClassNotFoundException ex) {
                    Logger.getLogger(Broker.class.getName()).log(Level.SEVERE, null, ex);
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                }
            }
        }
        //Bandera que funciona como un método para garantizar que el broker ya se ha sincronizado con todos los vecinos brokers
        siBrokersListos = true;
        System.out.println("Todos los brokers estan en linea ...");
        gui.escribirEstado("Se finalizó el protocolo OK de Brokers con exito, todos los brokers estan en linea !!!");
        return true;

    }

    private boolean crearHiloEscucha() {
        //Se crea u hilo que va a escuchar todos los mensajes que lleguen al socket leído en el archivo de configuración
        Broker b = this;
        Thread hiloEscucha = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    serverS = new ServerSocket(puertoBrokers);
                    System.out.println("Broker Escuchando - CrearHiloEscucha");

                    while (true) {
                        System.out.print("");
                        try {
                            Socket clientSocket = serverS.accept();
                            System.out.println("Solicitud recibida - Broker!!!");
                            //Se utiliza una clase ConnectionB para gesitonar los mensajes recibidos de otros brokers
                            ConnectionB c = new ConnectionB(clientSocket, b);
                        } catch (SocketTimeoutException e) {
                            System.out.println("Esuchando solicitudes");
                        }

                    }

                } catch (IOException ex) {
                    Logger.getLogger(Broker.class.getName()).log(Level.SEVERE, null, ex);

                }
            }
        });
        hiloEscucha.start();
        return true;
    }

    private void leerArchivo() {
        try {
            Scanner input = new Scanner(new File(NAMEFILE));
            while (input.hasNextLine()) {
                String line = input.nextLine();
                if (line.equals("puertoB:")) {
                    puertoBrokers = input.nextInt();
                } else if (line.equals("vecinosB:")) {
                    line = input.nextLine();
                    while (!line.equals("paises:")) {
                        System.out.println(line);
                        vecinosBrokers.add(line);
                        line = input.nextLine();
                    }
                    if (line.equals("paises:")) {
                        while (input.hasNext()) {
                            line = input.nextLine();
                            archivosPaises.add(line);
                        }
                    }
                }
            }
            input.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void balanceoCarga() {
        
        //Método que comienza con las peticiones de poblaciones
        System.out.println("Revisar balanceo...");
        //Solo se debe ejecutar cuando todos los brokers estén registrados y mis países se encuentren inicializados
        while (!(siBrokersListos && siPaisesListos)) {
            System.out.print("");
        }
        System.out.println("Se va a verificar la población de los brokers vecinos...");
        //Flag que me indica que se esta realizando un balanceo de carga
        balanceoCompletado = false;
        //Debo comprobar las poblaciones de los vecinos para saber si puedo realizar un balanceo
        if (this.paisesRegistrados.size()>1 && compararPoblaciones() ) {
            System.out.println("Un broker cumplió el criterio de balanceo");
            //Ordeno mis países para sacar el que más carga tenga y el candidato para cambiar de host
            ArrayList<Pais> aux = new ArrayList<>();
            aux.addAll(paisesRegistrados);
            Comparator<Pais> comparador = new Comparator<Pais>() {
                @Override
                public int compare(Pais t, Pais t1) {
                    return (int) (t.getPoblacion() - t1.getPoblacion());
                }
            };
            Collections.sort(aux, comparador);
            Pais pesado = aux.get(aux.size() - 1);
            //Voy a intentar enviar el país pesado 
            protocoloBalanceo(pesado);
        }
    }

    private boolean compararPoblaciones() {
        sumarPoblacionTotal();
        long auxPoblacion;
        boolean bandera = false;
        //Pido todas las poblaciones actuales de los vecinos brokers para ver si existe alguno cuya carga sea significativamente menor a la mía
        poblacionBrokers.clear();
        vecinosBalanceo.clear();
        for (int i = 0; i < vecinosBrokers.size(); i++) {
            bandera = false;
            while (bandera == false) {
                System.out.print("");
                try {
                    Socket s = new Socket(vecinosBrokers.get(i), puertoBrokers);
                    out = new ObjectOutputStream(s.getOutputStream());
                    //Utilizo un mensaje getPoblacion para recibir la población actual del broker
                    out.writeObject(new Mensaje(Tipo.getPoblacion, null));
                    in = new ObjectInputStream(s.getInputStream());
                    Mensaje m = (Mensaje) in.readObject();
                    if (m.tipo == Tipo.poblacionReturn) {
                        bandera = true;
                        auxPoblacion = (long) m.contenido;
                        System.out.println("El broker en: " + vecinosBrokers.get(i) + " tiene una poblacion de: " + auxPoblacion);
                        poblacionBrokers.add(i, auxPoblacion);
                    }
                } catch (IOException e) {
                    System.out.println("Broker: " + vecinosBrokers.get(i) + " - Esperando ...");
                } catch (ClassNotFoundException ex) {
                    Logger.getLogger(Broker.class.getName()).log(Level.SEVERE, null, ex);
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                }
            }
        }
        System.out.println("Se han pedido todas las poblaciones...");
        //Cuando tenga todas las poblaciones veo si algun broker cumple con las condiciones para realizar un balanceo
        for (int i = 0; i < poblacionBrokers.size(); i++) {
            //Un broker es considerado como apto para balanceo si tengo el doble o más que su población 
            if (poblacionBrokers.get(i) * 2 <= cargaActual) {
                //Guardo la dirección ip de los brokers que cumplieron con la condición
                vecinosBalanceo.add(new String(vecinosBrokers.get(i)));
                return true;
            }
        }

        return false;
    }

    public void protocoloBalanceo(Pais p) {
        
        gui.escribirEstado("Broker va a comenzar con el protocolo de balanceo con el pais "+p.getNomPais()+"...");
        
        System.out.println("Iniciando envío de país");
        boolean bandera = false;
        Pais aux = null;
        String ipBroker = null;
        for (String v : vecinosBalanceo) {
            bandera = false;
            while (bandera == false) {
                try {
                    Socket s = new Socket(v, puertoBrokers);
                    out = new ObjectOutputStream(s.getOutputStream());
                    //Envío una solicitud balanceRequest a cada vecino que cumpliera con los criterios
                    out.writeObject(new Mensaje(Tipo.BalanceRequest, p.getNomPais() + ";" + p.getPoblacion() + ";" + cargaActual));
                    in = new ObjectInputStream(s.getInputStream());
                    Mensaje m = (Mensaje) in.readObject();
                    bandera = true;
                    //Si el contendio del BalanceReply no es nulll significa que aceptó
                    if (m.tipo == Tipo.BalanceReply && m.contenido != null) {
                        System.out.println("El broker vecino aceptó mi solicitud");
                        //Si el vecino aceptó la solicitud debo interrumpir al país para que detenga su simulación
                        p.Worker.interrupt();
                        try {
                            //Continuo cuando el hilo Worker del país se detenga
                            p.Worker.join();
                        } catch (InterruptedException ex) {
                            Logger.getLogger(Broker.class.getName()).log(Level.SEVERE, null, ex);
                        }
                        System.out.println("Se decide enviar este país: " + p.getNomPais() + " que quedó en la iteración: " + p.iteraciones);
                        balanceoCompletado = true;
                        aux = (Pais) m.contenido;
                        ipBroker = new String(v);
                        break;
                        //Si es null es porque no aceptó y debo intentar con otro
                    } else if (m.tipo == Tipo.BalanceReply && m.contenido == null) {
                        System.out.println("El broker vecino declinó mi solicitud");
                        balanceoCompletado = false;
                        break;
                    }
                } catch (IOException e) {
                    System.out.println("readline:" + e.getMessage());
                } catch (ClassNotFoundException ex) {
                    Logger.getLogger(Broker.class.getName()).log(Level.SEVERE, null, ex);
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                }
            }

            if (balanceoCompletado) {
                try {
                    System.out.println("Enviaré el pais para el balanceo");
                    //Ejecuto el método para notificar el cambio de ip a los vecinos del país
                    p.notificarCambio(ipBroker);
                    Pais auxP = new Pais(p, null);
                    Socket s = new Socket(v, puertoBrokers);
                    out = new ObjectOutputStream(s.getOutputStream());
                    Mensaje m = new Mensaje(Tipo.BalanceLoad, auxP);
                    //Elimino al país en el host actual
                    eliminarPais(p);
                    //Envío el mensaje con el nuevo país
                    out.writeObject(m);
                    //Limpio las poblaciones solicitadas y los brokers que cumplen la condición
                    vecinosBalanceo.clear();
                    poblacionBrokers.clear();
                    System.out.println("Protocolo balanceo en broker completado...");
                    gui.escribirEstado("Protocolo balanceo en broker completado");
                    p.cerrarSocket();
                    break;
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                }
            }

        }
        if (balanceoCompletado == false) {
            System.out.println("Ningún país puede aceptar la solicitud de balanceo");
        }
    }

    public Pais verificacionBalanceRequest(String cadena) {
        //Debo dividir el contenido del mensaje para extraer los datos importantes
        String[] split = cadena.split(";");
        String nomPais = split[0];
        Long poblacionPais = Long.parseLong(split[1]);
        Long cargaActualSol = Long.parseLong(split[2]);
        //Actulizo la poblacion total que está asociada al broker y por lo tanto al host
        sumarPoblacionTotal();
        //Debo verificar que al aceptar el cambio, no quede en la posición de pedirle al solicitante un balanceo y quedar en un bucle de solcitides consecutivas
        if (((cargaActual + poblacionPais) * 2) <= cargaActualSol - poblacionPais) {
            System.out.println("No puedo aceptar el balanceo...");
            //Si no puedo aceptar, el contenido de la respuesta al broker solicitante es null y el sabrá que no puedo aceptar
            return null;
        } else {
            System.out.println("Si puedo aceptar el balanceo...");
            Pais aux = new Pais();
            aux.setNomPais("Confirmado");
            //Retorno un país que funciona simplemente para notificar que si estoy en la capacidad de hacer el balanceo
            return aux;
        }
    }

    private boolean inicializarPaises() {
        //Llamo un constructor especial que recibe el nombre del archivo de configuración de los países 
        //Los nombres de los archivos de los países están en el archivo de configuración de cada broker
        gui.escribirEstado("Se estan registrando los paises...");
        Pais auxP;
        for (String nomArchivo : archivosPaises) {
            auxP = new Pais(nomArchivo, gui);
            //Una vez el país comience su lógica de inicialización, lo agrego a mi arreglo de países
            paisesRegistrados.add(auxP);
        }
        siPaisesListos = true;
        gui.llenarPaises(paisesRegistrados);
        gui.escribirEstado("Se registraron los paises con exito !!!");
        return true;
    }

    private void activarBalanceador() {
        System.out.println("BROKER VA A COMENZAR CON EL PROTOCOLO DE BALANCEO");
        //Creo a un hilo balanceador que va a consultar las poblaciones de los brokers vecinos periodicamente
        this.balanceador = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        //Va a iniciar con la revisión cada 20 segundos para no saturar la red
                        sleep(2000);
                        //Método para iniciar con el balanceo de carga
                        balanceoCarga();
                        sleep(15000);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(Broker.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        });
        this.balanceador.start();
    }

    private void eliminarPais(Pais p) {
        //Método para eleminar al país que está guardado en el broker
        int index = -1, aux = 0, numAntes, numDespues;
        numAntes = paisesRegistrados.size();
        for (Pais itPais : paisesRegistrados) {
            if (itPais.getNomPais().equals(p.getNomPais())) {
                index = new Integer(aux);
            }
            aux++;
        }
        if (index > -1) {
            paisesRegistrados.remove(index);
        }
        numDespues = paisesRegistrados.size();
        System.out.println("Numero paises registrados antes " + numAntes + " ahora hay " + numDespues);
        gui.eliminarPais(p);
    }

    public void inicializarNuevoPais(Pais pais) {
        //Los países no estarán listos hasta que el nuevo país se inicialice
        siPaisesListos = false;
        //Creo un nuevo pais con base en los datos recibidos
        Pais auxP = new Pais(pais, this.gui);
        //Empiezo su ejecución
        auxP.start();
        //Lo agrego a mi arreglode países en ejecución
        paisesRegistrados.add(auxP);
        System.out.println("País agregado e inicializado...");
        //Ahora los páises vuelven a estar listos
        siPaisesListos = true;
    }

}//end Broker
