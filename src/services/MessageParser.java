/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package services;

import jade.lang.acl.*;

/**
 *
 * @author Martin
 */
public class MessageParser {
    public static String[] splitMessage(ACLMessage msg)
    {
        if (msg  == null) return null;
        return msg.getContent().split(";");
    }
}
