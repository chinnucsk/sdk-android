package com.playhaven.src.publishersdk.metadata;

import v2.com.playhaven.listeners.PHBadgeRequestListener;
import v2.com.playhaven.model.PHError;
import v2.com.playhaven.requests.badge.PHBadgeRequest;
import com.playhaven.src.common.PHAPIRequest;
import org.json.JSONObject;

/**
 * Adapter from {@link v2.com.playhaven.utils.PHURLOpener.Listener} to {@link v2.com.playhaven.listeners.PHBadgeRequestListener}.
 */
public class MetadataDelegateAdapter implements PHBadgeRequestListener {

    private PHAPIRequest.Delegate delegate;

    public MetadataDelegateAdapter(PHAPIRequest.Delegate delegate) {
        this.delegate = delegate;
    }

    @Override
    public void onBadgeRequestSucceeded(PHBadgeRequest request, JSONObject responseData) {
        delegate.requestSucceeded((PHPublisherMetadataRequest) request, responseData);
    }

    @Override
    public void onBadgeRequestFailed(PHBadgeRequest request, PHError error) {
        delegate.requestFailed((PHPublisherMetadataRequest) request, new Exception(error.getMessage()));
    }
}
