package com.playhaven.src.publishersdk.open;

import v2.com.playhaven.listeners.PHPrefetchListener;
import v2.com.playhaven.requests.open.PHOpenRequest;

/**
 * Simple object adapter for the prefetch listener.
 */
public class PrefetchDelegateAdapter implements PHPrefetchListener {

    private PHPublisherOpenRequest.PrefetchListener delegate;

    public PrefetchDelegateAdapter(PHPublisherOpenRequest.PrefetchListener delegate) {
        this.delegate = delegate;
    }

    @Override
    public void onPrefetchFinished(PHOpenRequest request) {
        delegate.prefetchFinished((PHPublisherOpenRequest) request);
    }
}
