/**
 *  Copyright 2012-2019 Solace Corporation. All rights reserved.
 *
 *  http://www.solace.com
 *
 *  This source is distributed under the terms and conditions
 *  of any contract or contracts between Solace and you or
 *  your company. If there are no contracts in place use of
 *  this source is not authorized. No support is provided and
 *  no distribution, sharing with others or re-use of this
 *  source is authorized unless specifically stated in the
 *  contracts referred to above.
 */

package com.solace.geek2;

import org.apache.log4j.Logger;

import com.solacesystems.jcsmp.JCSMPException;
import com.solacesystems.jcsmp.JCSMPFactory;
import com.solacesystems.jcsmp.JCSMPProperties;
import com.solacesystems.jcsmp.JCSMPSession;
import com.solacesystems.jcsmp.JCSMPStreamingPublishEventHandler;
import com.solacesystems.jcsmp.TextMessage;
import com.solacesystems.jcsmp.Topic;
import com.solacesystems.jcsmp.XMLMessageProducer;

/**
 * This is the main program of the purchase client. This will likely be just a test and for illustration 
 * purposes, since the final customer-client is to be implemented in a browser or mobile app. 
 * 
 * At this time, this code just sends a purchase message and exists. The final version will stay up and running
 * and be able to receive messages from the PurchaseAgent, LoyaltyAgent and FraudAgent.
 * 
 * @author Mike O'Brien
 *
 */
public class PurchaseClient {
	private static final Logger logger = Logger.getLogger(PurchaseClient.class.getName());
    
    public static void main(String... args) throws JCSMPException {
    	// Check command line arguments
        if (args.length < 8) {
            System.out.println("Usage: PurchaseClient <msg_backbone_ip:port> <vpn> <client-username> <password> <custId> <location> <product> <price>");
            System.exit(-1);
        }
        logger.info("PurchaseClient initializing...");

    	// Create a JCSMP Session
        final JCSMPProperties properties = new JCSMPProperties();
        properties.setProperty(JCSMPProperties.HOST, args[0]);      // msg-backbone ip:port
        properties.setProperty(JCSMPProperties.VPN_NAME, args[1]);  // message-vpn
        properties.setProperty(JCSMPProperties.USERNAME, args[2]);  // client-username (assumes no password)
        properties.setProperty(JCSMPProperties.PASSWORD, args[3]);  // client-username (assumes no password)
        final JCSMPSession session =  JCSMPFactory.onlyInstance().createSession(properties);
        
        String custId = args[4];
        String location = args[5];
        String product = args[6];
        String strPrice = args[7];
        
        String strTopic = "purchase/"+ location + "/" + custId;
        final Topic topic = JCSMPFactory.onlyInstance().createTopic(strTopic);
        
        session.connect();
        /** Anonymous inner-class for handling publishing events */
        XMLMessageProducer prod = session.getMessageProducer(new JCSMPStreamingPublishEventHandler() {
            public void responseReceived(String messageID) {
            	logger.info("Producer received response for msg: " + messageID);
            }
            public void handleError(String messageID, JCSMPException e, long timestamp) {
            	logger.warn("Producer received error");
            }
        });

        TextMessage msg = JCSMPFactory.onlyInstance().createMessage(TextMessage.class);
        msg.setText(generateJsonPayload(product, strPrice));
        logger.info("Connected. About to send message on topic " + topic.getName());
        prod.send(msg,topic);
        
        String confirmationMsg = "Message sent. Exiting."; 
        logger.info(confirmationMsg);
        System.out.println(confirmationMsg);
        session.closeSession();
    }
    
    protected static String generateJsonPayload(String product, String price) {
    	return "{\"total\": " + price + ",\"purchases\":[{\"product\":\"" + product + "\",\"amount\":" + price + "}]}";
    }
}
