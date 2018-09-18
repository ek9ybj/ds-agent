package agent;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class AgentMain {

    private static List<AgentThread> agentThreads = new ArrayList<>();
    protected static int n;
    protected static int m;
    protected static int t1;
    protected static int t2;
    protected static List<String> secretsOfOne = new ArrayList<>();
    protected static List<String> secretsOfTwo = new ArrayList<>();
    protected volatile static boolean over;

    public static void main(String[] args) {
        if(args.length < 4) {
            System.out.println("Required arguments: <n> <m> <t1> <t2>");
        } else {
            n = Integer.parseInt(args[0]);
            m = Integer.parseInt(args[1]);
            t1 = Integer.parseInt(args[2]);
            t2 = Integer.parseInt(args[3]);

            loadAgents(1, n);
            loadAgents(2, m);

            for(AgentThread a : agentThreads) {
                System.out.println(a.toString());
                Thread t = new Thread(a);
                t.start();
            }
        }
    }

    private static void loadAgents(int agency, int amount) {
        for (int i = 1; i <= amount; ++i) {
            try(Scanner input = new Scanner(new File("agent" + agency + "-" + i + ".txt"))) {
                String line = input.nextLine();
                String secret = input.nextLine();
                List<String> names = Arrays.asList(line.split(" "));
                agentThreads.add(new AgentThread(agency, i, names, secret));
                if(agency == 1) {
                    secretsOfOne.add(secret);
                } else {
                    secretsOfTwo.add(secret);
                }
            } catch(IOException e) {
                e.printStackTrace();
            }
        }
    }

    protected static void checkEndConditions() {
        if(over) return;
        int arrestedOne = 0;
        int arrestedTwo = 0;
        List<String> knownOne = new ArrayList<>();
        List<String> knownTwo = new ArrayList<>();
        for(AgentThread a : agentThreads) {
            if(a.agency == 1) {
                if(a.arrested) {
                    ++arrestedOne;
                }
                synchronized (a.secrets) {
                    for(String s : a.secrets) {
                        if(!knownOne.contains(s)) {
                            knownOne.add(s);
                        }
                    }
                }
            } else {
                if(a.arrested) {
                    ++arrestedTwo;
                }
                synchronized (a.secrets) {
                    for(String s : a.secrets) {
                        if(!knownTwo.contains(s)) {
                            knownTwo.add(s);
                        }
                    }
                }
            }
        }
        System.out.println("[CHECKING] One: [Arrested: " + arrestedOne + ", Secrets: " + knownOne + "] Two: [Arrested: " + arrestedTwo + ", Secrets: " + knownTwo + "]");
        if(arrestedTwo == m || knownOne.containsAll(secretsOfTwo)) {
            over = true;
            try {
                Thread.sleep(t2+200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("---------------------------");
            System.out.println("AGENCY ONE HAS WON THE GAME");
        }
        if(arrestedOne == n || knownTwo.containsAll(secretsOfOne)) {
            over = true;
            try {
                Thread.sleep(t2+200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("---------------------------");
            System.out.println("AGENCY TWO HAS WON THE GAME");
        }
    }
}
