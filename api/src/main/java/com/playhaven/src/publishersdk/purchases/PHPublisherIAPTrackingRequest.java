package com.playhaven.src.publishersdk.purchases;

import android.content.Context;
import v2.com.playhaven.configuration.PHConfiguration;
import v2.com.playhaven.model.PHError;
import v2.com.playhaven.requests.purchases.PHIAPTrackingRequest;
import com.playhaven.src.common.PHAPIRequest;
import com.playhaven.src.common.PHConfig;
import com.playhaven.src.publishersdk.content.PHPurchase;
import com.playhaven.src.utils.EnumConversion;

import java.lang.ref.WeakReference;
import java.util.Currency;
import java.util.Hashtable;

/**
 * Adapter for {@link v2.com.playhaven.requests.purchases.PHIAPTrackingRequest}.
 */
public class PHPublisherIAPTrackingRequest extends PHIAPTrackingRequest implements PHAPIRequest {
    /**
     * Represents the quantity of the purchase.
     * google billing doesn't actually use this field. It's meaningless.
     */
    public int quantity = 0;

    /** the store of origin */
    public PHPurchaseOrigin store = PHPurchaseOrigin.Google;

    /** We need a reference to our context for the send() method*/
    private WeakReference<Context> context;

    public String product = ""; // make sure not null to avoid errors

    public double price = 0; // Note: this is currently ignored by the server

    public PHError error;

    public Currency currencyLocale; // Note: this is currently ignored by the serve

    public PHPurchase.Resolution resolution = PHPurchase.Resolution.Cancel; // default to cancel?

    private static Hashtable<String, String> cookies = new Hashtable<String, String>();

    public enum PHPurchaseOrigin {
        Google			 ("GoogleMarketplace"),
        Amazon		 	 ("AmazonAppstore"),
        Paypal 			 ("Paypal"),
        Crossmo			 ("Crossmo"),
        Motorola		 ("MotorolaAppstore");

        private String originStr;

        private PHPurchaseOrigin(String originStr) {
            this.originStr = originStr;
        }

        public String getOrigin() {
            return this.originStr;
        }
    }



    public PHPublisherIAPTrackingRequest(Context context) {
        super();
        this.context = new WeakReference<Context>(context);
    }

    public PHPublisherIAPTrackingRequest(Context context, Delegate delegate) {
        super(new TrackingDelegateAdapter(delegate));
    }

    public PHPublisherIAPTrackingRequest(Context context, PHError error) {
        this(context);

        v2.com.playhaven.model.PHPurchase purchase = new v2.com.playhaven.model.PHPurchase();
        purchase.error = error;

        super.setPurchase(purchase);
    }

    public PHPublisherIAPTrackingRequest(Context context, PHPurchase purchase) {
        this(context, purchase.product, purchase.quantity, purchase.resolution);
    }

    /**
     * Crates a new {@link PHPublisherIAPTrackingRequest}
     * @param context a valid context for launching the request
     * @param product_id a valid product identifier
     * @param quantity quantity is meaningless to android billing
     * @param resolution the result of the android billing for this id.
     */
    public PHPublisherIAPTrackingRequest(Context context, String product_id, int quantity, PHPurchase.Resolution resolution) {
        this(context);

        v2.com.playhaven.model.PHPurchase purchase = new v2.com.playhaven.model.PHPurchase();

        purchase.product 		= product_id;
        purchase.resolution 	= EnumConversion.convertToNewBillingResult(resolution);

        // we don't use the quantity field
        super.setPurchase(purchase);
    }


    @Override
    public void setDelegate(Delegate delegate) {
        super.setIAPListener(new TrackingDelegateAdapter(delegate));
    }

    @Override
    /** sends the request.
     */
    public void send() {
        // we need to pull the token/secret in
        PHConfiguration config = new PHConfiguration();

        config.setCredentials(context.get(), PHConfig.token, PHConfig.secret);

        super.send(context.get());
    }

    /** we need to override this method because we use different field names */
    @Override
    public Hashtable<String, String> getAdditionalParams(Context context) {
        // wrap all of our fields into a PHPurchase and inform super before generating
        // the additional parameters

        v2.com.playhaven.model.PHPurchase purchase = new v2.com.playhaven.model.PHPurchase();
        purchase.error              = this.error;
        purchase.product            = this.product;
        purchase.price              = this.price;
        purchase.currencyLocale     = this.currencyLocale;
        purchase.resolution         = EnumConversion.convertToNewBillingResult(this.resolution);
        // ignore the quantity


        // make certain our superclass is using the right info
        super.setPurchase(purchase);

        // finally tell our superclass to generate the parameters
        return super.getAdditionalParams(context);


    }
}
