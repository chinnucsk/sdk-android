package com.playhaven.src.publishersdk.open;

import v2.com.playhaven.listeners.PHOpenRequestListener;
import v2.com.playhaven.model.PHError;
import v2.com.playhaven.requests.open.PHOpenRequest;
import com.playhaven.src.common.PHAPIRequest;
import org.json.JSONObject;

/**
 * Simple adapter between traditional {@link com.playhaven.src.common.PHAPIRequest} listener
 * and the modern {@link v2.com.playhaven.listeners.PHContentRequestListener}. This adapter
 * only handles the base API callbacks.
 */
public class APIRequestDelegateAdapter implements PHOpenRequestListener {

    private PHAPIRequest.Delegate delegate;

    public APIRequestDelegateAdapter(PHAPIRequest.Delegate delegate) {
        this.delegate = delegate;
    }

    @Override
    public void onOpenSuccessful(PHOpenRequest request) {
        // we pass in a blank JSON object
        delegate.requestSucceeded((PHPublisherOpenRequest) request, new JSONObject());
    }

    @Override
    public void onOpenFailed(PHOpenRequest request, PHError error) {
        delegate.requestFailed((PHPublisherOpenRequest) request, new Exception(error.getMessage()));
    }
}
