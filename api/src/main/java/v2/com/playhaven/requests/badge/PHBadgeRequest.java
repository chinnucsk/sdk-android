package v2.com.playhaven.requests.badge;

import java.util.Hashtable;

import android.content.Context;
import v2.com.playhaven.interstitial.PHContentEnums;
import v2.com.playhaven.listeners.PHBadgeRequestListener;
import v2.com.playhaven.requests.base.PHAPIRequest;
import v2.com.playhaven.model.PHError;
import org.json.JSONObject;

/**
 * Request for the {@link v2.com.playhaven.views.badge.PHBadgeView}.
 */
public class PHBadgeRequest extends PHAPIRequest {

    /** The placement we are checking for a new badge number */
	public String placement = "";

    /** The special listener for the badge request*/
    private PHBadgeRequestListener listener;

	public PHBadgeRequest(String placement) {
		super();
		this.placement = placement;
	}
	
	public PHBadgeRequest(PHBadgeRequestListener listener, String placement) {
		this(placement);
		this.setMetadataListener(listener);
	}


    public PHBadgeRequestListener getListener() {
        return listener;
    }

    public void setMetadataListener(PHBadgeRequestListener listener) {
        this.listener= listener;
    }

    public PHBadgeRequestListener getMetadataListener() {
        return this.listener;
    }

	////////////////////////////////////////////////////
    ////////////////// Overrides ///////////////////////
    @Override
    public void handleRequestSuccess(JSONObject json) {

        // see if the request failed.
        if (json == null || JSONObject.NULL.equals(json)) {
            if (this.listener != null) {
                this.listener.onBadgeRequestFailed(this, new PHError(PHContentEnums.Error.NoResponseField.getMessage()));
            }

            return;
        }

        // otherwise, we succeeded
        if (this.listener != null)
            this.listener.onBadgeRequestSucceeded(this, json);
    }

    @Override
    public void handleRequestFailure(PHError error) {
        if (this.listener != null)
            this.listener.onBadgeRequestFailed(this, error);
    }

	@Override
	public String baseURL(Context context) {
		return super.createAPIURL(context, "/v3/publisher/content/");
	}
	@Override
	public Hashtable<String, String> getAdditionalParams(Context context) {
		Hashtable<String, String> params = new Hashtable<String, String>();
		
		if (placement != null)
			params.put("placement_id", this.placement);
		
		params.put("metadata", "1");
		
		return params;
	}
}
