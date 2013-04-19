package com.playhaven.src.publishersdk.content;

import android.app.Activity;
import com.playhaven.src.utils.EnumConversion;

/**
 * A simple facade for the real PHPurchase class. We add the "quantity" field
 * ever though it is meaningless.
 */
public class PHPurchase extends v2.com.playhaven.model.PHPurchase {

    public PHPurchase(String tag) {
        super(tag);
    }

    /** Special conversion constructor from the new PHPurchase */
    public PHPurchase(v2.com.playhaven.model.PHPurchase purchase) {
        super(purchase.getTag());

        this.quantity       = 0;
        super.resolution    = purchase.resolution;

        // convert to our older resolution field as well
        this.resolution     = EnumConversion.convertToOldBillingResult(purchase.resolution);
        this.price          = purchase.price;
        this.callback       = purchase.callback;
        this.currencyLocale = purchase.currencyLocale;
        this.receipt        = purchase.receipt;
        this.name           = purchase.name;
        this.product        = purchase.product;
        this.marketplace    = purchase.marketplace;
        this.error          = purchase.error;

    }

    /** The response from the android billing framework.
     */
    public enum Resolution {

        Buy 	("buy"),
        Cancel  ("cancel"),
        Error 	("error");

        private String type;

        private Resolution(String type) {
            this.type = type;
        }

        public String getType() {
            return type;
        }

    }

    /** the result from android billing.
     */
    public Resolution resolution;

    /** quantity but meaningless field.
     */
    public int quantity;

    /**
     * Informs the content template of the android billing result.
     * @param resolution The result of the android billing process.
     * @param context The context for sending the message
     */
    public void reportResolution(Resolution resolution, Activity context) {
    	this.resolution = resolution;
        super.reportAndroidBillingResult(EnumConversion.convertToNewBillingResult(resolution), context);
    }


}
