package v2.com.playhaven.listeners;

import java.nio.ByteBuffer;
import v2.com.playhaven.model.PHError;

public interface PHHttpRequestListener {
	public void onHttpRequestSucceeded(ByteBuffer response, int responseCode);
	public void onHttpRequestFailed(PHError error);

}