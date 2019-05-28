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

import java.util.ArrayList;

/**
 * A simple "data structure" that holds the contents of a single purchase message.
 * 
 * @author Mike O'Brien
 *
 */
public class PurchaseMessage {
	public ArrayList<ProductPurchase> Purchases = new ArrayList<ProductPurchase>(); 
	public double total = 0.0f;
	public String message;
	public String clientID; 
	public String location; 
	public long timestamp;
	
	public PurchaseMessage clone() {
		PurchaseMessage rc = new PurchaseMessage();
		rc.timestamp = this.timestamp;
		rc.clientID = this.clientID;
		rc.location = this.location;
		rc.message = this.message;
		rc.total = this.total;
		
		for (ProductPurchase onePurchase : Purchases) {
			ProductPurchase clonedPurchase = onePurchase.clone();
			rc.Purchases.add(clonedPurchase);
		}
		
		return rc;
	}
}
