package v2.com.playhaven.interstitial.jsbridge.handlers;

import android.os.Bundle;
import v2.com.playhaven.interstitial.PHContentEnums;
import v2.com.playhaven.interstitial.requestbridge.bridges.ContentRequestToInterstitialBridge;
import v2.com.playhaven.listeners.PHSubContentRequestListener;
import v2.com.playhaven.model.PHContent;
import v2.com.playhaven.model.PHError;
import v2.com.playhaven.requests.content.PHSubContentRequest;
import v2.com.playhaven.requests.crashreport.PHCrashReport;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Handles a sub-interstitial request from the webview. This is most frequently used in the transition
 * between the "game of the week" and the "more games" screen.
 */
public class SubrequestHandler extends AbstractHandler implements PHSubContentRequestListener {


    /** Handle the webviews request */
    @Override
    public void handle(JSONObject jsonPayload) {

        if (jsonPayload == null) return;

        PHSubContentRequest request = new PHSubContentRequest(this);

        request.setBaseURL  (jsonPayload.optString("url", ""));
        request.setWebviewCallback(bridge.getCurrentQueryVar("callback"));

        // tell the content displayer to launch a new subrequest
        contentDisplayer.get().launchSubRequest(request);

        // tell all delegates we have started a subrequest
        notifyRequesterOfStarting();
    }

    /** Simple helper method to notify the original content
     * requester that we are starting the sub-request.
     */
    private void notifyRequesterOfStarting() {

        Bundle message = new Bundle();

        String event = ContentRequestToInterstitialBridge.InterstitialEvent.SentSubrequest.toString();

        contentDisplayer.get().sendEventToRequester(event, message);
    }

    /** Simple helper method for sending a broadcast notifying the requester of
     * an error.
     * @param error The error we wish to send back.
     */
    private void notifyRequesterOfError(PHContentEnums.Error error) {
        // inform the request listener of the error
        Bundle message = new Bundle();

        String error_key = ContentRequestToInterstitialBridge.InterstitialEventArgument.Error.toString();
        message.putString(error_key, error.toString());

        String event = ContentRequestToInterstitialBridge.InterstitialEvent.Failed.toString();

        contentDisplayer.get().sendEventToRequester(event, message);
    }
    ////////////////////////////////////////////////
    /////////// *Sub* request listener /////////////

    @Override
    public void onSubContentRequestSucceeded(PHSubContentRequest request, JSONObject responseData) {
        if (responseData.length() == 0) return;

        try {
            PHContent content = new PHContent(responseData);


            if (content.url != null) { // should be showing yet another 'sub interstitial' like "More Games"

                // display a "nested" or sub-content activity based off of this initial one.
                // this new content displayer will take over the listener of the initial one.
                contentDisplayer.get().launchNestedContentDisplayer(content);

                // Note:
                // We notify the interstitial template via a javascript callback. The interstitial template
                // usually then makes a dismiss call which hides us to show the underlying "sub interstitial"
                // Thus, the *template* dismisses us *after* we have already displayed the new interstitial

                sendResponseToWebview(request.getWebviewCallback(), responseData, null);
            } else {
                try {
                    JSONObject error_dict = new JSONObject();
                    error_dict.put("error", "1");
                    sendResponseToWebview(request.getWebviewCallback(), responseData, error_dict);


                    // inform the original requester of the error
                    notifyRequesterOfError(PHContentEnums.Error.FailedSubrequest);


                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) { //swallow all exceptions
            PHCrashReport.reportCrash(e, "PHInterstitialActivity - requestSucceeded(request, responseData)", PHCrashReport.Urgency.critical);
        }
    }

    @Override
    public void onSubContentRequestFailed(PHSubContentRequest request, PHError e) {
        try {
            if (request == null) return;

            JSONObject error = new JSONObject();
            error.putOpt("error", "1");
            PHSubContentRequest sub_request = (PHSubContentRequest) request;

            // tell the webview about the error
            sendResponseToWebview(sub_request.getWebviewCallback(), null, error);

            // notify the original requester
            notifyRequesterOfError(PHContentEnums.Error.FailedSubrequest);

        } catch (JSONException ex) {
            PHCrashReport.reportCrash(ex,  "PHInterstitialActivity - requestFailed(request, responseData)", PHCrashReport.Urgency.low);
        } catch (Exception exc) {
            PHCrashReport.reportCrash(exc, "PHInterstitialActivity - requestFailed(request, responseData)", PHCrashReport.Urgency.critical);
        }
    }
}
