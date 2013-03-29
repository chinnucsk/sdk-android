package v2.com.playhaven.listeners;

import v2.com.playhaven.model.PHError;
import v2.com.playhaven.requests.open.PHOpenRequest;

public interface PHOpenRequestListener {
	public void onOpenSuccessful(PHOpenRequest request);
	public void onOpenFailed(PHOpenRequest request, PHError error);
}
