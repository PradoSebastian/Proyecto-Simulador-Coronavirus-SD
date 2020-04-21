package Clases;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @version 1.0
 * @created 07-mar.-2020 9:30:50
 */
public class Pais extends Thread implements Serializable {

    private String nomPais;
    private long poblacion;
    private float porcentAislamiento;
    private float porcentajePoblaInfec;
    private float porcentPoblaVulne;
    public ArrayList<Pais> vecinosAereos;
    public ArrayList<Pais> vecinosTerrestres;
    public HashMap<String, Integer> puertosVecinos;
    public HashMap<String, String> ipVecinos;
    private int puertoPaises;
    private ServerSocket serverS;
    private String ipBroker = "localhost";
    public ObjectOutputStream out;
    public ObjectInputStream in;
    public Thread Worker;
    public double iteraciones;
    public boolean activo = true;

    public long poblacionSana;
    public long poblacionSanaVulnerable;
    public long poblacionAislamiento;
    public long poblacionVulnerable;
    public long poblacionInfectada;
    public long poblacionInfectadaVulnerable;
    public static final double TASA_TRANSMISION = 0.04;
    public static final double TASA_MORTALIDAD = 0.02;
    public static final double TASA_TRANSMICION_VULNERABLES = 0.06;
    public static final double TASA_MORTALIDAD_VULNERABLES = 0.04;
    public static final double TASA_TRANSMISION_AEREA = 0.01;
    public static final double TASA_TRANSMISION_TERRESTRE = 0.01;
    public static final double TASA_TRANSMISION_AEREA_VULNERABLES = 0.02;
    public static final double TASA_TRANSMISION_TERRESTRE_VULNERABLES = 0.02;
    
    public GUI gui;

    public Pais(String nFile, GUI gui) {
        
        this.gui = gui;
        
        //Recibo el nombre del archivo que se debe usar para inicializar este país
        iteraciones = 0;
        vecinosAereos = new ArrayList();
        vecinosTerrestres = new ArrayList();
        puertosVecinos = new HashMap<>();
        ipVecinos = new HashMap<>();
        //Leo todos los atributos, incluyendo a mis vecinos, del archivo de configuración
        leerArchivo(nFile);

        poblacionAislamiento = (long) (poblacion * porcentAislamiento);
        poblacionInfectada = (long) (poblacion * porcentajePoblaInfec);
        poblacionVulnerable = (long) (poblacion * porcentPoblaVulne);
        poblacionInfectadaVulnerable = 0;
        poblacionSana = (long) ((poblacion * (1 - porcentajePoblaInfec)) - poblacionVulnerable);
        poblacionSanaVulnerable = poblacionVulnerable;

//        System.out.println("Porcentajes: \n");
//        System.out.println("Aislamiento: " + porcentAislamiento);
//        System.out.println("Vulnerables: " + porcentPoblaVulne);
//        System.out.println("infectados: " + porcentajePoblaInfec);
//        
//        simulacion();
        //Una vez se haya leído todo, se comienza a correr el hilo 
        this.start();
    }

    public Pais(Pais p, GUI gui) {
        //Contrusctor que sirve para crear un país con los mismos atributos y vecinos de uno recibido como parametro
        this.gui = gui;
        this.vecinosAereos = new ArrayList();
        this.vecinosTerrestres = new ArrayList();
        this.puertosVecinos = new HashMap<>();
        this.ipVecinos = new HashMap<>();
        this.nomPais = p.getNomPais();
        this.poblacion = p.getPoblacion();
        this.porcentAislamiento = p.porcentAislamiento;
        this.porcentPoblaVulne = p.porcentPoblaVulne;
        this.porcentajePoblaInfec = p.porcentajePoblaInfec;
        this.puertoPaises = p.getPuertoPaises();
        this.vecinosAereos.addAll(p.getVecinosAereos());
        this.vecinosTerrestres.addAll(p.getVecinosTerrestres());
        this.ipVecinos.putAll(p.ipVecinos);
        this.puertosVecinos.putAll(p.puertosVecinos);
        this.poblacionAislamiento = p.poblacionAislamiento;
        this.poblacionInfectada = p.poblacionInfectada;
        this.poblacionVulnerable = p.poblacionVulnerable;
        this.poblacionInfectadaVulnerable = p.poblacionInfectadaVulnerable;
        this.poblacionSana = p.poblacionSana;
        this.poblacionSanaVulnerable = p.poblacionSanaVulnerable;
    }

    public Pais() {

    }

    public void run() {
        //Lógica principal del país
        System.out.println("El país " + nomPais + " va a contactarse con sus vecinos...");
        System.out.println("Vecinos terrestres: " + vecinosTerrestres.size());
        System.out.println("Vecinos aereos: " + vecinosAereos.size());
        //Creo el hilo que va a escuchar todos los mensajes que lleguen al socket que leí del archivo
        crearHiloEscucha();
        //Se sincronizan todos los vecinos antes de empezar con la ejecución del país
        if (vecinosOk()) {
            System.out.println("El país " + nomPais + " esta corriendo...");
            //Se llama al método simular para la simulación de la propagación 
            simular();
        } else {
            System.out.println("PaisOk fallido en " + this.nomPais);
        }

    }

    private void leerArchivo(String nFile) {
        try {
            Scanner input = new Scanner(new File(nFile));
            while (input.hasNextLine()) {
                String line = input.nextLine();
                if (line.equals("nombre:")) {
                    nomPais = input.nextLine();
                } else if (line.equals("poblacion:")) {
                    poblacion = input.nextLong();
                } else if (line.equals("porcentajeaislamiento:")) {
                    porcentAislamiento = input.nextFloat();
                } else if (line.equals("porcentajepoblacioninfectada:")) {
                    porcentajePoblaInfec = input.nextFloat();
                } else if (line.equals("porcentajepoblacionvulnerable:")) {
                    porcentPoblaVulne = input.nextFloat();
                } else if (line.equals("puertopaises:")) {
                    puertoPaises = input.nextInt();
                } else if (line.equals("vecinosaereo:")) {
                    line = input.nextLine();
                    while (!line.equals("vecinosterrestres:")) {
                        String[] split = line.split(";");
                        vecinosAereos.add(new Pais());
                        vecinosAereos.get(vecinosAereos.size() - 1).setNomPais(split[0]);
                        ipVecinos.put(split[0], split[1]);
                        puertosVecinos.put(split[0], Integer.valueOf(split[2]));
                        line = input.nextLine();
                    }
                    while (input.hasNextLine()) {
                        line = input.nextLine();
                        String[] split = line.split(";");
                        vecinosTerrestres.add(new Pais());
                        vecinosTerrestres.get(vecinosTerrestres.size() - 1).setNomPais(split[0]);
                        ipVecinos.put(split[0], split[1]);
                        puertosVecinos.put(split[0], Integer.valueOf(split[2]));
                    }
                }
            }
            input.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void cargar() {

        long a = 123495872;
        long b = 1239852;
        long c = 0;
        // Call an expensive task, or sleep if you are monitoring a remote process
        for (double i = 0; i < 20000000; i++) {

            c += a / b;
            c += c * b;
            if (i % 1000000 == 0) {
                iteraciones++;
            }
        }

    }

    private boolean vecinosOk() {
        gui.escribirEstado("Iniciando Protocolo Ok paises en Pais "+ this.getNomPais()+"... ");
        //Método para sincronizar a todos los vecinos y poder asegurarme de que están en linea
        boolean vecinosAListos = false;
        boolean vecinosTListos = false;
        Pais auxVec;
        //Primero ejecuto el protocolo con todos los vecinos aereos
        if (vecinosAereos.size() > 0) {
            for (Pais vecino : vecinosAereos) {
                //Envio un OkRequesPais a todos mis vecinos y espero su reply 
                //Termino cuando todos estén listos
                vecinosAListos = false;
                while (!vecinosAListos) {
                    try {
                        Socket s = new Socket(ipVecinos.get(vecino.getNomPais()), puertosVecinos.get(vecino.getNomPais()));
                        out = new ObjectOutputStream(s.getOutputStream());
                        out.writeObject(new Mensaje(Tipo.OkRequestPais, null));
                        in = new ObjectInputStream(s.getInputStream());
                        Mensaje m = (Mensaje) in.readObject();
                        if (m.tipo == Tipo.OkreplyPais) {
                            vecinosAListos = true;
                            auxVec = (Pais) m.contenido;
                            System.out.println("El vecino aereo: " + auxVec.getNomPais() + " - Registrado en " + nomPais);
                            vecino.poblacion = auxVec.poblacion;
                            vecino.porcentAislamiento = auxVec.porcentAislamiento;
                            vecino.porcentPoblaVulne = auxVec.porcentPoblaVulne;
                            vecino.porcentajePoblaInfec = auxVec.porcentajePoblaInfec;
                            vecino.poblacion = auxVec.poblacion;
                            vecino.poblacionInfectada = auxVec.poblacionInfectada;
                            vecino.poblacionInfectadaVulnerable = auxVec.poblacionInfectadaVulnerable;
                            vecino.poblacionSana = auxVec.poblacionSana;
                            vecino.poblacionSanaVulnerable = auxVec.poblacionSanaVulnerable;
                            vecino.poblacionAislamiento = auxVec.poblacionAislamiento;
                            vecino.poblacionVulnerable = auxVec.poblacionVulnerable;
                        }
                    } catch (IOException e) {
                        System.out.println("Vecino aereo: " + vecino.nomPais + " - Esperando desde " + this.nomPais);
                    } catch (ClassNotFoundException ex) {
                        Logger.getLogger(Broker.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (Exception e) {
                        System.out.println(e.getMessage());
                    }
                }
            }
        } else {
            vecinosAListos = true;
        }
        //Después ejecuto el protocolo con todos los vecinos terrestres
        if (vecinosTerrestres.size() > 0) {
            for (Pais vecino : vecinosTerrestres) {
                //Envio un OkRequesPais a todos mis vecinos y espero su reply 
                //Termino cuando todos estén listos
                vecinosTListos = false;
                while (!vecinosTListos) {
                    try {
                        Socket s = new Socket(ipVecinos.get(vecino.getNomPais()), puertosVecinos.get(vecino.getNomPais()));
                        out = new ObjectOutputStream(s.getOutputStream());
                        out.writeObject(new Mensaje(Tipo.OkRequestPais, null));
                        in = new ObjectInputStream(s.getInputStream());
                        Mensaje m = (Mensaje) in.readObject();
                        if (m.tipo == Tipo.OkreplyPais) {
                            vecinosTListos = true;
                            auxVec = (Pais) m.contenido;
                            System.out.println("El vecino terrestre: " + auxVec.getNomPais() + " - Registrado en " + nomPais);
                            vecino.poblacion = auxVec.poblacion;
                            vecino.porcentAislamiento = auxVec.porcentAislamiento;
                            vecino.porcentPoblaVulne = auxVec.porcentPoblaVulne;
                            vecino.porcentajePoblaInfec = auxVec.porcentajePoblaInfec;
                            vecino.poblacion = auxVec.poblacion;
                            vecino.poblacionInfectada = auxVec.poblacionInfectada;
                            vecino.poblacionInfectadaVulnerable = auxVec.poblacionInfectadaVulnerable;
                            vecino.poblacionSana = auxVec.poblacionSana;
                            vecino.poblacionSanaVulnerable = auxVec.poblacionSanaVulnerable;
                            vecino.poblacionAislamiento = auxVec.poblacionAislamiento;
                            vecino.poblacionVulnerable = auxVec.poblacionVulnerable;
                        }
                    } catch (IOException e) {
                        System.out.println("Vecino terrestre: " + vecino.nomPais + " - Esperando ...");
                    } catch (ClassNotFoundException ex) {
                        Logger.getLogger(Broker.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (Exception e) {
                        System.out.println(e.getMessage());
                    }
                }
            }
        } else {
            vecinosTListos = true;
        }

        if (vecinosAListos && vecinosTListos) {
            gui.escribirEstado("Protocolo Ok paises en Pais "+ this.getNomPais()+" finalizó con exito, todos los vecinos estan conectados !!!");
            return true;
        }
        return false;
    }

    private boolean crearHiloEscucha() {
        //Se crea al hilo que va a recibir todas las peticiones o mensajes 
        Pais p = this;
        Thread hiloEscucha = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    serverS = new ServerSocket(puertoPaises);
                    System.out.println("Pais " + p.getNomPais() + " esuchando - CrearHiloEscucha");

                    while (true) {
                        try {
                            Socket clientSocket = serverS.accept();
                            System.out.println("Solicitud recibida - en Pais: " + p.getNomPais());
                            // Se utiliza una clase ConnectionP para gestionar todas los mensajes entre paises
                            ConnectionP conP = new ConnectionP(clientSocket, p);
                        } catch (SocketTimeoutException e) {
                            System.out.println("Esuchando solicitudes");
                        } catch (SocketException e) {
                            System.out.println("Socket cerrado en hilo escucha");
                            break;
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

    private void simular() {
        //La simulación va a estar a cargo de un hilo worker de cada país 
        Pais p = this;
        this.Worker = new Thread(new Runnable() {
            @Override
            public void run() {
                System.out.println("Iniciando carga en :" + p.getNomPais());
                simulacion();
            }
        });
        this.Worker.start();
    }

    public void notificarCambio(String ip) {
        //Este método se llama cuando se va a realizar un cambio de host del país
        //Debo notificar a todos mis vecinos aereos sobre mi nueva dirección 
        for (int i = 0; i < vecinosAereos.size(); i++) {
            try {
                //Obtengo la dirección ip y número de puerto para cada vecino aereo
                Socket s = new Socket(ipVecinos.get(vecinosAereos.get(i).getNomPais()), puertosVecinos.get(vecinosAereos.get(i).getNomPais()));
                out = new ObjectOutputStream(s.getOutputStream());
                //Para informar utilizp un mensaje hostChange, mi nombre y mi nueva dirección 
                out.writeObject(new Mensaje(Tipo.hostChange, new DTOCambioHost(ip, this.getNomPais())));
            } catch (IOException ex) {
                Logger.getLogger(Pais.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        //Debo notificar a todos mis vecinos terrestres sobre mi nueva dirección 
        for (int i = 0; i < vecinosTerrestres.size(); i++) {
            try {
                //Obtengo la dirección ip y número de puerto para cada vecino terrestre
                Socket s = new Socket(ipVecinos.get(vecinosTerrestres.get(i).getNomPais()), puertosVecinos.get(vecinosTerrestres.get(i).getNomPais()));
                out = new ObjectOutputStream(s.getOutputStream());
                //Para informar utilizp un mensaje hostChange, mi nombre y mi nueva dirección
                out.writeObject(new Mensaje(Tipo.hostChange, new DTOCambioHost(ip, this.getNomPais())));
            } catch (IOException ex) {
                Logger.getLogger(Pais.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public void notificarEstado() {
        //Este método sirve para notificar mi estado actual a todos los vecinos
        //Guardo mis atributos actuales que les pueden interezar a mis vecinos
        Pais auxP = new Pais();
        auxP.setNomPais(this.nomPais);
        auxP.poblacion = this.poblacion;
        auxP.poblacionInfectada = this.poblacionInfectada;
        auxP.poblacionInfectadaVulnerable = this.poblacionInfectadaVulnerable;
        auxP.poblacionSana = this.poblacionSana;
        auxP.poblacionSanaVulnerable = this.poblacionSanaVulnerable;
        auxP.poblacionAislamiento = this.poblacionAislamiento;
        auxP.poblacionVulnerable = this.poblacionVulnerable;

        //Notificar a los vecinos aereos
        for (int i = 0; i < vecinosAereos.size(); i++) {
            try {
                //Obtengo la dirección ip y número de puerto para cada vecino aereo
                Socket s = new Socket(ipVecinos.get(vecinosAereos.get(i).getNomPais()), puertosVecinos.get(vecinosAereos.get(i).getNomPais()));
                out = new ObjectOutputStream(s.getOutputStream());
                //El mensaje es de tipo notifyCurrentState y contiene mis atributos necesarios para la simulación y que mis vecinos requieren tener actualizados
                out.writeObject(new Mensaje(Tipo.notifyCurrentState, auxP));
            } catch (IOException ex) {
                Logger.getLogger(Pais.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        //Notificar a los vecinos terrestres 
        for (int i = 0; i < vecinosTerrestres.size(); i++) {
            try {
                //Obtengo la dirección ip y número de puerto para cada terrestre
                Socket s = new Socket(ipVecinos.get(vecinosTerrestres.get(i).getNomPais()), puertosVecinos.get(vecinosTerrestres.get(i).getNomPais()));
                out = new ObjectOutputStream(s.getOutputStream());
                //El mensaje es de tipo notifyCurrentState y contiene mis atributos necesarios para la simulación y que mis vecinos requieren tener actualizados
                out.writeObject(new Mensaje(Tipo.notifyCurrentState, auxP));
            } catch (IOException ex) {
                Logger.getLogger(Pais.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public void actualizarEstadoVecino(Pais estadoVecino) {
        boolean esVecinoAereo = false;
        for (Pais itVecino : vecinosAereos) {
            if (itVecino.getNomPais().equals(estadoVecino.getNomPais())) {
                esVecinoAereo = true;
                //Actualizo los datos locales del vecino recibido como parámetro
                itVecino.poblacion = estadoVecino.poblacion;
                itVecino.porcentAislamiento = estadoVecino.porcentAislamiento;
                itVecino.porcentPoblaVulne = estadoVecino.porcentPoblaVulne;
                itVecino.porcentajePoblaInfec = estadoVecino.porcentajePoblaInfec;
                itVecino.poblacionInfectada = estadoVecino.poblacionInfectada;
                itVecino.poblacionInfectadaVulnerable = estadoVecino.poblacionInfectadaVulnerable;
                itVecino.poblacionSana = estadoVecino.poblacionSana;
                itVecino.poblacionSanaVulnerable = estadoVecino.poblacionSanaVulnerable;
                itVecino.poblacionAislamiento = estadoVecino.poblacionAislamiento;
                itVecino.poblacionVulnerable = estadoVecino.poblacionVulnerable;
                System.out.println("Estado de " + itVecino.getNomPais() + " actualizado en " + this.nomPais);
                break;
            }
        }
        if (!esVecinoAereo) {
            for (Pais itVecino : vecinosTerrestres) {
                if (itVecino.getNomPais().equals(estadoVecino.getNomPais())) {
                    //Actualizo los datos locales del vecino recibido como parámetro
                    itVecino.setPoblacion(estadoVecino.getPoblacion());
                    itVecino.setPorcentAislamiento(estadoVecino.getPorcentAislamiento());
                    itVecino.setPorcentPoblaVulne(estadoVecino.getPorcentPoblaVulne());
                    itVecino.setPorcentajePoblaInfec(estadoVecino.getPorcentajePoblaInfec());
                    System.out.println("Estado de " + itVecino.getNomPais() + " actualizado en " + this.nomPais);
                    break;
                }
            }
        }
    }

    public void simulacion() {
        int cont = 0;
        long nuevosMuertos = 0, nuevosMuertosVulnerables = 0, nuevosInfectados = 0, nuevosInfectadosVulnerables = 0;
        
        System.out.println("Inicio del país " + nomPais + ": ");
        System.out.println("PoblacionTotal: " + poblacion);
        System.out.println("Poblacion Sana: " + poblacionSana);
        System.out.println("Poblacion Sana Vulnerable: " + poblacionSanaVulnerable);
        System.out.println("Poblacion infectada: " + poblacionInfectada);
        System.out.println("Poblacion infectada vulnerable: " + poblacionInfectadaVulnerable);
        System.out.println("");

        while (((poblacion) > poblacionAislamiento) && Worker.isInterrupted() == false) {
            if ((poblacionInfectada * TASA_MORTALIDAD) < 1 && (poblacionInfectada * TASA_MORTALIDAD) > 0) {
                nuevosMuertos = 1;
            } else if ((poblacionInfectada * TASA_MORTALIDAD) < 0) {
                nuevosMuertos = 0;
            } else {
                nuevosMuertos = (long) (poblacionInfectada * TASA_MORTALIDAD);
            }

            if ((poblacionInfectadaVulnerable * TASA_MORTALIDAD_VULNERABLES) < 1 && (poblacionInfectadaVulnerable * TASA_MORTALIDAD_VULNERABLES) > 0) {
                nuevosMuertosVulnerables = 1;
            } else if ((poblacionInfectadaVulnerable * TASA_MORTALIDAD_VULNERABLES) < 0) {
                nuevosMuertosVulnerables = 0;
            } else {
                nuevosMuertosVulnerables = (long) (poblacionInfectadaVulnerable * TASA_MORTALIDAD_VULNERABLES);
            }
            poblacionVulnerable -= nuevosMuertosVulnerables;
            poblacion = poblacion - nuevosMuertos - nuevosMuertosVulnerables;
            poblacionInfectada -= (nuevosMuertos);
            poblacionInfectadaVulnerable -= nuevosMuertosVulnerables;

            if ((poblacionSana + poblacionSanaVulnerable) > poblacionAislamiento) {
                nuevosInfectados = (long) ((poblacionInfectada * TASA_TRANSMISION) + (poblacionInfectadaVulnerable * TASA_TRANSMISION));
                nuevosInfectadosVulnerables = (long) ((poblacionInfectadaVulnerable * TASA_TRANSMICION_VULNERABLES) + (poblacionInfectada * TASA_TRANSMICION_VULNERABLES));

                for (Pais vA : vecinosAereos) {
                    nuevosInfectados += (long) ((vA.poblacionInfectada + vA.poblacionInfectadaVulnerable) * TASA_TRANSMISION_AEREA);
                    nuevosInfectadosVulnerables += (long) ((vA.poblacionInfectada + vA.poblacionInfectadaVulnerable) * TASA_TRANSMISION_AEREA_VULNERABLES);
                }

                for (Pais vT : vecinosTerrestres) {
                    nuevosInfectados += (long) ((vT.poblacionInfectada + vT.poblacionInfectadaVulnerable) * TASA_TRANSMISION_TERRESTRE);
                    nuevosInfectadosVulnerables += (long) ((vT.poblacionInfectada + vT.poblacionInfectadaVulnerable) * TASA_TRANSMISION_TERRESTRE_VULNERABLES);
                }

                if (((poblacionSanaVulnerable - nuevosInfectadosVulnerables) + poblacionSana) < poblacionAislamiento) {
                    nuevosInfectadosVulnerables = (poblacionSanaVulnerable + poblacionSana) - poblacionAislamiento;
                }
                if ((poblacionSanaVulnerable - nuevosInfectadosVulnerables) < 0 || nuevosInfectadosVulnerables < 0) {
                    nuevosInfectadosVulnerables = poblacionSanaVulnerable;
                }
                poblacionSanaVulnerable -= nuevosInfectadosVulnerables;
                poblacionInfectadaVulnerable += nuevosInfectadosVulnerables;

                if (((poblacionSana - nuevosInfectados) + poblacionSanaVulnerable) < poblacionAislamiento) {
                    nuevosInfectados = (poblacionSanaVulnerable + poblacionSana) - poblacionAislamiento;
                }
                if ((poblacionSana - nuevosInfectados) < 0 || nuevosInfectados < 0) {
                    nuevosInfectados = poblacionSana;
                }
                poblacionSana -= nuevosInfectados;
                poblacionInfectada += nuevosInfectados;
            } else {
                nuevosInfectados = 0;
                nuevosInfectadosVulnerables = 0;
            }

            cargar();

            iteraciones++;
            cont++;

            //En cada iteración, compruebo que no hayan interrumpido al hilo y cada 10 iteraciones notifico mi estado
            if (Worker.isInterrupted() == false && cont == 10) {
                System.out.println("\n\nPoblacion actual: " + poblacion);
                System.out.println("Poblacion Sana: " + poblacionSana);
                System.out.println("Poblacion Sana Vulnerable: " + poblacionSanaVulnerable);
                System.out.println("nuevosM: " + nuevosMuertos);
                System.out.println("nuevosMV: " + nuevosMuertosVulnerables);
                System.out.println("nuevosI: " + nuevosInfectados);
                System.out.println("nuevosIV: " + nuevosInfectadosVulnerables);
                System.out.println("Poblacion infectada: " + poblacionInfectada);
                System.out.println("Poblacion infectada vulnerable: " + poblacionInfectadaVulnerable);
                gui.actualizarPais(this);
                notificarEstado();
                gui.escribirEstado("Pais "+ this.getNomPais()+" notificó estado a los vecinos !!! ");
                cont = 0;
            }
        }

        if (Worker.isInterrupted()) {
            System.out.println("Se pausó la simulacion del país: " + nomPais + "!!!!!!!!!!!!!!!!!!!!!!!!!!!!");

        } else {
            System.out.println("\n\nResultado Final del pais " + nomPais + ":");
            System.out.println("Poblacion actual: " + poblacion);
            System.out.println("Poblacion Sana: " + poblacionSana);
            System.out.println("Poblacion Sana Vulnerable: " + poblacionSanaVulnerable);
            System.out.println("Poblacion infectada: " + poblacionInfectada);
            System.out.println("Poblacion infectada vulnerable: " + poblacionInfectadaVulnerable);
            gui.actualizarPais(this);
            gui.escribirEstado("El pais "+ nomPais+ " ha terminado su simulación !!!");
        }

    }

    public void cerrarSocket() {
        try {
            this.serverS.close();
        } catch (IOException ex) {
            Logger.getLogger(Pais.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public boolean isActivo() {
        return activo;
    }

    public String getNomPais() {
        return nomPais;
    }

    public void setNomPais(String nomPais) {
        this.nomPais = nomPais;
    }

    public long getPoblacion() {
        return poblacion;
    }

    public void setPoblacion(long Poblacion) {
        this.poblacion = Poblacion;
    }

    public float getPorcentAislamiento() {
        return porcentAislamiento;
    }

    public int getPuertoPaises() {
        return puertoPaises;
    }

    public void setPorcentAislamiento(float PorcentAislamiento) {
        this.porcentAislamiento = PorcentAislamiento;
    }

    public float getPorcentajePoblaInfec() {
        return porcentajePoblaInfec;
    }

    public void setPorcentajePoblaInfec(float PorcentajePoblaInfec) {
        this.porcentajePoblaInfec = PorcentajePoblaInfec;
    }

    public float getPorcentPoblaVulne() {
        return porcentPoblaVulne;
    }

    public void setPorcentPoblaVulne(float PorcentPoblaVulne) {
        this.porcentPoblaVulne = PorcentPoblaVulne;
    }

    public ArrayList<Pais> getVecinosAereos() {
        return vecinosAereos;
    }

    public void setVecinosAereos(ArrayList<Pais> VecinosAereos) {
        this.vecinosAereos.addAll(VecinosAereos);
    }

    public ArrayList<Pais> getVecinosTerrestres() {
        return vecinosTerrestres;
    }

    public void setVecinosTerrestres(ArrayList<Pais> VecinosTerrestres) {
        this.vecinosTerrestres.addAll(VecinosTerrestres);
    }

}//end Pais

