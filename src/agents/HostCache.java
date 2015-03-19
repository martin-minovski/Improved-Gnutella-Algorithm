package agents;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.*;
import jade.lang.acl.*;
import java.util.*;
import java.util.Random;
import services.MessageParser;
import services.MessageSender;
import structs.*;

public class HostCache extends Agent {

    ArrayList<structs.SuperNodeStruct> superNodes = new ArrayList<>();
    ArrayList<ServantStruct> servants = new ArrayList<>();
    Random rand = new Random();

    void informOtherHostCaches(String guid, String nodeType, String neighbourOrParent) {
        for (int i = 0; i < ManagerNode.hostCaches.length; i++) {
            //Check all "hardcoded" host caches. If self, don't send message.
            if (!ManagerNode.hostCaches[i].equals(getLocalName())) {
                send(MessageSender.SendMessage(ManagerNode.hostCachesGUIDs[i], "NEW_NODE;" + guid + ";" + nodeType + ";" + neighbourOrParent));
            }
        }
    }

    /**
     * Adds a new node and returns random neighbouring/parent supernode GUID
     *
     * @param guid
     * @param isSuperNode
     * @return
     */
    void addNewNode(String guid, boolean isSuperNode, String neighbourOrParentGUID) {

        if (isSuperNode) {
            superNodes.add(new SuperNodeStruct(guid));
        } else {

            //First check if servant has connected before
            Iterator<ServantStruct> i = servants.iterator();
            while (i.hasNext()) {
                ServantStruct servant = i.next();
                if (servant.guid.equals(guid) && !servant.isOnline) {
                    servant.isOnline = true;
                }
            }

            servants.add(new ServantStruct(guid, neighbourOrParentGUID));

        }


    }

    @Override
    protected void setup() {


        addBehaviour(new CyclicBehaviour(this) {
            @Override
            public void action() {
                ACLMessage incoming = blockingReceive();
                if (incoming != null) {
                    String[] msg = MessageParser.splitMessage(incoming);

                    String senderGUID = incoming.getSender().getName();

                    if (msg[0].equals("CON_REQ")) {

                        //If there are no supernodes..
                        if (superNodes.isEmpty()) {
                            if (Long.parseLong(msg[1]) < 2097152) {
                                //If this node cannot be a supernode, refuse connection
                                send(MessageSender.SendMessage(senderGUID, "CON_REF;NO_SUP"));
                                if (ManagerNode.logHostCacheConv) System.out.println(getLocalName() + ": " + senderGUID + " tried to connect, but no supernodes exist.");
                            } else {
                                //Add to list of known supernodes, but no other supernode to send back
                                superNodes.add(new SuperNodeStruct(senderGUID));
                                send(MessageSender.SendMessage(senderGUID, "CON_ACK;ONLY_SUP"));
                                if (ManagerNode.logHostCacheConv) System.out.println(getLocalName() + ": First SuperNode " + senderGUID + " connected!");
                                informOtherHostCaches(senderGUID, "SUP", "-");
                            }

                        } //Else, proceed normally
                        else {
                            SuperNodeStruct randomSuperNode = superNodes.get(rand.nextInt(superNodes.size()));

                            boolean hasEnoughBandwidth = (Long.parseLong(msg[1]) >= 2097152);

                            String nodeType;
                            if (hasEnoughBandwidth) {
                                nodeType = "SUP";
                            } else {
                                nodeType = "SER";
                            }
                            String responseString = "CON_ACK;" + nodeType + ";" + randomSuperNode.guid;

                            send(MessageSender.SendMessage(senderGUID, responseString));

                            addNewNode(senderGUID, hasEnoughBandwidth, randomSuperNode.guid);

                            if (ManagerNode.logHostCacheConv) System.out.println(getLocalName() + ": " + senderGUID + " connected via me (" + nodeType + "). Neighbor/parent is " + randomSuperNode.guid);

                            informOtherHostCaches(senderGUID, nodeType, randomSuperNode.guid);
                        }
                    }

                    if (msg[0].equals("NEW_NODE")) {
                        addNewNode(msg[1], msg[2].equals("SUP"), msg[3]);
                        //System.out.println(getLocalName() + ": " + senderGUID + " informed me that " + msg[1] + " joined the network (" + msg[2] + ")");
                    }

                }
            }
        });
    }
}
