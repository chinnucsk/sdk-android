package v2.com.playhaven.interstitial.jsbridge.handlers;

import android.os.Bundle;
import v2.com.playhaven.interstitial.PHContentEnums;
import v2.com.playhaven.interstitial.requestbridge.bridges.ContentRequestToInterstitialBridge;
import v2.com.playhaven.requests.content.PHContentRequest;
import v2.com.playhaven.requests.crashreport.PHCrashReport;
import v2.com.playhaven.utils.PHURLOpener;
import v2.com.playhaven.interstitial.jsbridge.PHJSBridge;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Handler for external launch call from webview.
 */
public class LaunchHandler extends AbstractHandler implements PHURLOpener.Listener {


    @Override
    public void handle(JSONObject jsonPayload) {
        if (jsonPayload == null) return;

        if (doesntHaveContentDisplayer()) return;

        contentDisplayer.get().launchURL(jsonPayload.optString("url", ""), this);
    }

    @Override
    public void onURLOpenerFinished(PHURLOpener loader) {

        if (loader.getContentTemplateCallback() != null) { // called as a launch event
            try {
                JSONObject r = new JSONObject();
                r.put("url", loader.getTargetURL());

                sendResponseToWebview(loader.getContentTemplateCallback(), r, null);


                // notify the content view that launched a new url
                Bundle message = new Bundle();

                // put the target url into the message
                String launch_url_key = ContentRequestToInterstitialBridge.InterstitialEventArgument.LaunchURL.getKey();
                message.putString(launch_url_key, loader.getTargetURL());

                String event = ContentRequestToInterstitialBridge.InterstitialEvent.LaunchedURL.toString();

                // finally send the event
                contentDisplayer.get().sendEventToRequester(event, message);

            } catch (JSONException e) {
                PHCrashReport.reportCrash(e, "PHInterstitialActivity - onURLOpenerFinished", PHCrashReport.Urgency.critical);
            } catch (Exception e) { //swallow all exceptions
                PHCrashReport.reportCrash(e, "PHInterstitialActivity - onURLOpenerFinished", PHCrashReport.Urgency.critical);
            }
        }

        // notify the original requester that the ad is dismissing us.
        Bundle message = new Bundle();

        // get the key for the close type parameter
        String close_type_key = ContentRequestToInterstitialBridge.InterstitialEventArgument.CloseType.getKey();
        message.putString(close_type_key, PHContentRequest.PHDismissType.AdSelfDismiss.toString());

        // get the event name
        String event = ContentRequestToInterstitialBridge.InterstitialEvent.Dismissed.toString();

        // finally send the event
        contentDisplayer.get().sendEventToRequester(event, message);

        // then dismiss the actual content view
        contentDisplayer.get().dismiss();
    }

    @Override
    public void onURLOpenerFailed(PHURLOpener loader) {

        if (loader.getContentTemplateCallback() != null) {
            try {
                JSONObject response = new JSONObject();
                JSONObject error 	= new JSONObject();

                error.put			("error", 	"1");
                response.put		("url", loader.getTargetURL());

                sendResponseToWebview(loader.getContentTemplateCallback(), response, error);
            } catch (JSONException e) {
                PHCrashReport.reportCrash(e, "PHInterstitialActivity - onURLOpenerFailed", PHCrashReport.Urgency.critical);
            } catch (Exception e) { //swallow all exceptions
                PHCrashReport.reportCrash(e, "PHInterstitialActivity - onURLOpenerFailed", PHCrashReport.Urgency.critical);
            }
        }

        // notify the original requester of the error.
        Bundle message = new Bundle();

        // get the key for the close type parameter
        String error_key = ContentRequestToInterstitialBridge.InterstitialEventArgument.Error.getKey();
        message.putString(error_key, PHContentEnums.Error.CouldNotLoadURL.toString());

        // get the event name
        String event = ContentRequestToInterstitialBridge.InterstitialEvent.Failed.toString();

        // finally send the event
        contentDisplayer.get().sendEventToRequester(event, message);
    }
}
