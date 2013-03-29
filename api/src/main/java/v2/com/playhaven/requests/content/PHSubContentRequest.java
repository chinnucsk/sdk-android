package v2.com.playhaven.requests.content;

import v2.com.playhaven.interstitial.PHInterstitialActivity;
import v2.com.playhaven.model.PHError;
import v2.com.playhaven.listeners.PHSubContentRequestListener;
import org.json.JSONObject;

import android.content.Context;

import v2.com.playhaven.requests.base.PHAPIRequest;
import v2.com.playhaven.utils.PHStringUtil;

/**
 * <p>
 * A generic API request that the interstitial templates can initiate
 * with the ph://subrequest callback. It is most often used as a <em>interstitial</em>
 * sub-request.
 * </p>
 *
 * <p>
 *     This request is the same as a regular content request except that it contains some metadata
 *     such as the webview callback. The main {@link PHInterstitialActivity} spawns this sub-content
 *     request off in response to the webview sending a ph://subrequest slug over the JS bridge.
 * </p>
 */
public class PHSubContentRequest extends PHAPIRequest {
	
	private String webviewCallback;

    private PHSubContentRequestListener listener;

	public PHSubContentRequest(PHSubContentRequestListener listener) {
		super();

        this.listener = listener;
	}

    /** Sets the listener for this request.
     *
     * @param listener the listener
     */
    public void setSubContentReuqestListener(PHSubContentRequestListener listener) {
        this.listener = listener;
    }

    /** Gets the listener for this request.
     *
     * @return the listener or null if one has not yet been set.
     */
    public PHSubContentRequestListener getSubContentListener() {
        return this.listener;
    }

    /** Sets the webview callback used for notifying the webview
     * that this request has finished.
     * @param callback the webview callback
     */
    public void setWebviewCallback(String callback) {
        this.webviewCallback = callback;
    }

    /**
     * Gets the webview callback used for notifying the webview
     * that this request has finished.
     */
    public String getWebviewCallback() {
        return this.webviewCallback;
    }

    ////////////////////////////////////////////////
    //////////////// Overrides /////////////////////

    @Override
    public void handleRequestSuccess(JSONObject json) {
        if (listener != null)
            listener.onSubContentRequestSucceeded(this, json);
    }

    @Override
    public void handleRequestFailure(PHError error) {
        if (listener != null)
            listener.onSubContentRequestFailed(this, error);
    }

	@Override
	public String getURL(Context context) {

		if (this.fullUrl == null) {
			// we don't want any prefix or signed params, just the base url.
			// This will have been set externally.
			this.fullUrl = this.baseURL(context);
		}
		return this.fullUrl;
	}
	
	@Override
	public void send(Context context) {
		if ( ! JSONObject.NULL.equals(baseURL(context)) 	&&
		       baseURL(context).length() > 0				  ) {
			
			super.send(context);
			return;
		}

		PHStringUtil.log("No URL set for PHSubContentRequest");
		
	}
}
