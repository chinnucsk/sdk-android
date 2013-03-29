package v2.com.playhaven.interstitial.jsbridge;

import android.os.Bundle;
import v2.com.playhaven.model.PHContent;
import v2.com.playhaven.requests.content.PHSubContentRequest;
import v2.com.playhaven.utils.PHURLOpener;

/**
 * Abstract interface all content displayers should implement. The
 * {@link v2.com.playhaven.interstitial.jsbridge.handlers.AbstractHandler} and the
 * respective subclasses need this interface to "manipulate" the content displayer.
 *
 * Shares many methods with the {@link v2.com.playhaven.interstitial.requestbridge.base.ContentDisplayer} interface.
 *
 * Unfortunately, since the handlers are tightly coupled with the content displayer this interface is a bit
 * specific. If you have any ideas for decoupling the handlers, suggestions are welcome. The main problem
 * is that the handlers actually belong <em>in</em> the content displayer but we wanted to refactor them
 * out to make the code readable (shorter).
 */
public interface ManipulatableContentDisplayer {

    /** Called to get the unique tag used for identifying the
     * content view on the bridge between the content displayer
     * and the content requester.
     *
     * @see v2.com.playhaven.interstitial.requestbridge.BridgeManager
     */
    public String getTag();

    /** Gets the content this content displayer is showing*/
    public PHContent getContent();

    /** Called to dismiss the content displayer */
    public void dismiss();

    /** Enables the displayer to be closable.
     * In practical terms this includes actions such
     * as enabling a close button.
     */
    public void enableClosable();

    /**
     * Restricts the displayer from beingn closable.
     */
    public void disableClosable();

    /**
     * Queries the content displayer on the status of "closability".
     */
    public boolean isClosable();

    /**
     * Launches a "nested" content displayer from
     * this content displayer. This nested receiver should
     * share the same tag, same close button, etc.
     * TODO: this might become problematic because we are effectively
     * replacing the old one
     */
    public void launchNestedContentDisplayer(PHContent content);

    /** Gets the secret from {@link v2.com.playhaven.configuration.PHConfiguration}. Handlers
     * can't do this without a context.
     */
    public String getSecret();

    /** Gets teh device ID from {@link v2.com.playhaven.configuration.PHConfiguration}. Handlers can't do this without a context.*/
    public String getDeviceID();

    /**
     * Opens a {@link v2.com.playhaven.utils.PHURLOpener} from the content displayer
     * context. It does not launch the final url.
     * @param url the url we wish to open
     * @param listener the listener for the url opener
     */
    public void openURL(String url, PHURLOpener.Listener listener);

    /**
     * Launches a {@link v2.com.playhaven.utils.PHURLOpener} from the content displayer
     * context. It opens the final url with the appropriate external app on the device.
     * @param url the url we wish to open and launch
     * @param listener the listener for the url opener
     */
    public void launchURL(String url, PHURLOpener.Listener listener);

    /**
     * Launches a new {@link v2.com.playhaven.requests.content.PHSubContentRequest}.
     */
    public void launchSubRequest(PHSubContentRequest request);

    /**
     * Notifies the underlying {@link v2.com.playhaven.interstitial.requestbridge.base.ContentRequester} of this
     * content displayer of an event. This event will traverse
     * a {@link v2.com.playhaven.interstitial.requestbridge.base.RequestBridge}.
     */
    public void sendEventToRequester(String event, Bundle message);

}
