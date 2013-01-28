package com.playhaven.src.common;

import org.json.JSONObject;

/**
 * Simple adapter for old PHAPIRequest interface. Most important entry it contains is the
 * listener. However, it has to be an interface for the shim layer class hierarchy to work properly.
 * @deprecated
 */
public interface PHAPIRequest {

    /** The deprecated listener interface
     * @deprecated As of 1.12.2, you should implement the listeners in the listeners package.
     *             @see v2.com.playhaven.listeners.PHContentRequestListener
     *             @see v2.com.playhaven.listeners.PHRewardListener
     *             @see v2.com.playhaven.listeners.PHPurchaseListener
    */
    public static interface Delegate {
        public void requestSucceeded(PHAPIRequest request, JSONObject responseData);
        public void requestFailed(PHAPIRequest request, Exception e);
    }

    /** Sets the listener for this request. */
    public void setDelegate(Delegate delegate);

    /** sends the request */
    public void send();
}
