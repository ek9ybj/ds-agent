package agent;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.*;

public class AgentThread implements Runnable {
    protected int agency;
    protected List<String> names;
    protected String secret;
    private Thread contactThread;
    protected int port;
    protected int number;
    protected List<String> secrets = new ArrayList<>();
    private List<String> spoiled = new ArrayList<>();
    protected volatile boolean arrested = false;


    public AgentThread(int agency, int number, List<String> names, String secret) {
        this.agency = agency;
        this.names = names;
        this.secret = secret;
        this.number = number;
        secrets.add(secret);
    }

    public String toString() {
        return "Agency: " + agency + ", Secret: '" + secret + "', Names: " + names;
    }

    public void run() {
        Random random = new Random();
        while (!arrested && !AgentMain.over) {
            port = random.nextInt(101) + 20000;
            try (ServerSocket server = new ServerSocket(port)) {
                startContactThread();
                server.setSoTimeout(AgentMain.t2);
                System.out.println(nameTag() + "listening on " + port);
                Socket client = server.accept();
                System.out.println(nameTag() + "someone connected on " + port);
                try (Scanner clientIn = new Scanner(client.getInputStream());
                     PrintWriter clientOut = new PrintWriter(client.getOutputStream(), true);
                ) {
                    String name = names.get(random.nextInt(names.size()));
                    clientOut.println(name); // out 1
                    //System.out.println("sent 1: " + name);
                    System.out.println(nameTag() + "responded with name " + name);
                    int guess = clientIn.nextInt(); // in 2
                    clientIn.nextLine();
                    //System.out.println("recv 2: " + guess);
                    if (guess == agency) {
                        System.out.println(nameTag() + "someone guessed agency (" + agency + ") " + guess + " correctly");
                        clientOut.println("OK"); // out 3
                        //System.out.println("sent 3: OK");
                        String response = clientIn.nextLine(); // in 4
                        //System.out.println("recv 4: " + response);
                        if (response.equals("OK")) {
                            String receivedSecret = clientIn.nextLine(); // in 5
                            //System.out.println("recv 5: " + receivedSecret);
                            addToSecrets(receivedSecret);
                            clientOut.println(secret); // out 6
                            //System.out.println("sent 6: " + secret);
                            System.out.println(nameTag() + "same agency, received secret '" + receivedSecret + "' sent secret '" + secret + "'");
                            System.out.println(nameTag() + "known secrets: " + secrets.toString());
                        } else {
                            int receivedNumber = clientIn.nextInt(); // in 5
                            //System.out.println("recv 5: " + receivedNumber);
                            if (receivedNumber == number) {
                                System.out.println(nameTag() + "someone guessed the number, sharing a secret..");
                                boolean good = false;
                                String spoil = "";
                                while (!good) {
                                    spoil = secrets.get(random.nextInt(secrets.size()));
                                    if (!spoiled.contains(spoil)) {
                                        good = true;
                                    }
                                }
                                spoiled.add(spoil);
                                clientOut.println(spoil); // out 6
                                //System.out.println("sent 6: " + spoil);
                                System.out.println(nameTag() + "spoiled secrets: " + spoiled.toString());
                                if (secrets.size() == spoiled.size()) {
                                    System.out.println(nameTag() + "spoiled every known secret, got arrested");
                                    arrested = true;
                                    AgentMain.checkEndConditions();
                                }
                            } else {
                                System.out.println(nameTag() + "someone guessed the wrong number (" + receivedNumber + "), closing connection");
                                client.close();
                            }
                        }
                    } else {
                        System.out.println(nameTag() + "someone guessed agency " + guess + " incorrectly, closing connection");
                        client.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } catch (SocketTimeoutException e) {
                if(!AgentMain.over) {
                    System.out.println(nameTag() + "no one connected on " + port + ", trying another..");
                }
            } catch (BindException e) {
                System.out.println(nameTag() + "port " + port + " was in use, finding a new..");
            } catch (IOException e) {
                e.printStackTrace();
            }
            AgentMain.checkEndConditions();
        }
    }

    private String nameTag() {
        return "[" + names.get(0) + "] ";
    }

    private void startContactThread() {
        if (contactThread == null) {
            contactThread = new Thread(new AgentContactThread(this));
            contactThread.start();
            System.out.println(nameTag() + "started contact thread");
        }
    }

    protected  synchronized void addToSecrets(String secret) {
        if (!secrets.contains(secret)) {
            secrets.add(secret);
        }
    }
}
