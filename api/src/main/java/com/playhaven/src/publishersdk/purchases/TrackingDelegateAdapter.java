package com.playhaven.src.publishersdk.purchases;

import v2.com.playhaven.listeners.PHIAPRequestListener;
import v2.com.playhaven.model.PHError;
import v2.com.playhaven.requests.purchases.PHIAPTrackingRequest;
import com.playhaven.src.common.PHAPIRequest;
import org.json.JSONObject;

/**
 * An adapter for the {@link v2.com.playhaven.listeners.PHIAPRequestListener}
 */
public class TrackingDelegateAdapter implements PHIAPRequestListener {

    private PHAPIRequest.Delegate delegate;

    public TrackingDelegateAdapter(PHAPIRequest.Delegate delegate) {
        this.delegate = delegate;
    }

    @Override
    public void onIAPRequestSucceeded(PHIAPTrackingRequest request) {
        delegate.requestSucceeded((PHPublisherIAPTrackingRequest) request, new JSONObject());
    }

    @Override
    public void onIAPRequestFailed(PHIAPTrackingRequest request, PHError error) {
        delegate.requestFailed((PHPublisherIAPTrackingRequest) request, new Exception(error.getMessage()));
    }
}
