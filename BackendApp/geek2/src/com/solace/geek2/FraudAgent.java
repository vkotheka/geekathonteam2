
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

/**
 * Sub-class of the AbstractAgent, this class implements the main program of the Fraud Agent. Upon receipt 
 * of a purchase message, it checks to see if the customer has made a purchase at another location with the 
 * last 5 minutes, and if so, sends out a textual message to the customer informing them of the condition.
 * 
 * Obviously, this is for illustrative/architectural/demo purposes and is not intended to be any type of 
 * real-life fraud detection algorithm.
 *  
 * @author Mike O'Brien
 *
 */
public class FraudAgent extends AbstractAgent {
	private static final Logger logger = Logger.getLogger(FraudAgent.class.getName());
	

	/**
	 * Implements the abstract method of the base. This method is called after the 
	 * Receipt of a purchase message. Note that this message is already in the "database".
	 */
	@Override
	public void onPurchase(PurchaseMessage pmsg) throws JCSMPException {
		// lets see if we have a previous message from this customer
		PurchaseMessage lastMsg = model.getLastPurchaseBefore(pmsg.clientID, pmsg);
		if (lastMsg != null) {
			// yes, we have a previous message. Was it at thye same store or a different one?
			if (lastMsg.location.equals(pmsg.location ) == false) {
				// hmm, Ok, different store. How long ago was that? Lets check the timestamps
				long millsBetween = pmsg.timestamp - lastMsg.timestamp;
				long mins = (millsBetween/1000)/60; 
				
				if (mins < 5) {
					// this is not good, 2 purchases less than 5 minutes apart at different locations 
					logger.info(pmsg.clientID + "'s last purchase was less than 5 minutes ago, from a different location. " + 
							"Possible fraud!");
					
					// send a message out to the customer
					String outTopic = "fraud/" + pmsg.location + "/" + pmsg.clientID;
					String text = "Your last purchase was less than 5 minutes ago, from a different location. " + 
							"This looks like a possible fraudulent usage. Please contact our customer service center.";
					sendResponseMessage(pmsg, text, outTopic);
				}			
			}
			else {
				logger.info(pmsg.clientID + "'s last purchase was as the same location. This is OK. ");
			}
		}
	}
	
	/**
	 * Main program for the Fraud agent
	 * @param args
	 * @throws JCSMPException
	 * @throws InterruptedException
	 */
    public static void main(String... args) throws JCSMPException, InterruptedException {
		// Check command line arguments
	    if (args.length < 5) {
	        System.out.println("Usage: FraudAgent <msg_backbone_ip:port> <vpn> <client-username> <password> <topic>");
	        System.exit(-1);
	    }
	    
	    FraudAgent agent = new FraudAgent();
	    agent.run(args);
	}
}
