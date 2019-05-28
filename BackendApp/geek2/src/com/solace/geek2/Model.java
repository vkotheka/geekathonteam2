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

import java.io.IOException;
import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;
import org.json.JSONWriter;

/**
 * This class encapsulates a simple RAM database with all product purchases from 
 * all customers. It also contains utility methods to marshal and unmarshal purchase
 * messages from JSOn to Java and vice versa.   
 * 
 * @author Mike O'Brien
 *
 */
public class Model {
	// definitions of the fields in the JSOn schema
	public static String jsonField_purchases = "purchases";
	public static String jsonField_message = "message";
	public static String jsonField_total = "total";
	public static String jsonField_product = "product";
	public static String jsonField_amount = "amount";
	
	private ArrayList<PurchaseMessage> Purchases = new ArrayList<PurchaseMessage>();
	private static final Logger logger = Logger.getLogger(Model.class.getName());
	
	/**
	 * Utility method to convert a JSON array into as simple java array.
	 * 
	 * @param obj
	 * @param pattern
	 * @return
	 * @throws JSONException
	 */
	public static ArrayList<JSONObject> parseJsonArray(JSONObject obj, String pattern) throws JSONException {
		ArrayList<JSONObject> listObjs = new ArrayList<JSONObject>();
        JSONArray jsonArray = obj.getJSONArray (pattern);
        for (int i = 0; i < jsonArray.length(); ++i) {
          final JSONObject site = jsonArray.getJSONObject(i);
          listObjs.add(site);
        }
        return listObjs;
    }
	
	/**
	 * Look into the database and get the message which chronologically precedes the current message
	 * for this customer (if any). Retruns null if this is the first message from  this customer. 
	 * 
	 * @param clientID
	 * @param msg
	 * @return
	 */
	public PurchaseMessage getLastPurchaseBefore(String clientID, PurchaseMessage msg) {
		logger.debug("looking up last purchase for client " + clientID);
		PurchaseMessage lastMessage = null;
		for (PurchaseMessage oneObj: Purchases) {
			if (oneObj.clientID.equals(clientID)) {
				if (lastMessage == null) {
					if (msg.timestamp != oneObj.timestamp) {
						lastMessage = oneObj;
					}
				}
				else if (msg.timestamp != oneObj.timestamp) {
					if (oneObj.timestamp > lastMessage.timestamp) {
						lastMessage = oneObj;
					}
				}
			}
		}
		return lastMessage;
	}
	
	/**
	 * Look through the "database" and return the total purchases for this customer.
	 * 
	 * @param clientID
	 * @return
	 */ 
	public double getTotalPurchasesForClient(String clientID) {
		double rc = 0.0f;
		for (PurchaseMessage oneObj: Purchases) {
			if (oneObj.clientID.equals(clientID)) {
				rc += oneObj.total;
			}
		}
		return rc;
	}
	/**
	 * Converts a PurchaseMessage object into a json payload
	 * 
	 * @param msg
	 * @param includeMessageText
	 * @return
	 */
	public static String toJson(PurchaseMessage msg, boolean includeMessageText) {
	    JSONWriter jsonWriter = new JSONStringer();

		jsonWriter.object();
		jsonWriter.key(jsonField_total);
		jsonWriter.value(msg.total);
		
		if (includeMessageText) {
			jsonWriter.key(jsonField_message);
			jsonWriter.value(msg.message);
		}
		
	    jsonWriter.key(jsonField_purchases);
	    jsonWriter.array();
		for (ProductPurchase oneObj: msg.Purchases) {
			jsonWriter.object();
			jsonWriter.key(jsonField_product);
			jsonWriter.value(oneObj.product);
			jsonWriter.key(jsonField_amount);
			jsonWriter.value(oneObj.amount);
			jsonWriter.endObject();
		}
		jsonWriter.endArray();
		jsonWriter.endObject();
		
		return jsonWriter.toString();
	}
	/**
	 * Loads a json based message payload into Java objects
	 * 
	 * @param strJsonData
	 * @param clientId
	 * @param location
	 * @return
	 * @throws IOException
	 */
	public PurchaseMessage loadFromJson(String strJsonData, String clientId, String location) throws IOException {
    	logger.debug("loading message from json payload.");

	    // using a JSON parser
	    JSONObject obj = new JSONObject(strJsonData);
	
	    PurchaseMessage msg = new PurchaseMessage();
	    msg.clientID = clientId;
	    msg.location = location;
	    msg.total = obj.getDouble(jsonField_total);
	    
	    // the "PurchaseMessage" java object has a field to store a text message that is part of the defined
	    // payload for some of the messages in this solution. Incoming purchases do not have a text message,
	    // so we will likley not be loading any messages
	    if (obj.has(jsonField_message)) {
		    msg.message = obj.getString(jsonField_message);
	    }
        
	    // iterate over a potential array of product purchases
	    ArrayList<JSONObject> edgePairsObj = parseJsonArray(obj, jsonField_purchases);
	    for (JSONObject jsonObj: edgePairsObj) {
	    	ProductPurchase purchase = new ProductPurchase();
	    	purchase.amount = jsonObj.getDouble(jsonField_amount);
	    	purchase.product  = jsonObj.getString(jsonField_product);
	    	
	    	// add this product purchase into the PurchaseMessage object
	    	msg.Purchases.add(purchase);
	    }
	    Purchases.add(msg);
	    
	    logger.debug("loaded " + msg.Purchases.size() + " product purchases from json payload.");
	    return msg;
    }
}
