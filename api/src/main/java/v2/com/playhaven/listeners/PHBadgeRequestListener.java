package v2.com.playhaven.listeners;

import v2.com.playhaven.model.PHError;
import v2.com.playhaven.requests.badge.PHBadgeRequest;
import org.json.JSONObject;

/**
 * Simple listener interface all clients who send a badge request should implement.
 */
public interface PHBadgeRequestListener {
    public void onBadgeRequestSucceeded(PHBadgeRequest request, JSONObject responseData);
    public void onBadgeRequestFailed(PHBadgeRequest request, PHError error);
}
