package v2.com.playhaven.listeners;

import v2.com.playhaven.requests.content.PHContentRequest;
import v2.com.playhaven.model.PHPurchase;

public interface PHPurchaseListener {
	public void onMadePurchase(PHContentRequest request, PHPurchase purchase);
}
