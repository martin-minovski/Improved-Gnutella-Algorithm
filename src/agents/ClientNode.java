package agents;

import static agents.ManagerNode.frame;
import java.lang.Thread;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.*;
import jade.lang.acl.*;
import java.util.*;
import java.util.Random;
import services.MessageParser;
import services.MessageSender;
import structs.*;

public class ClientNode extends Agent {

    Random rand = new Random();
    public boolean isSuperNode = true;
    ArrayList<structs.SuperNodeStruct> neighbours = new ArrayList<>();
    ArrayList<ServantStruct> servants = new ArrayList<>();
    public String parentSuperNode;
    public boolean connected = false;
    int currentHostCacheNumber = 0;
    long bandwidth = 5000;               // SHOULD BE AGENT ARGUMENT
    ArrayList<String> files;
    ArrayList<String> filesWanted;
    String filesString;
    
    
    //Statistics vars:
    int pingsReceived = 0;
    int pingsForwarded = 0;
    int pongsForwarded = 0;
    int fileRequestsInitiated = 0;
    int fileRequestsForwarded = 0;
    int fileHitsForwarded = 0;
    int fileHitsReceived = 0;
    int totalMessagesReceived = 0;

    void printStatistics() {
        String superNode = "";
        if (!isSuperNode) {
            superNode = "(my supernode is " + parentSuperNode + ")";
        }
        ManagerNode.addToOutput(getLocalName() + " Statistics " + superNode + ": " + 
                "\n\t" + pingsReceived + " pings received," + 
                "\n\t" + pingsForwarded + " pings forwarded," + 
                "\n\t" + pongsForwarded + " piongs forwarded," + 
                "\n\t" + fileRequestsInitiated + " file requests initiated," + 
                "\n\t" + fileRequestsForwarded + " file requests forwarded," + 
                "\n\t" + fileHitsForwarded + " file hits forwarded," + 
                "\n\t" + fileHitsReceived + " file hits received." + 
                "\n\t" + "Total messages received: " +  totalMessagesReceived
                );
    }
    
    void addNeighbour(String name) {

        neighbours.add(new SuperNodeStruct(name));
        frame.addVertex(getLocalName(), name.split("@")[0], "");

        String shout = getLocalName() + ": **UPDATED NEIGHBOURS (" + neighbours.size() + "): ";
        Iterator<SuperNodeStruct> i = neighbours.iterator();
        while (i.hasNext()) {
            shout += i.next().guid + ",  ";
        }
        if (ManagerNode.logNeighbourTables) {
            ManagerNode.addToOutput(shout);
        }



    }

    void tryConnect() {
        //This randomizes host cache selection for connect attempt
        if (currentHostCacheNumber < ManagerNode.hostCaches.length - 1) {
            currentHostCacheNumber++;
        } else {
            currentHostCacheNumber = 0;
        }

        send(MessageSender.SendMessage(ManagerNode.hostCachesGUIDs[currentHostCacheNumber], "CON_REQ;" + bandwidth));
    }

    void connectionAcknowledged(String[] msg) {
        String nodeType = msg[1];
        connected = true;
        if (nodeType.equals("SUP")) {
            String neighbour = msg[2];
            if (ManagerNode.logDetailConversation) {
                ManagerNode.addToOutput(getLocalName() + ": Connected! I am supernode and my neighbour is " + neighbour);
            }
            ping(neighbour);
        }
        if (nodeType.equals("SER")) {
            parentSuperNode = msg[2];
            isSuperNode = false;
            if (ManagerNode.logDetailConversation) {
                ManagerNode.addToOutput(getLocalName() + ": Connected! I am servant and my parent is " + parentSuperNode);
            }

            registerWithParent();
        }
        if (nodeType.equals("ONLY_SUP")) {
            if (ManagerNode.logDetailConversation) {
                ManagerNode.addToOutput(getLocalName() + ": Connected! I am the only supernode in the network!");
            }
        }
    }

    void registerWithParent() {
        send(MessageSender.SendMessage(parentSuperNode, "REG;" + getName() + ";" + filesString));
    }

    void ping(String nodeID) {
        String message = "PING;" + ManagerNode.pingTTL + ";" + getName();
        send(MessageSender.SendMessage(nodeID, message));
        if (ManagerNode.logDetailConversation) {
            ManagerNode.addToOutput(getLocalName() + ": Initiating ping to " + nodeID);
        }
    }

    void processPing(String[] msg) {

        int TTL = Integer.parseInt(msg[1]);
        String[] path = msg[2].split(",");
        String originator = path[0];
        TTL--;

        pingsReceived++;

        //First check if ping originator is already a neighbour
        boolean existing = false;
        Iterator<SuperNodeStruct> i = neighbours.iterator();
        while (i.hasNext()) {
            SuperNodeStruct neighbour = i.next();
            if (neighbour.guid.equals(originator)) {
                existing = true;
            }
        }

        if (!existing) {

            send(MessageSender.SendMessage(path[path.length - 1], "PONG;" + getName() + ";" + msg[2]));

            if (TTL > 0) {
                i = neighbours.iterator();
                while (i.hasNext()) {
                    SuperNodeStruct neighbour = i.next();
                    send(MessageSender.SendMessage(neighbour.guid, "PING;" + TTL + ";" + msg[2] + "," + getName()));

                    pingsForwarded++;
                }
            }

            if (neighbours.size() < ManagerNode.maxNeighbours) {
                addNeighbour(originator);
                if (ManagerNode.logDetailConversation) {
                    ManagerNode.addToOutput(getLocalName() + ": Received ping message: " + msg[2] + ". Added originator to neighbours. Ponging!");
                }
            } else {
                if (ManagerNode.logDetailConversation) {
                    ManagerNode.addToOutput(getLocalName() + ": Received ping message: " + msg[2] + ".");
                }
            }


        } else {

            if (ManagerNode.logDetailConversation) {
                ManagerNode.addToOutput(getLocalName() + ": Received ping, but initiator (" + originator + ") is already in my list of neighbours.");
            }
        }
    }

    void processPong(String[] msg) {

        String sender = msg[1];
        String pathstring = msg[2];

        if (pathstring.equals(getName())) {
            //Pong received
            if (neighbours.size() < ManagerNode.maxNeighbours) {
                addNeighbour(sender);
                if (ManagerNode.logDetailConversation) {
                    ManagerNode.addToOutput(getLocalName() + ": Received final pong message. Adding " + sender + " to neighbours.");
                }
            } else {
                if (ManagerNode.logDetailConversation) {
                    ManagerNode.addToOutput(getLocalName() + ": Received pong from " + sender + " but maximum neighbour number reached.");
                }
            }

        } else {
            String[] path = pathstring.split(",");
            String trimmedMsg = pathstring;
            if (pathstring.lastIndexOf(",") != -1) {
                trimmedMsg = pathstring.substring(0, pathstring.lastIndexOf(","));
            }

            String message = "PONG;" + sender + ";" + trimmedMsg;
            String nexthop = path[path.length - 2];
            send(MessageSender.SendMessage(nexthop, message));
            pongsForwarded++;
            if (ManagerNode.logDetailConversation) {
                ManagerNode.addToOutput(getLocalName() + ": Received pong message: " + pathstring + " from " + sender + ". Passing pong to " + nexthop + ". The message is " + message);
            }
        }
    }

    void requestFiles() {
        for (int i = 0; i < filesWanted.size(); i++) {

            if (ManagerNode.logFileExchange) {
                ManagerNode.addToOutput(getLocalName() + ": Requesting file " + filesWanted.get(i));
            }
            if (isSuperNode) {
                Iterator<SuperNodeStruct> j = neighbours.iterator();
                while (j.hasNext()) {
                    send(MessageSender.SendMessage(j.next().guid, "FILE_REQ;" + filesWanted.get(i) + ";" + ManagerNode.fileRequestTTL + ";" + getName()));
                    fileRequestsInitiated++;
                }
            } else {
                send(MessageSender.SendMessage(parentSuperNode, "FILE_REQ;" + filesWanted.get(i) + ";" + ManagerNode.fileRequestTTL + ";" + getName()));
                fileRequestsInitiated++;
            }
        }
    }

    void processFileRequest(String[] msg) {
        String filename = msg[1];
        int TTL = Integer.parseInt(msg[2]);
        String[] path = msg[3].split(",");
        //String originator = path[0];
        TTL--;

        //If node has the file, return a FILE HIT message
        for (int i = 0; i < files.size(); i++) {
            if (filename.equals(files.get(i))) {
                send(MessageSender.SendMessage(path[path.length - 1], "FILE_HIT;" + filename + ";" + msg[3] + ";" + msg[3] + "," + getName()));
                fileRequestsForwarded++;
                if (ManagerNode.logFileExchange) {
                    ManagerNode.addToOutput(getLocalName() + ": Query received for file " + filename + ". I have it, so reply with query hit.");
                }
                return;
            }
        }

        //If TTL has expired, drop message
        if (TTL == 0) {
            return;
        }

        if (isSuperNode) {

            //Check if any of the servants has the file. If so, forward request to that servant
            for (int i = 0; i < servants.size(); i++) {
                if (servants.get(i).checkIfHasFile(filename)) {
                    send(MessageSender.SendMessage(servants.get(i).guid, "FILE_REQ;" + filename + ";" + TTL + ";" + msg[3] + "," + getName()));
                    if (ManagerNode.logFileExchange) {
                        ManagerNode.addToOutput(getLocalName() + ": Local servant " + servants.get(i).guid + " has the file " + filename);
                    }
                    return;
                }
            }

            //If not, forward the flooded request to neighbouring supernodes
            for (int i = 0; i < neighbours.size(); i++) {
                SuperNodeStruct neighbour = neighbours.get(i);
                if (!neighbour.guid.equals(path[path.length - 1])) {
                    send(MessageSender.SendMessage(neighbours.get(i).guid, "FILE_REQ;" + filename + ";" + TTL + ";" + msg[3] + "," + getName()));

                    if (ManagerNode.logFileExchange) {
                        //ManagerNode.addToOutput(getLocalName() + ": Query received for file " + filename + ". I don't have it and neither do my servants, so broadcast query.");
                    }
                }
            }

        }

    }

    void processFileHit(String[] msg) {

        String filename = msg[1];
        String pathstring = msg[2];

        if (pathstring.equals(getName())) {
            fileHitsReceived++;
            //File hit message is intended for this node
            for (int i = 0; i < filesWanted.size(); i++) {
                if (filesWanted.get(i).equals(filename)) {
                    filesWanted.remove(i);
                    files.add(filename);
                    if (ManagerNode.logFileExchange || ManagerNode.logFileReceive) {
                        ManagerNode.addToOutput(getLocalName() + ": Received file " + filename + ". Hooray! Path: " + msg[3]);
                    }
                    return;
                }
            }
            if (ManagerNode.logFileExchange) {
                ManagerNode.addToOutput(getLocalName() + ": I already have " + filename + ". Thanks anyway");
            }

        } else {
            String[] path = pathstring.split(",");
            String trimmedPath = pathstring;
            if (pathstring.lastIndexOf(",") != -1) {
                trimmedPath = pathstring.substring(0, pathstring.lastIndexOf(","));
            }

            String message = "FILE_HIT;" + filename + ";" + trimmedPath + ";" + msg[3];
            String nexthop = path[path.length - 2];

            if (ManagerNode.logFileExchange) {
                ManagerNode.addToOutput(getLocalName() + ": Passing query hit to " + nexthop);
            }

            send(MessageSender.SendMessage(nexthop, message));
            fileHitsForwarded++;
        }
    }

    @Override
    protected void setup() {
        Object[] args = getArguments();
        bandwidth = Long.parseLong((String) args[0]);
        filesString = ((String) args[1]).split("/")[0];
        //Populate file array lists
        files = new ArrayList<>(Arrays.asList(filesString.split(",")));
        filesWanted = new ArrayList<>(Arrays.asList(((String) args[1]).split("/")[1].split(",")));


        //Update visualization
        if (bandwidth >= 2097152) {
            frame.insertNode(getLocalName());
        }

        //Connect to network
        try {
            if (ManagerNode.logDetailConversation) {
                ManagerNode.addToOutput(getLocalName() + ": Trying to connect...");
            }
            tryConnect();
        } catch (Exception e) {
        }

        addBehaviour(new CyclicBehaviour(this) {
            @Override
            public void action() {

                ACLMessage incoming = blockingReceive();
                if (incoming != null) {
                    totalMessagesReceived++;
                    
                    String[] msg = MessageParser.splitMessage(incoming);
                    String command = msg[0];

                    if (command.equals("CON_ACK")) {
                        connectionAcknowledged(msg);
                    }

                    if (command.equals("REG")) {
                        //Simply add servant to struct.
                        if (isSuperNode) {
                            servants.add(new ServantStruct(msg[1], getName(), msg[2]));
                        }
                    }

                    if (command.equals("PING")) {
                        processPing(msg);
                    }

                    if (command.equals("PONG")) {
                        processPong(msg);
                    }

                    if (command.equals("FILE_REQ")) {
                        processFileRequest(msg);
                    }


                    if (command.equals("FILE_HIT")) {
                        processFileHit(msg);
                    }


                    if (command.equals("REQ_TRIG")) {
                        requestFiles();
                    }

                    if (command.equals("DISPLAY_STATISTICS")) {
                        printStatistics();
                    }
                    
                }
            }
        });

    }
}
