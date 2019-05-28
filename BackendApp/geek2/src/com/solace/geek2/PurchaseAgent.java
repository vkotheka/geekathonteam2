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

import com.solacesystems.jcsmp.JCSMPException;

/**
 * Sub-class of the AbstractAgent, this class implements the main program of the Purchase Agent. Upon receipt 
 * of a purchase message, stores the message in a "database", and sends out a thank-you to the customer.
 * 
 * @author Mike O'Brien
 *
 */
public class PurchaseAgent extends AbstractAgent {

	/**
	 * Implements the abstract method of the base. This method is called after the 
	 * Receipt of a purchase message. Note that this message is already in the "database".
	 */
	@Override
	public void onPurchase(PurchaseMessage pmsg) throws JCSMPException {
		String text = "Thank you for your purchase at Geek2's store, location " + pmsg.location;
		String outTopic = "confirm/" + pmsg.location + "/" + pmsg.clientID;
		sendResponseMessage(pmsg, text, outTopic);
	}
	
	/**
	 * Main program for the Purchase agent
	 * @param args
	 * @throws JCSMPException
	 * @throws InterruptedException
	 */
    public static void main(String... args) throws JCSMPException, InterruptedException {
		// Check command line arguments
	    if (args.length < 5) {
	        System.out.println("Usage: PurchaseAgent <msg_backbone_ip:port> <vpn> <client-username> <password> <topic>");
	        System.exit(-1);
	    }
	    
	    PurchaseAgent agent = new PurchaseAgent();
	    agent.run(args);
	}
}
