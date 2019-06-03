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

import java.util.HashMap;

import org.apache.log4j.Logger;

import com.solacesystems.jcsmp.JCSMPException;

/**
 * Sub-class of the AbstractAgent, this class implements the main program of the Loyalty Agent. Upon receipt 
 * of a purchase message, stores the message in a "database". This simple illustrative program implements 3 loyalty
 * levels: one after $20 in total purchase history, another after $40 and a final one after $60. Upon first achieving 
 * a given level, a thank you is sent out to the customer with a reward offered to them.
 * 
 * @author Mike O'Brien
 *
 */public class LoyaltyAgent extends AbstractAgent {
	private static final Logger logger = Logger.getLogger(LoyaltyAgent.class.getName());

	// and enumeration for the loyalty levels 
	public enum eLoyalityLevel {eNone, e20, e40, e60 }
	
	// a data structure for storing the current loyalty level of specific customers
	public class ClientLoyalityLevel {
		public String clientId;
		public eLoyalityLevel currentLoyaltyLevel = eLoyalityLevel.eNone;
	}
	// a map of the current loyalty level for all customers
	public HashMap<String, ClientLoyalityLevel> clientLoyaltyMap = new HashMap<String, ClientLoyalityLevel>(); 
	
	/**
	 * Fetches the currently loyalty level for a given customer 
	 * @param clientID
	 * @return
	 */
	private eLoyalityLevel getClientLoyalty(String clientID) {
		eLoyalityLevel eRc = eLoyalityLevel.eNone;
		if (clientLoyaltyMap.containsKey(clientID)) {
			ClientLoyalityLevel current = clientLoyaltyMap.get(clientID);
			eRc = current.currentLoyaltyLevel;
		}
		return eRc;
	}
	/**
	 * Sets the current loyalty level for a given customer
	 * @param clientID
	 * @param eLevel
	 */
	private void setClientLoyalty(String clientID, eLoyalityLevel eLevel) {
		ClientLoyalityLevel current = null;
		if (clientLoyaltyMap.containsKey(clientID)) {
			current = clientLoyaltyMap.get(clientID);
		}
		else {
			current = new ClientLoyalityLevel();
			current.clientId = clientID;
		}
		current.currentLoyaltyLevel = eLevel;
		clientLoyaltyMap.put(clientID, current);		
	}
	
	/**
	 * Implements the abstract method of the base. This method is called after the 
	 * Receipt of a purchase message. Note that this message is already in the "database".
	 * 
	 * It checks to see if the client has achieved a new loyalty level and if so sends out a message
	 * Informing the customer and offering a reward
	 */
	@Override
	public void onPurchase(PurchaseMessage pmsg) throws JCSMPException {
		boolean bSend = false;
		String howMuch = "";
		String reward = "";
		String thisClientId = pmsg.clientID;
		double tally = model.getTotalPurchasesForClient(thisClientId);
		logger.info(thisClientId + " has spent a total of $" + tally);
		
		if (tally > 20.0f) {
			eLoyalityLevel eLevel = getClientLoyalty(thisClientId);
			if (eLevel == eLoyalityLevel.eNone) {
				setClientLoyalty(thisClientId, eLoyalityLevel.e20);
				howMuch = "20";
				reward = "a free coffee";
				bSend = true;
				logger.info(thisClientId + " has achieved loyalty level " + howMuch);
			}
		}

		if (tally > 40.0f) {
			eLoyalityLevel eLevel = getClientLoyalty(thisClientId);
			if ((eLevel == eLoyalityLevel.eNone) || (eLevel == eLoyalityLevel.e20)) {
				setClientLoyalty(thisClientId, eLoyalityLevel.e40);
				howMuch = "40";
				reward = "$2.00 in free gas";
				bSend = true;
				logger.info(thisClientId + " has achieved loyalty level " + howMuch);
			}
		}
		
		if (tally > 60.0f) {
			eLoyalityLevel eLevel = getClientLoyalty(thisClientId);
			if ((eLevel == eLoyalityLevel.eNone) || (eLevel == eLoyalityLevel.e20)|| (eLevel == eLoyalityLevel.e40)) {
				setClientLoyalty(thisClientId, eLoyalityLevel.e60);
				howMuch = "60";
				reward = "$5.00 in free gas";
				bSend = true;
				logger.info(thisClientId + " has achieved loyalty level " + howMuch);
			}
		}

		if (bSend) {
			String json = "{ \"message\": \"Thank you for your total purchases of $" + howMuch + ".00 at Geeks2. You have earned " + 
					reward + "! Please come again soon to claim your reward.\"}";
			String raw = "Thank you for your total purchases of $" + howMuch + ".00 at Geeks2. You have earned " + 
					reward + "! Please come again soon to claim your reward.";
			String outTopic = "loyalty/" + pmsg.location + "/" + pmsg.clientID;
			this.sendTextMessage(raw, outTopic);
		}
	}
	/**
	 * Main program for the Loyalty agent
	 * @param args
	 * @throws JCSMPException
	 * @throws InterruptedException
	 */
    public static void main(String... args) throws JCSMPException, InterruptedException {
		// Check command line arguments
	    if (args.length < 5) {
	        System.out.println("Usage: LoyaltyAgent <msg_backbone_ip:port> <vpn> <client-username> <password> <topic>");
	        System.exit(-1);
	    }
	    LoyaltyAgent agent = new LoyaltyAgent();
	    agent.run(args);
	}
}
