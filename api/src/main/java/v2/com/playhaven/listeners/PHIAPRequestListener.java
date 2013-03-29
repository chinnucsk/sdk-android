package v2.com.playhaven.listeners;

import v2.com.playhaven.model.PHError;
import v2.com.playhaven.requests.purchases.PHIAPTrackingRequest;

/**
 * Simple listener interface all clients who send a IAP tracking request should implement.
 */
public interface PHIAPRequestListener {
    public void onIAPRequestSucceeded(PHIAPTrackingRequest request);
    public void onIAPRequestFailed(PHIAPTrackingRequest request, PHError error);
}

