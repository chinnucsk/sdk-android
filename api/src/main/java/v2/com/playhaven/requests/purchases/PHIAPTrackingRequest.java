package v2.com.playhaven.requests.purchases;

import java.util.Currency;
import java.util.Hashtable;
import java.util.Locale;

import v2.com.playhaven.listeners.PHIAPRequestListener;
import org.json.JSONObject;

import android.content.Context;

import v2.com.playhaven.requests.base.PHAPIRequest;
import v2.com.playhaven.model.PHError;
import v2.com.playhaven.model.PHPurchase;

/**
 * Request for in-app purchase.
 * Note: unlike iOS, Google Play does not support a quantity attribute: http://stackoverflow.com/questions/8387184/android-in-app-billing-set-product-quantity
 * We have also tightened up access to the data members. Currently, the only way to fill
 * this request with data is to attach a {@link PHPurchase}.
 * @author samstewart
 *
 */
public class PHIAPTrackingRequest extends PHAPIRequest {

    //////////////////////////////////////////////////////
	//////////////// Member Variables ////////////////////

    /** The listener for the IAP tracking request */
    private PHIAPRequestListener listener;

    /** the completed PHPurchase we are sending to the server */
    private PHPurchase purchase;


    /** who sets these again? */
    static private Hashtable<String, String> cookies = new Hashtable<String, String>();
    
    public PHIAPTrackingRequest() {
    	super();
    }
    
	public PHIAPTrackingRequest(PHIAPRequestListener listener) {
		super();
        setIAPListener(listener);
	}

    public PHIAPTrackingRequest(PHIAPRequestListener listener, PHPurchase purchase) {
        super();
        this.purchase = purchase;
        setIAPListener(listener);
    }

	public PHIAPTrackingRequest(PHPurchase purchase) {
		this.purchase = purchase;
	}

    public void setPurchase(PHPurchase purchase) {
        this.purchase = purchase;
    }

    public void setIAPListener(PHIAPRequestListener listener) {
        this.listener = listener;
    }

    public PHIAPRequestListener getIAPListener() {
        return this.listener;
    }
	
	//////////////////////////////////////////////////////////////
	//////////////////////// Cookie Method ///////////////////////

    /**
     * Sets a unique cookie from the content templates so that we
     * don't send the same pruchase twice.
     * @param product
     * @param cookie
     */
	public static void setConversionCookie(String product, String cookie) {
        synchronized (cookies) {
            if (JSONObject.NULL.equals(cookie) || cookie.length() == 0) return;

            cookies.put(product, cookie);
        }

	}

    /**
     * Checks to see if a cookie exists for the given product id.
     * If it does, we "expire" the cookie and return it. Thus, this
     * method has side effects.
     * @param product The product ID for the cookie
     * @return the cookie string or null if no cookie exists.
     */
	public static String getAndExpireCookie(String product) {
        synchronized (cookies) {
            String cookie =  cookies.get(product);

            // we wish to expire the cookie to prevent
            // the purchase from being sent again.
            cookies.remove(product);

            return cookie;
        }
	}

	//////////////////////////////////////////////////////////////
	////////////////////////// Overrides /////////////////////////

    @Override
    public void handleRequestSuccess(JSONObject data) {
        if (this.listener != null)
            this.listener.onIAPRequestSucceeded(this);
    }

    @Override
    public void handleRequestFailure(PHError error) {
        if (this.listener != null)
            this.listener.onIAPRequestFailed(this, error);
    }

	@Override
	public String baseURL(Context context) {
		return super.createAPIURL(context, "/v3/publisher/iap/");
	}

	@Override
	public Hashtable<String, String> getAdditionalParams(Context context) {
        if (purchase == null) return new Hashtable<String, String>();

		// always refresh locale
		purchase.currencyLocale = Currency.getInstance(Locale.getDefault()); // gotta love Java?
		
		Hashtable<String, String> purchaseData = new Hashtable<String, String>();
		
		purchaseData.put("product",    (purchase.product != null ? purchase.product: ""));

		purchaseData.put("resolution", (purchase.resolution != null ? purchase.resolution.getType() : ""));

		purchaseData.put("price",      Double.toString(purchase.price));

        purchaseData.put("quantity",   String.valueOf(PHPurchase.DEFAULT_QUANTITY));

		if (purchase.hasError() && purchase.error.getErrorCode() != 0)
			purchaseData.put("error",    Integer.toString(purchase.error.getErrorCode()));
		
		purchaseData.put("price_locale", (purchase.currencyLocale != null ? purchase.currencyLocale.getCurrencyCode() : ""));

		purchaseData.put("store",  (purchase.marketplace != null ? purchase.marketplace.getOrigin() : null));
		
		// getting the cookie will expire it
		String cookie = PHIAPTrackingRequest.getAndExpireCookie((this.purchase.product != null ? this.purchase.product : null));

		purchaseData.put("cookie",       (cookie != null ? cookie : "")); // avoid a null value

		return purchaseData;
	}
}
