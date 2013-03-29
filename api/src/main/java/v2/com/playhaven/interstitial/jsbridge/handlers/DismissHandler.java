package v2.com.playhaven.interstitial.jsbridge.handlers;

import org.json.JSONObject;

/**
 * Handles a dismiss order from the content templates.
 * It extends launch handler because it needs to notify the webview
 * of a successful ping request. The {@link LaunchHandler} implements
 * the {@link v2.com.playhaven.utils.PHURLOpener.Listener} callback methods.
 */
public class DismissHandler extends LaunchHandler {

    @Override
    public void handle(JSONObject jsonPayload) {


        String closePingUrl = 	(jsonPayload != null 			  ?
                                 jsonPayload.optString("ping", "") :
                                 null);



        // send the ping back to the server but don't launch the url
        contentDisplayer.get().openURL(closePingUrl, this);

        // we will dismiss the content displayer after the URL loader finishes
        // see (LaunchHandler)
    }
}
