package v2.com.playhaven.model;

import android.content.Context;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import v2.com.playhaven.interstitial.requestbridge.BridgeManager;
import v2.com.playhaven.interstitial.requestbridge.bridges.ContentRequestToInterstitialBridge;

import java.util.Currency;

/** Simple container for managing purchases. It can self-report the final result of the transaction
 * via {@link PHPurchase#reportAndroidBillingResult(v2.com.playhaven.model.PHPurchase.AndroidBillingResult, android.content.Context)}.
 * @author samstewart
 *
 */
public class PHPurchase implements Parcelable {

    public enum AndroidBillingResult {
        Bought 	   ("buy"),
        Cancelled  ("cancel"),
        Failed     ("error");

        private String type;

        private AndroidBillingResult(String type) {

            this.type = type;
        }

        public String getType() {

            return type;
        }

    }

    /** The marketplace of the purchase */
    public enum PHMarketplaceOrigin {
        Google			 ("GoogleMarketplace"),
        Amazon		 	 ("AmazonAppstore"),
        Paypal 			 ("Paypal"),
        Crossmo			 ("Crossmo"),
        Motorola		 ("MotorolaAppstore");

        private String originStr;

        private PHMarketplaceOrigin(String originStr) {
            this.originStr = originStr;
        }

        public String getOrigin() {
            return this.originStr;
        }
    }

    /** Since quantity is meaningless, we leave this at 1*/
    public static final int DEFAULT_QUANTITY = 1;
    
    /** retained to prevent legacy integration issues */ 
    public int quantity = DEFAULT_QUANTITY;

    public String product;
    
    private String tag;
    
    public String name;

    /** the error (if an error occurred) for this PHPurchase */
    public PHError error;

    /** the currency locale of the purchase */
    public Currency currencyLocale;

    public String receipt;

    /** The price of the product.
     * Note: this is currently ignored by the server.
     */
    public double price = 0;

    public String callback;

    public AndroidBillingResult resolution;
    
    public static final String NO_CONTENTVIEW_INTENT = "v2.com.playhaven.null";

    /** the store of origin */
    public PHMarketplaceOrigin marketplace = PHMarketplaceOrigin.Google;
    
    /** retained to prevent legacy integration issues */
	public String contentview_intent;

    /**
     * Creates a new {@link PHPurchase} and associates it with an {@link v2.com.playhaven.interstitial.PHInterstitialActivity}
     * by means of the {@link v2.com.playhaven.interstitial.requestbridge.BridgeManager} "tag" identifier.
     */
	public PHPurchase(String tag) {
		this.tag = tag;
	}

    /** Creates a new {@link PHPurchase} <em>without</em> a tag (disassociated from any {@link v2.com.playhaven.interstitial.PHInterstitialActivity}*/
    public PHPurchase() {
         // no - op
    }


    /** determines whether or not this {@link PHPurchase} had an error */
    public boolean hasError() {
        return (error != null);
    }

    /**
     * Gets the tag which associates this purchase with an interstitial.
     * This tag can only be set in the constructor.
     */
    public String getTag() {
        return this.tag;
    }

    /**
     * Informs the content template that originally "spawned"
     * this {@link PHPurchase} that the Android billing process
     * has completed with the given status. We use the {@link PHPurchase#tag}
     * that we received when we were created by the original interstitial activity.
     *
     * @param resolution The final status of the Android billing process
     * @param context A valid context for sending a broadcast
     */
    public void reportAndroidBillingResult(AndroidBillingResult resolution, Context context) {
    	this.resolution = resolution;

        Bundle message          = new Bundle();

        String purchase_key     = ContentRequestToInterstitialBridge.InterstitialEventArgument.Purchase.getKey();

        message.putParcelable(purchase_key, this);

        String event            = ContentRequestToInterstitialBridge.InterstitialEvent.PurchaseResolved.toString();

		// notify the original interstitial activity from the "requesters" perspective
        // We use the tag that was supplied when were created
        BridgeManager.sendMessageFromRequester(tag,
                                               event,
                                               message,
                                               context);
    }

    ////////////////////////////////////////////////////
	////////////////// Parcelable Methods //////////////

	public static final Parcelable.Creator<PHPurchase> CREATOR = new Creator<PHPurchase>() {
		
		@Override
		public PHPurchase[] newArray(int size) {
			return new PHPurchase[size];
		}
		
		@Override
		public PHPurchase createFromParcel(Parcel source) {
			return new PHPurchase(source);
		}
	};
	
	public PHPurchase(Parcel in) {
		this.product 			= in.readString();
		
		if (this.product != null && this.product.equals(PHContent.PARCEL_NULL))
			this.product = null;
		
		this.name 				= in.readString();
		
		if (this.name != null && this.name.equals(PHContent.PARCEL_NULL))
			this.name = null;
		
		this.receipt 			= in.readString();
		
		if (this.receipt != null && this.receipt.equals(PHContent.PARCEL_NULL))
			this.receipt = null;
		
		
		this.callback 			= in.readString();
		
		if (this.callback != null && this.callback.equals(PHContent.PARCEL_NULL))
			this.callback = null;
		
		this.tag = in.readString();

        if (this.tag != null && this.tag.equals(PHContent.PARCEL_NULL))
            this.tag = null;
        
        String resCandidate = in.readString();
		this.resolution = (resCandidate.equals(PHContent.PARCEL_NULL)) ? 
				null : AndroidBillingResult.valueOf(resCandidate);

        if (this.resolution != null && this.resolution.equals(PHContent.PARCEL_NULL))
            this.resolution = null;
	}
	
	public int describeContents() {
		return 0;
	}
	
	public void writeToParcel(Parcel out, int flags) {
		out.writeString(product == null ? PHContent.PARCEL_NULL : product);
		out.writeString(name == null ? PHContent.PARCEL_NULL : name);
		out.writeString(receipt == null ? PHContent.PARCEL_NULL : receipt);
		out.writeString(callback == null ? PHContent.PARCEL_NULL : callback);
		out.writeString(tag == null ? PHContent.PARCEL_NULL : tag);
		out.writeString(resolution == null ? PHContent.PARCEL_NULL : resolution.toString());
	}
}

