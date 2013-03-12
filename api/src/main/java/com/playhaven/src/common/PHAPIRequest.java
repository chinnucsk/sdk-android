package com.playhaven.src.common;

import org.json.JSONObject;

/**
 * Simple adapter for old PHAPIRequest interface. Most important entry it contains is the
 * listener. However, it has to be an interface for the shim layer class hierarchy to work properly.
 */
public interface PHAPIRequest {

    public static interface Delegate {
        public void requestSucceeded(PHAPIRequest request, JSONObject responseData);
        public void requestFailed(PHAPIRequest request, Exception e);
    }

    /** Sets the listener for this request. */
    public void setDelegate(Delegate delegate);

    /** sends the request */
    public void send();
}
