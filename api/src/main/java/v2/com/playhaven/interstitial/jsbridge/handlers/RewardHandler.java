package v2.com.playhaven.interstitial.jsbridge.handlers;

import android.os.Bundle;
import v2.com.playhaven.configuration.PHConfiguration;
import v2.com.playhaven.interstitial.PHContentEnums;
import v2.com.playhaven.interstitial.requestbridge.bridges.ContentRequestToInterstitialBridge;
import v2.com.playhaven.requests.crashreport.PHCrashReport;
import v2.com.playhaven.interstitial.jsbridge.PHJSBridge;
import v2.com.playhaven.model.PHReward;
import v2.com.playhaven.utils.PHStringUtil;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;

/**
 * Handler for webview rewards.
 */
public class RewardHandler extends AbstractHandler {


    @Override
    public void handle(JSONObject jsonPayload) {
        try {


            if (jsonPayload == null) return;

            JSONArray rewards = (jsonPayload.isNull("rewards") ?
                    new JSONArray()           :
                    jsonPayload.optJSONArray("rewards"));

            for (int i = 0; i < rewards.length(); i++) {

                JSONObject reward = rewards.optJSONObject(i);

                if (validateReward(reward)) {

                    PHReward r  = new PHReward();

                    r.name 		= reward.optString(PHContentEnums.Reward.IDKey		    .key(), "");

                    r.quantity 	= reward.optInt   (PHContentEnums.Reward.QuantityKey	.key(), -1);

                    r.receipt 	= reward.optString(PHContentEnums.Reward.ReceiptKey	.key(),     "");


                    Bundle args = new Bundle();
                    args.putParcelable(ContentRequestToInterstitialBridge.InterstitialEventArgument.Reward.getKey(), r);

                    String event = ContentRequestToInterstitialBridge.InterstitialEvent.UnlockedReward.toString();

                    contentDisplayer.get().sendEventToRequester(event, args);
                }

            }

            sendResponseToWebview(bridge.getCurrentQueryVar("callback"), null, null);

        } catch (Exception e) {  // swallow all exceptions and report
            PHCrashReport.reportCrash(e, "PHInterstitialActivity - handleRewards", PHCrashReport.Urgency.low);
        }
    }


    /** Checks to make sure the reward is valid */
    private boolean validateReward(JSONObject data) throws NoSuchAlgorithmException,
            UnsupportedEncodingException {

        if (JSONObject.NULL.equals(data) || data.length() == 0) return false;

        String reward 			= data.optString(PHContentEnums.Reward.IDKey.			key(), "");
        String quantity 		= data.optString(PHContentEnums.Reward.QuantityKey.	key(), "");
        String receipt 			= data.optString(PHContentEnums.Reward.ReceiptKey.	key(), "");
        String signature 		= data.optString(PHContentEnums.Reward.SignatureKey.	key(), "");

        String device_id 		= contentDisplayer.get().getDeviceID();

        if (device_id == null)
            device_id = "null";

        PHConfiguration config = new PHConfiguration();

        String generatedSig		= PHStringUtil.hexDigest(String.format(
                "%s:%s:%s:%s:%s",
                reward,
                quantity,
                device_id,
                receipt,
                contentDisplayer.get().getSecret())
        );

        PHStringUtil.log("Checking reward signature:  " + signature + " against: " + generatedSig);

        return (generatedSig.equalsIgnoreCase(signature));
    }
}
