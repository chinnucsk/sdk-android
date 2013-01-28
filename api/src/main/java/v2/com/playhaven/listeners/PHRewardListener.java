package v2.com.playhaven.listeners;

import v2.com.playhaven.requests.content.PHContentRequest;
import v2.com.playhaven.model.PHReward;

public interface PHRewardListener {
	public void onUnlockedReward(PHContentRequest request, PHReward reward);
}
