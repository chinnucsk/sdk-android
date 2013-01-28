package com.playhaven.src.publishersdk.content.adapters;

import v2.com.playhaven.listeners.PHPurchaseListener;
import v2.com.playhaven.model.PHPurchase;
import v2.com.playhaven.requests.content.PHContentRequest;
import com.playhaven.src.publishersdk.content.PHPublisherContentRequest;

/**
 * Adapter for old purchase listener.
 */
public class PurchaseDelegateAdapter implements PHPurchaseListener {

    /** the adaptee */
    private PHPublisherContentRequest.PurchaseDelegate delegate;

    public PurchaseDelegateAdapter(PHPublisherContentRequest.PurchaseDelegate delegate) {
        this.delegate = delegate;
    }

    @Override
    public void onMadePurchase(PHContentRequest request, PHPurchase purchase) {
        delegate.shouldMakePurchase((PHPublisherContentRequest) request,
                                     new com.playhaven.src.publishersdk.content.PHPurchase(purchase));
    }
}
