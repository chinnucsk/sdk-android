package v2.com.playhaven.interstitial.jsbridge.handlers;

import v2.com.playhaven.requests.crashreport.PHCrashReport;
import v2.com.playhaven.interstitial.jsbridge.PHJSBridge;
import v2.com.playhaven.utils.PHStringUtil;
import org.json.JSONObject;

/**
 * Simple handler for close button request.
 */
public class CloseButtonHandler extends AbstractHandler {

    @Override
    public void handle(JSONObject jsonPayload) {
        try {

            if (jsonPayload == null) return;

            String shouldHide = jsonPayload.optString("hidden");

            PHStringUtil.log("WebView asks us to hide close button: " + shouldHide);

            // should we hide the button?
            if ( ! JSONObject.NULL.equals(shouldHide) && shouldHide.length() > 0) {

                boolean shouldDisableClosable = Boolean.parseBoolean(shouldHide);

                if (shouldDisableClosable)
                    contentDisplayer.get().disableClosable();
                else
                    contentDisplayer.get().enableClosable();
            }

            JSONObject response = new JSONObject();

            // make certain that our changes took effect by querying
            // the actual state of closability
            response.put("hidden", (contentDisplayer.get().isClosable() ? "false" : "true"));

            // inform the webview that we took action
            sendResponseToWebview(bridge.getCurrentQueryVar("callback"), response, null);

        } catch (Exception e) { // swallow all exceptions
            PHCrashReport.reportCrash(e, "PHInterstitialActivity - handleCloseButton", PHCrashReport.Urgency.critical);
        }
    }
}
