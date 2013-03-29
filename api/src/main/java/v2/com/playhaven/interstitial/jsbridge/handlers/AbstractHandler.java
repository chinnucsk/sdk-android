package v2.com.playhaven.interstitial.jsbridge.handlers;

import v2.com.playhaven.interstitial.jsbridge.ManipulatableContentDisplayer;
import v2.com.playhaven.interstitial.jsbridge.PHJSBridge;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;

/**
 * Generic interface for JS bridge webview callbacks such as ph://reward, ph://purchase, etc.
 */
public abstract class AbstractHandler {

    /** A reference to the containing
     * {@link PHJSBridge}.
     */
    protected PHJSBridge bridge;

    /** A weak reference to the content displayer so that
     * we can update its state depending on the webview callbacks we receive.
     * Subclasses need this to actually affect the content displayer.
     */
    protected WeakReference<ManipulatableContentDisplayer> contentDisplayer;

    /**
     * The core method called when this handler should act.
     * This method should not be overridden by subclasses.
     */
    public final void handle() {
        if (doesntHaveContentDisplayer()) return;

        handle(getRequestContext());
    }

    /** The method subclasses should override to provide
     * their custom behavior when handled.
     * @param jsonPayload The optional json context the webview
     *                    might pass back. This might be null.
     */
    protected abstract void handle(JSONObject jsonPayload);

    /** Attaches the bridge that this handler is part of. Must be called at
     * least once.
     * @param bridge the bridge that maps a route to this handler.
     */
    public void attachBridge(PHJSBridge bridge) {
        this.bridge = bridge;
    }

    /** Adds the content displayer. Must be called at least once. */
    public void attachContentDisplayer(ManipulatableContentDisplayer contentDisplayer) {
        this.contentDisplayer = new WeakReference<ManipulatableContentDisplayer>(contentDisplayer);
    }

    /** Checks to see if we have a valid content displayer */
    protected boolean doesntHaveContentDisplayer() {
        return (contentDisplayer == null || contentDisplayer.get() == null);
    }
    /**
     * Simple utility method for parsing the JSON context
     * the webview passes us.
     * @return The JSON object corresponding to the "context" parameter from the webview
     *         or null if there is no request context.
     */
    private JSONObject getRequestContext() {
        try {
            String contextStr = bridge.getCurrentQueryVar("context");

            JSONObject context;

            if (contextStr == null || contextStr.equals("undefined") || contextStr.equals("null"))
                context = new JSONObject();
            else
                context = new JSONObject(contextStr);


            if ( ! JSONObject.NULL.equals(context) &&
                    context.length() > 0 			 )
                return context;

        } catch (JSONException e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Sends a response to the webview contained within the router
     * that contains these handlers.
     * @param callback the playhaven callback we wish to call in the content template javascript
     * @param payload the message body we wish to send to the webview.
     * @param error   the error error we wish to send to the webview. This can be null.
     */
    protected void sendResponseToWebview(String callback, JSONObject payload, JSONObject error) {
        if (bridge == null) return;

        bridge.sendMessageToWebview(callback, payload, error);
    }

}
