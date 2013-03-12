package com.playhaven.src.publishersdk.content;

/**
 * Class adapter for {@link v2.com.playhaven.model.PHReward}.
 */
public class PHReward extends v2.com.playhaven.model.PHReward {

    /** Converts a new PHReward to an old one */
    public PHReward(v2.com.playhaven.model.PHReward reward) {
        this.name       = reward.name;
        this.quantity   = reward.quantity;
        this.receipt    = reward.receipt;
    }
}
