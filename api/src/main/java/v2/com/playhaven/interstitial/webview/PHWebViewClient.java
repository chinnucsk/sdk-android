package v2.com.playhaven.interstitial.webview;

import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import v2.com.playhaven.configuration.PHConfiguration;
import v2.com.playhaven.interstitial.jsbridge.ManipulatableContentDisplayer;
import v2.com.playhaven.interstitial.requestbridge.bridges.ContentRequestToInterstitialBridge;
import v2.com.playhaven.requests.crashreport.PHCrashReport;
import v2.com.playhaven.interstitial.jsbridge.PHJSBridge;
import v2.com.playhaven.model.PHContent;
import v2.com.playhaven.utils.PHStringUtil;

import java.lang.ref.WeakReference;

/**
 * Our own personal WebViewClient so that we can handle the callbacks
 * from the webview content templates. We have to be careful about the implicit inner
 * reference to the enclosing context.
 */
public class PHWebViewClient extends WebViewClient {

    /** our reference to the content displayer*/
    private WeakReference<ManipulatableContentDisplayer> contentDisplayer;

    /**
     * our reference to the bridge between this webview and
     * the native code.
     */
    private PHJSBridge bridge;

    /**
     * our reference to the content the webview is displaying.
     */
    private PHContent content;


    private PHConfiguration config;

    public PHWebViewClient(ManipulatableContentDisplayer contentDisplayer, PHJSBridge bridge, PHContent content) {
        super();
        this.bridge = bridge;
        this.contentDisplayer = new WeakReference<ManipulatableContentDisplayer>(contentDisplayer);
        this.config           = new PHConfiguration();
        this.content          = content;
    }

    /** Simple method to check if we have a valid content displayer */
    public boolean doesntHaveContentDisplayer() {
        return (this.contentDisplayer == null || this.contentDisplayer.get() == null);
    }

    /**
     * Notify listeners that the webview has loaded only if we just loaded the template.
     * We only check that the url starts with the template url to avoid trailing space issues
     */
    @Override
    public void onPageFinished(WebView webview, String url) {


        if (doesntHaveContentDisplayer()) return;

        // if we just loaded the template...
        if (url.startsWith(content.url.toString())) {
            String event = ContentRequestToInterstitialBridge.InterstitialEvent.Loaded.toString();

            Bundle message = new Bundle();

            message.putParcelable(ContentRequestToInterstitialBridge.InterstitialEventArgument.Content.getKey(), content);

            // tell the content requester that we have loaded
            contentDisplayer.get().sendEventToRequester(event, message);
        }
    }

    @Override
    public void onLoadResource(WebView view, String url) {
			/*
			 * Note: We have to listen in both 'onLoadResource' and 'shouldOverrideUrlLoading'
			 * because Android does not call 'shouldOverrideUrlLoading' after the initial
			 * iframe 'src' parameter is set. Since we use iframes for making the request
			 * to the native SDK, this is somewhat problematic. We need iframes to ensure
			 * the timers in the interstitial template are respected so modification was not an option.
			 * However, to avoid slowdown, we filter for "ph://" urls here since *every* resource
			 * request is routed through this point. Here's to hoping the Android dev team fixes this!
			 *
			 * Most of the requests appear to route through the shouldOverrieUrlLoading
			 * handler. However, requests such as the ph://closeButton only route
			 * through the onLoadResource handler. We avoid "double intercepting" the requests
			 * routed through shouldOverrideUrlLoading because they were 'overridden'.
			 */

        try {
            if (url.startsWith("ph://")) routePlayhavenCallback(url);

        } catch (Exception e) { // swallow all exceptions
            PHCrashReport.reportCrash(e, "PHWebViewClient - onLoadResource", PHCrashReport.Urgency.critical);
        }
    }

    @Override
    public void onReceivedError(WebView view, int errorCode,
                                String description, String failingUrl) {

        if (doesntHaveContentDisplayer()) return;

        try {
            // just blank out the webview
            view.loadUrl("");

            description = String.format("Error loading template at url: %s Code: %d Description: %s", failingUrl, errorCode, description);

            PHStringUtil.log(description);

            // inform the listener of the problem
            Bundle message = new Bundle();

            String event = ContentRequestToInterstitialBridge.InterstitialEvent.Failed.toString();
            message.putString(event, description);

            contentDisplayer.get().sendEventToRequester(event, message);

        } catch (Exception e) { // swallow all exceptions
            PHCrashReport.reportCrash(e, "PHWebViewClient - onReceivedError", PHCrashReport.Urgency.low);
        }

    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView webview, String url) {
        try {
            // route the callback and return whether or not
            // a route exists.
            return routePlayhavenCallback(url);
        } catch (Exception e) { // swallow all exceptions
            PHCrashReport.reportCrash(e, "PHWebViewClient - shouldOverrideUrlLoading", PHCrashReport.Urgency.critical);
        }

        return false;
    }


    /** Check to see if a route exists for the given url.
     * If a route does exist, perform the routing and call
     * the {@link v2.com.playhaven.interstitial.jsbridge.handlers.AbstractHandler}
     * @param url The url we wish to resolve against the routes
     * @return true if a route exists and we have called the appropriate handler, false otherwise.
     */
    private boolean routePlayhavenCallback(String url) {
        PHStringUtil.log("Received webview callback: " + url);

        try {
            if (bridge.hasRoute(url)) {
                bridge.route(url);
                return true;
            }

        } catch (Exception e) { // swallow all errors
            PHCrashReport.reportCrash(e, "PHWebViewClient - url routing", PHCrashReport.Urgency.critical);
        }

        return false;
    }
}
