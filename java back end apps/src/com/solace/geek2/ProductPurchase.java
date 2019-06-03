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

/**
 * Simple "data structure". Holds a single product purchased.
 * @author Mike O'Brien
 *
 */
public class ProductPurchase {
	public String product;
	public double amount;
	
	/**
	 * Make a copy of this object.
	 */
	public ProductPurchase clone() {
		ProductPurchase rc = new ProductPurchase();
		rc.amount = this.amount;
		rc.product = this.product;
		return rc;
	}
}
