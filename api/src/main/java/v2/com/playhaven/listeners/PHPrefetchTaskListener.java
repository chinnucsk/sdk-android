package v2.com.playhaven.listeners;

/** 
 * This listener interface is <em>only</em> used internally and corresponds
 * to the PHPrefetchTask for downloading resources from a URL. On the other hand,
 * the {@link PHPrefetchListener} should be used externally when making an open request
 * for pre-fetching.
 * @author samstewart
 *
 */
public interface PHPrefetchTaskListener {
	public void onPrefetchDone(int result);
}
