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
 *
 * HelloWorldSub
 *
 * This sample shows the basics of creating session, connecting a session,
 * subscribing to a topic, and receiving a message. This is meant to be a
 * very basic example for demonstration purposes.
 */

package com.solace.geek2;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import org.apache.log4j.Logger;

import com.solacesystems.jcsmp.BytesXMLMessage;
import com.solacesystems.jcsmp.Destination;
import com.solacesystems.jcsmp.JCSMPException;
import com.solacesystems.jcsmp.JCSMPFactory;
import com.solacesystems.jcsmp.JCSMPProperties;
import com.solacesystems.jcsmp.JCSMPSession;
import com.solacesystems.jcsmp.JCSMPStreamingPublishEventHandler;
import com.solacesystems.jcsmp.TextMessage;
import com.solacesystems.jcsmp.Topic;
import com.solacesystems.jcsmp.XMLMessageConsumer;
import com.solacesystems.jcsmp.XMLMessageListener;
import com.solacesystems.jcsmp.XMLMessageProducer;

/**
 * This abstract class is the base class for the family of agents that receives purchase messages. It
 * contains the re-usable logic to connect to the broker, manage subscription (and publishing), 
 * multi-threaded receipt of messages, and keep a database in memory of customer purchases
 * Sub-classes need only implement the onPurchase(PurchaseMessage pmsg) method, and deal with
 * a Java object with the pruchase data
 *  
 * @author Mike O'Brien
 *
 */
public abstract class AbstractAgent {
	private static final Logger logger = Logger.getLogger(AbstractAgent.class.getName());
	protected BlockingQueue<TextMessage> queue = new ArrayBlockingQueue<>(100);  
	protected XMLMessageProducer thisProducer = null;
	protected Model model = new Model();
	
	/**
	 * Simple constructor
	 */
	public AbstractAgent() {
		displayAsciiArtSignatureInLogs();
	}
	
	/**
	 * Reads in a text file and dumps the contents to the log. Important: in order for this to work, 
	 * ensure the classpath for your agent includes the correct config subfolder where the proper 
	 * sig file is found.
	 */
	private void displayAsciiArtSignatureInLogs() {
		InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("Sig.txt");
		try {
			String theString = readFromInputStream(inputStream);
			theString = "====================================================================================\n" + theString;
			logger.info(theString);
			System.out.println(theString);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	/**
	 * Utility method to load a text buffer from an input stream
	 * @param inputStream
	 * @return
	 * @throws IOException
	 */
	private String readFromInputStream(InputStream inputStream) throws IOException {
	    StringBuilder resultStringBuilder = new StringBuilder();
	    try (BufferedReader br
	      = new BufferedReader(new InputStreamReader(inputStream))) {
	        String line;
	        while ((line = br.readLine()) != null) {
	            resultStringBuilder.append(line).append("\n");
	        }
	    }
	  return resultStringBuilder.toString();
	}
	/**
	 * The "main" program for the agent. Subclasses should implement the Java main method for the program's entry point, 
	 * create an instance of itself and call this run() method. this method establishes all Solace i/o and controls the 
	 * main program loop.This method only returns when the program is terminated.
	 *   
	 * @param args
	 * @throws JCSMPException
	 * @throws InterruptedException
	 */
	public void run(String... args) throws JCSMPException, InterruptedException {
        System.out.println("Geeks2 Agent initializing...");
        // obtain properties from trhe command line
        final JCSMPProperties properties = new JCSMPProperties();
        properties.setProperty(JCSMPProperties.HOST, args[0]);  // msg-backbone-ip:port
        properties.setProperty(JCSMPProperties.VPN_NAME, args[1]); // message-vpn
        // client-username (assumes no password)
        properties.setProperty(JCSMPProperties.USERNAME, args[2]);
        properties.setProperty(JCSMPProperties.PASSWORD, args[3]);  // client-username (assumes no password)

        // the topic to subscribe to...
        final Topic topic = JCSMPFactory.onlyInstance().createTopic(args[4]);
        final JCSMPSession session = JCSMPFactory.onlyInstance().createSession(properties);

        /** Anonymous inner-class for MessageListener async threaded message callback */
        final XMLMessageConsumer cons = session.getMessageConsumer(new XMLMessageListener() {
            public void onReceive(BytesXMLMessage msg) {
                if (msg instanceof TextMessage) {
                    logger.info("Message received on " + msg.getDestination().getName());
                	// put the message into the RAM queue and return control on this thread back to the Solace API layer
                	queue.offer((TextMessage) msg);                	
                } else {
                    logger.warn("Unknown message received.");
                }
            }
            public void onException(JCSMPException e) {
                logger.warn("Consumer received exception: %s%n",e);
            }
        });
        // make the subscription
        session.addSubscription(topic);
        cons.start();
        String msg = "This agent is now connected to the Solace broker, awaiting purchase messages.";
        System.out.println(msg);
        logger.info(msg);

        //create a producer for sending out messages
        thisProducer = session.getMessageProducer(new JCSMPStreamingPublishEventHandler() {
            public void responseReceived(String messageID) {
            	logger.info("Producer received response for msg: " + messageID);
            }
            public void handleError(String messageID, JCSMPException e, long timestamp) {
            	logger.warn("Producer received error");
            }
        });

        // doesn't return from this... 
        processQueuedMessagesOnMainThread();
        
        // Close consumer
        cons.close();
        System.out.println("Exiting.");
        session.closeSession();		
	}
	
	/**
	 * Utility method for sub-class' (specific agents) to send outgoing messages. This method will clone the  
	 * PurchaseMessage that is passed in, add the textual message to it, and send on the specified topic.
	 * @param pmsg
	 * @param textMessageToAdd
	 * @param topic
	 * @throws JCSMPException
	 */
	protected void sendResponseMessage(PurchaseMessage pmsg, String textMessageToAdd, String topic) throws JCSMPException {
    	PurchaseMessage reply = pmsg.clone();
		reply.message = textMessageToAdd;
		String json = Model.toJson(reply, true);
		sendTextMessage (json, topic);
	}
	
	/**
	 * Utility method to send a text message.
	 * 
	 * @param text
	 * @param topic
	 * @throws JCSMPException
	 */
	protected void sendTextMessage(String text, String topic) throws JCSMPException {
		TextMessage replyMsg = JCSMPFactory.onlyInstance().createMessage(TextMessage.class);
		replyMsg.setText(text);
		Topic replyTopic = JCSMPFactory.onlyInstance().createTopic(topic);
		thisProducer.send(replyMsg,replyTopic);    
	}
	
	/**
	 * A loop that executes on the main program thread. It pulls any message that have been put into the 
	 * RAM queue by the Solace thread (callback above), de-serializes the JSON into a PurchaseMessage
	 * object, stores it in the RAM database, and involves trhe sub-class' onPurchase() method to do
	 * something useful.
	 * 
	 * @throws InterruptedException
	 * @throws JCSMPException
	 */
	protected void processQueuedMessagesOnMainThread() throws InterruptedException, JCSMPException {
		boolean bFinished = false;
    	while (!bFinished) {
    		// get the next message off of the ram queue, waiti forever until something arrives
    		TextMessage msg = queue.take();
        	Destination dest = msg.getDestination();
        	String topicRecieved = dest.getName();
        	
        	// topic structure is: [message type]/location/customerId. We aren't interested in the type, because 
        	// this agent will have subscribed only to the "purchase" message type. We need the location and 
        	// clientId. 
        	String[] topicParts = topicRecieved.split("/");
        	String location = topicParts[1];
        	String clientId = topicParts[2];
        	
    		String strJsonData = ((TextMessage) msg).getText();
        	try {
        		PurchaseMessage pmsg = model.loadFromJson(strJsonData, clientId, location);
        		pmsg.timestamp = System.currentTimeMillis();
        		
        		// call the child subclass
        		onPurchase(pmsg);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}    
        }	
	}
	/**
	 * Subclass' implement this method to handle purchase event messages
	 * 
	 * @param pmsg
	 * @throws JCSMPException
	 */
	public abstract void onPurchase(PurchaseMessage pmsg) throws JCSMPException;
	
}
