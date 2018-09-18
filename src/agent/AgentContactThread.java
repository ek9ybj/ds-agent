package agent;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.Socket;
import java.util.*;

public class AgentContactThread implements Runnable {

    private AgentThread agent;
    private Map<String, Integer> knownAgents = new HashMap<>();
    private Map<String, Integer> knownNumbers = new HashMap<>();
    private Map<String, List<Integer>> triedNumbers = new HashMap<>();

    public AgentContactThread(AgentThread agent) {
        this.agent = agent;
    }

    public void run() {
        while(!agent.arrested && !AgentMain.over) {
            Random random = new Random();
            int time = random.nextInt(AgentMain.t2 - AgentMain.t1 + 1) + AgentMain.t1;
            try {
                Thread.sleep(time);
                boolean found = false;
                while(!found) {
                    int port = random.nextInt(101) + 20000;
                    if(port != agent.port) {
                        found = true;
                        try(Socket server = new Socket("localhost", port);
                            Scanner clientIn = new Scanner(server.getInputStream());
                            PrintWriter clientOut = new PrintWriter(server.getOutputStream(), true);
                            ) {
                            String name = clientIn.nextLine(); // in 1
                            //System.out.println("recv 1: " + name);
                            System.out.println(nameTag() + "has connected to " + name);
                            Integer guess = knownAgents.get(name);
                            System.out.println(nameTag() + "known names: " + knownAgents.toString());
                            if (guess == null) {
                                System.out.println(nameTag() + name + " wasn't known, guessing agency");
                                guess = random.nextInt(2) + 1;
                                clientOut.println(guess); // out 2
                                //System.out.println("sent 2: " + guess);
                            } else {
                                System.out.println(nameTag() + name + " was known, sending correct agency");
                                clientOut.println(guess); // out 2
                                //System.out.println("sent 2: " + guess);
                            }
                            String response = clientIn.nextLine(); // in 3
                            //System.out.println("recv 3: " + response);
                            if (response.equals("OK")) {
                                System.out.println(nameTag() + "correct agency was sent");
                                knownAgents.put(name, guess);
                            }
                            if(guess == agent.agency) {
                                clientOut.println("OK"); // out 4
                                //System.out.println("sent 4: OK");
                                System.out.println(nameTag() + "same agency, exchanging secrets");
                                clientOut.println(agent.secret); // out 5
                                //System.out.println("sent 5: " + agent.secret);
                                String secret = clientIn.nextLine(); // in 6
                                //System.out.println("recv 6: : " + secret);
                                agent.addToSecrets(secret);
                                System.out.println(nameTag() + "known secrets: " + agent.secrets.toString());
                            } else {
                                clientOut.println("???"); // out 4
                                //System.out.println("sent 4: ???");
                                System.out.println(nameTag() + "different agency, guessing number");
                                System.out.println(nameTag() + "known numbers: " + knownNumbers.toString());
                                Integer known = knownNumbers.get(name);
                                int number = 0;
                                List<Integer> tries = triedNumbers.get(name);
                                if(known != null) {
                                    number = known.intValue();
                                } else {
                                    if(tries == null) {
                                        tries = new ArrayList<>();
                                    }
                                    boolean good = false;
                                    while(!good) {
                                        int n;
                                        if (agent.agency == 0) {
                                            n = AgentMain.m;
                                        } else {
                                            n = AgentMain.n;
                                        }
                                        number = random.nextInt(n) + 1;
                                        if(!tries.contains(number)) {
                                            good = true;
                                        }
                                    }
                                }
                                clientOut.println(number); // out 5
                                //System.out.println("sent 5: " + number);
                                String secret = clientIn.nextLine(); // in 6
                                //System.out.println("recv 6: " + secret);
                                tries.add(number);
                                triedNumbers.put(name, tries);
                                agent.addToSecrets(secret);
                                System.out.println(nameTag() + "number was guessed correctly, received secret");
                                knownNumbers.put(name, number);
                                System.out.println(nameTag() + "known numbers: " + knownNumbers.toString());
                                System.out.println(nameTag() + "known secrets: " + agent.secrets.toString());
                            }
                        } catch (NoSuchElementException e) {
                            System.out.println(nameTag() + "incorrect information was sent, the server closed the connection");
                        } catch (ConnectException e) {
                            if(!AgentMain.over) {
                                System.out.println(nameTag() + "no one was listening on " + port + ", trying another..");
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            AgentMain.checkEndConditions();
        }
    }

    private String nameTag() {
        return "[" + agent.names.get(0) + "] ";
    }
}
