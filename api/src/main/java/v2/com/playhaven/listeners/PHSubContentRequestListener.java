package v2.com.playhaven.listeners;

import v2.com.playhaven.model.PHError;
import v2.com.playhaven.requests.content.PHSubContentRequest;
import org.json.JSONObject;

/**
 * Simple listener for the {@link v2.com.playhaven.requests.content.PHSubContentRequest}.
 */
public interface PHSubContentRequestListener {
    public void onSubContentRequestSucceeded(PHSubContentRequest request, JSONObject json);
    public void onSubContentRequestFailed(PHSubContentRequest request, PHError error);
}
