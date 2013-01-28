package v2.com.playhaven.interstitial.jsbridge.handlers;

import android.os.Bundle;
import v2.com.playhaven.configuration.PHConfiguration;
import v2.com.playhaven.interstitial.PHContentEnums;
import v2.com.playhaven.interstitial.requestbridge.bridges.ContentRequestToInterstitialBridge;
import v2.com.playhaven.requests.crashreport.PHCrashReport;
import v2.com.playhaven.interstitial.jsbridge.PHJSBridge;
import v2.com.playhaven.model.PHPurchase;
import v2.com.playhaven.requests.purchases.PHIAPTrackingRequest;
import v2.com.playhaven.utils.PHStringUtil;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;

/**
 * Handles the event where a users initiates an in-app purchase.
 */
public class PurchaseHandler extends AbstractHandler {


    @Override
    public void handle(JSONObject jsonPayload) {
        try {
            if (jsonPayload == null) return;

            JSONArray purchases = (jsonPayload.isNull("purchases") ?
                                   new JSONArray()             :
                                   jsonPayload.optJSONArray("purchases"));


            for (int i = 0; i< purchases.length(); i++) {

                JSONObject purchase = purchases.optJSONObject(i);

                if (validatePurchase(purchase)) {

                    // create a new purchase item and tie it to the content displayer
                    PHPurchase p 	= new PHPurchase(contentDisplayer.get().getTag());

                    p.product 		= purchase.optString	(PHContentEnums.Purchase.ProductIDKey	.key(), "");

                    p.name 			= purchase.optString	(PHContentEnums.Purchase.NameKey		.key(), "");

                    p.receipt 		= purchase.optString	(PHContentEnums.Purchase.ReceiptKey	    .key(), "");

                    p.callback	    = bridge.getCurrentQueryVar("callback");

                    String cookie 	= purchase.optString(PHContentEnums.Purchase.CookieKey.key());

                    PHIAPTrackingRequest.setConversionCookie(p.product, cookie);

                    Bundle message = new Bundle();

                    String purchase_key = ContentRequestToInterstitialBridge.InterstitialEventArgument.Purchase.getKey();
                    message.putParcelable(purchase_key, p);

                    String event = ContentRequestToInterstitialBridge.InterstitialEvent.MadePurchase.toString();

                    contentDisplayer.get().sendEventToRequester(event, message);
                }

            }

            sendResponseToWebview(bridge.getCurrentQueryVar("callback"), null, null);

        } catch (Exception e) { // swallow all exceptions
            PHCrashReport.reportCrash(e, "PHInterstitialActivity - handlePurchase", PHCrashReport.Urgency.critical);
        }
    }

    /** Checks to make certain the purchase is valid */
    private boolean validatePurchase(JSONObject data) throws NoSuchAlgorithmException,
            UnsupportedEncodingException {
        if (JSONObject.NULL.equals(data) || data.length() == 0) return false;

        String productID 		  = data.optString(PHContentEnums.Purchase.ProductIDKey.	key(), "");

        String name 			  = data.optString(PHContentEnums.Purchase.NameKey.	  	    key(), "");

        String receipt 			  = data.optString(PHContentEnums.Purchase.ReceiptKey.  	key(), "");

        String signature 		  = data.optString(PHContentEnums.Purchase.SignatureKey.	key(), "");

        PHConfiguration config = new PHConfiguration();

        // check the signature by generating our own
        String generatedSig		  = PHStringUtil.hexDigest(
                String.format("%s:%s:%s:%s:%s:%s", productID,
                        name,
                        PHPurchase.DEFAULT_QUANTITY, // we fix the value of quantity (since it's meaningless)
                        contentDisplayer.get().getDeviceID(),
                        receipt,
                        contentDisplayer.get().getSecret())
        );

        PHStringUtil.log("Checking purchase signature:  " + signature + " against: " + generatedSig);

        return (generatedSig.equals(signature));
    }
}
