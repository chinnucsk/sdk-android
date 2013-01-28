package v2.com.playhaven.interstitial.jsbridge.handlers;


import v2.com.playhaven.configuration.PHConfiguration;
import org.json.JSONObject;

/**
 * Handler for the webview load context event. The content template asks
 * for the JSON payload we downloaded and we send it back.
 */
public class LoadContextHandler extends AbstractHandler {

    /** The javascript command used for notifying the content template
     * of the JS bridge protocol version.
     */
    private static final String SET_PROTOCOL_JAVASCRIPT = "window.PlayHavenDispatchProtocolVersion = %d";

    @Override
    public void handle(JSONObject jsonObject) {


        PHConfiguration config = new PHConfiguration();

        // first tell the webview about the appropriate protocol for communication
        String javascriptCommand = String.format(

                SET_PROTOCOL_JAVASCRIPT,

                config.getJSBridgeProtocolVersion()
        );

        // sends the new version
        bridge.runJavascript(javascriptCommand);

        // now provide the webview with the appropriate context
        JSONObject downloadedContent = contentDisplayer.get().getContent().context;

        sendResponseToWebview(bridge.getCurrentQueryVar("callback"), downloadedContent, null);


    }
}
