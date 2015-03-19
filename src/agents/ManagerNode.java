package agents;

import jade.core.Agent;
import jade.core.behaviours.*;
import jade.lang.acl.*;
import jade.wrapper.*;
import java.util.Random;
import frames.MainFrame;
import java.io.*;
import java.util.ArrayList;
import javax.swing.JFrame;
import services.MessageSender;

public class ManagerNode extends Agent {

    public static final String[] hostCaches = {"HC1", "HC2", "HC3", "HC4", "HC5"};
    public static int pingTTL = 3;
    public static int fileRequestTTL = 4;
    public static int maxNeighbours = 4;
    public static boolean logHostCacheConv = false;
    public static boolean logDetailConversation = false;
    public static boolean logNeighbourTables = false;
    public static boolean logFileExchange = false;
    public static boolean logFileReceive = false;
    public static boolean displayIndividualStatistics = false;
    public static ArrayList<String> nodeNames = new ArrayList<>();
    public static ArrayList<String> nodeBandwidths = new ArrayList<>();
    public static ArrayList<String> nodeFiles = new ArrayList<>();
    public static int delayBetweenNodeCreate = 100;
    public static String[] hostCachesGUIDs = new String[5];
    public static MainFrame frame = new MainFrame();
    Random rand = new Random();
    private ArrayList<String> nodeFullNames = new ArrayList<>();
    public static String output = "";

    public static void addToOutput(String string) {
        output += string + "\n";
    }

    static void saveOutput() {
        try {
            FileOutputStream out = new FileOutputStream("output.txt");
            out.write(output.getBytes());
            out.close();
        } catch (IOException e) {
            System.out.println("An error occurred: " + e.getLocalizedMessage());
            System.exit(1);
        }
    }

    @Override
    protected void setup() {




        Object[] args = getArguments();

        String config = (String) args[1];
        try (BufferedReader br = new BufferedReader(new FileReader(config))) {
            String line = br.readLine();
            int counter = 0;
            while (line != null) {
                String param = line.split(" ")[1];
                switch (counter) {
                    case 0:
                        if (param.equals("true")) {
                            logHostCacheConv = true;
                        }
                        break;
                    case 1:
                        if (param.equals("true")) {
                            logDetailConversation = true;
                        }
                        break;
                    case 2:
                        if (param.equals("true")) {
                            logNeighbourTables = true;
                        }
                        break;
                    case 3:
                        if (param.equals("true")) {
                            logFileExchange = true;
                        }
                        break;
                    case 4:
                        if (param.equals("true")) {
                            logFileReceive = true;
                        }
                        break;
                    case 5:
                        pingTTL = Integer.parseInt(param);
                        break;
                    case 6:
                        fileRequestTTL = Integer.parseInt(param);
                        break;
                    case 7:
                        maxNeighbours = Integer.parseInt(param);
                        break;
                    case 8:
                        delayBetweenNodeCreate = Integer.parseInt(param);
                        break;
                    case 9:
                        if (param.equals("true")) {
                            displayIndividualStatistics = true;
                        }
                        break;
                }
                line = br.readLine();
                counter++;
            }
        } catch (Exception e) {
        }

        String filePath = (String) args[0];
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line = br.readLine();
            while (line != null) {
                String[] node = line.split(";");
                nodeNames.add(node[0]);
                nodeBandwidths.add(node[1]);
                nodeFiles.add(node[2]);

                line = br.readLine();
            }
        } catch (Exception e) {
        }


        //Boot host caches
        for (int i = 0; i < hostCaches.length; i++) {

            AgentContainer c = getContainerController();
            try {
                AgentController a = c.createNewAgent(hostCaches[i], "agents.HostCache", null);
                a.start();
                hostCachesGUIDs[i] = a.getName();
            } catch (Exception e) {
            }
        }



        //Boot nodes
        for (int i = 0; i < nodeNames.size(); i++) {

            AgentContainer c = getContainerController();
            try {
                AgentController a = c.createNewAgent(nodeNames.get(i), "agents.ClientNode", new String[]{
                    nodeBandwidths.get(i),
                    nodeFiles.get(i)
                });
                a.start();
                //Add full name to list
                nodeFullNames.add(a.getName());

                Thread.sleep(delayBetweenNodeCreate);
            } catch (Exception e) {
            }
        }

        try {
            Thread.sleep(2000);

            //Start file request flood
            for (int i = 0; i < nodeNames.size(); i++) {
                send(MessageSender.SendMessage(nodeFullNames.get(i), "REQ_TRIG;"));

                Thread.sleep(500);
            }
        } catch (Exception e) {
        }



        if (displayIndividualStatistics) {

            try {
                Thread.sleep(2000);
                //Display statistics
                for (int i = 0; i < nodeNames.size(); i++) {
                    send(MessageSender.SendMessage(nodeFullNames.get(i), "DISPLAY_STATISTICS;"));
                }
            } catch (Exception e) {
            }
        }

        try {
            Thread.sleep(2000);
        } catch (Exception e) {
        }
        
        saveOutput();
        
        
        frame.render();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1024, 900);
        frame.setVisible(true);

    }
}
