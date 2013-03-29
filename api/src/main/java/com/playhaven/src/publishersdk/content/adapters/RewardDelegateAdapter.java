package com.playhaven.src.publishersdk.content.adapters;

import v2.com.playhaven.listeners.PHRewardListener;
import v2.com.playhaven.model.PHReward;
import v2.com.playhaven.requests.content.PHContentRequest;
import com.playhaven.src.publishersdk.content.PHPublisherContentRequest;

/**
 * Adapts the old reward listener to the new {@link v2.com.playhaven.listeners.PHRewardListener}
 */
public class RewardDelegateAdapter implements PHRewardListener {

    /** the adaptee */
    private PHPublisherContentRequest.RewardDelegate delegate;

    public RewardDelegateAdapter(PHPublisherContentRequest.RewardDelegate delegate) {
        this.delegate = delegate;
    }

    @Override
    public void onUnlockedReward(PHContentRequest request, PHReward reward) {
        delegate.unlockedReward((PHPublisherContentRequest) request, new com.playhaven.src.publishersdk.content.PHReward(reward));
    }
}
