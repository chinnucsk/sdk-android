package v2.com.playhaven.listeners;

import v2.com.playhaven.requests.open.*;

/**
 * <p>
 * The public interface clients should implement if they
 * wish to be notified of the prefetch finishing in an open request.
 * </p>
 *
 * <p>
 *     This interface is distinguished from {@link PHPrefetchTaskListener} because
 *     publishers should use this interface to
 * </p>
 */
public interface PHPrefetchListener {
	public void onPrefetchFinished(PHOpenRequest request);
}
