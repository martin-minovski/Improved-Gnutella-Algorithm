/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package services;

import jade.core.AID;
import jade.lang.acl.*;

/**
 *
 * @author Martin
 */
public class MessageSender {

    public static ACLMessage SendMessage(String recipient, String message) {
        ACLMessage response = new ACLMessage(ACLMessage.INFORM);
        response.setContent(message);
        response.addReceiver(new AID(recipient, AID.ISGUID));
        return response;
    }
}
