package v2.com.playhaven.requests.open;


import android.content.Context;
import com.playhaven.src.utils.PHStringUtil;
import v2.com.playhaven.cache.PHCache;
import v2.com.playhaven.configuration.PHConfiguration;
import v2.com.playhaven.listeners.PHOpenRequestListener;
import v2.com.playhaven.listeners.PHPrefetchListener;
import v2.com.playhaven.listeners.PHPrefetchTaskListener;
import v2.com.playhaven.model.PHError;
import v2.com.playhaven.requests.base.PHAPIRequest;
import v2.com.playhaven.requests.crashreport.PHCrashReport;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Hashtable;
import java.util.concurrent.ConcurrentLinkedQueue;

public class PHOpenRequest extends PHAPIRequest implements PHPrefetchTaskListener {
	private ConcurrentLinkedQueue<PHPrefetchTask> prefetchTasks = new ConcurrentLinkedQueue<PHPrefetchTask>();
	
	/**
     * This flag indicates whether or not we should precede with pre-caching after receiving a response or if
	 * we should instead wait for a direct command. Only used for internal unit testing and by default true.
	 */
	public boolean startPrecachingImmediately = true;

    /** Used to notify listeners of the prefetch task
     * finishing.
     */
	private PHPrefetchListener prefetch_listener;
	
	private PHOpenRequestListener open_listener;

    private boolean shouldPrecache;

	private PHSession session;

    private PHConfiguration config;
	
	public void setPrefetchListener(PHPrefetchListener listener) {
		this.prefetch_listener = listener;
	}
	
	public void setOpenRequestListener(PHOpenRequestListener listener) {
		this.open_listener = listener;
	}

    public PHOpenRequestListener getOpenRequestListener() {
        return this.open_listener;
    }

    public PHPrefetchListener getPrefetchListener() {
        return this.prefetch_listener;
    }

	public PHSession getSession() {
		return session;
	}
	
	public ConcurrentLinkedQueue<PHPrefetchTask> getPrefetchTasks() {
		return prefetchTasks;
	}
	
	public PHOpenRequest(PHOpenRequestListener listener) {
		this();
		this.open_listener = listener;
	}

	
	public PHOpenRequest() {
		super();


        config = new PHConfiguration();
	}

    ///////////////////////////////////////////////////////////////
    //////////////////////// Overrides /////////////////////////////
	
	@Override
	public String baseURL(Context context) {
		return super.createAPIURL(context, "/v3/publisher/open/");
	}
	
	@Override
	public void send(Context context) {

        // we need to pre-cache this value because we won't
        // always have a context handle
        shouldPrecache = config.getShouldPrecache(context);

        // initialize the cache
        synchronized (PHOpenRequest.class) {
            if (shouldPrecache) {
                synchronized (PHCache.class) {

                    PHCache.installCache(context);
                }
            }
        }

        session = PHSession.getInstance(context);

		// Note: ordering is important! You *must* call session.start() *before* sending the request
	    session.start();

	    super.send(context);
	    
	}
	
	/**
	 * We override this method so that we can inform our own listener of the error.
	 */
	@Override
	public void handleRequestFailure(PHError e) {
		if (open_listener != null) 
			open_listener.onOpenFailed(this, e);
	}

    /**
     * We override this method so that we can control how we notify of successes.
     */
	@Override
	public void handleRequestSuccess(JSONObject res) {

        PHStringUtil.log("Open request received a response...should we precache: " + shouldPrecache);

        // if we should be pre-caching the *interstitial template* start doing so
		if (res != null    &&
            shouldPrecache && res.has("precache") ) {
			prefetchTasks.clear();

            // go through each of the elements we should precache
            // and kick off a new PHPrefetchTask
			JSONArray precached = res.optJSONArray("precache");

			if (precached != null) {	
				
				for (int i = 0; i < precached.length(); i++) {
					String url = precached.optString(i);

                    // spawns off another PHPrefetchTask
					if (url != null) {
						PHPrefetchTask task = new PHPrefetchTask();
						task.setPrefetchListener(this);

						task.setURL(url);


						prefetchTasks.add(task);
					}
					
				}
			}
			
			// start fetching the pre-cached elements
			if (startPrecachingImmediately)
				startNextPrefetch();
		}
		
		session.startAndReset();
		
		// we don't notify the API listener but instead our own listener
		if (open_listener != null)
			open_listener.onOpenSuccessful(this);
		
	}
	
	public void startNextPrefetch() {
        PHStringUtil.log("Starting precache task with a total of: " + prefetchTasks.size());

		if (prefetchTasks.size() > 0)
            prefetchTasks.poll().execute();
	}
	
	///////////////////////////////////////////////////////////
	//////////////////// Prefetch Listener ////////////////////
	
	@Override
	public void onPrefetchDone(int result) {
		try {
            PHStringUtil.log("Pre-cache task done. Starting next out of " + prefetchTasks.size());

            // if this is the last prefetch task finishing, notify the listener
            if (prefetchTasks.size() == 0 && prefetch_listener != null) {
                prefetch_listener.onPrefetchFinished(this);

                return;
            }


			// previous prefetch is finished, start the next one
			if (prefetchTasks.size() > 0 && startPrecachingImmediately)
				startNextPrefetch();
			
		} catch (Exception e) { // swallow all exceptions
			PHCrashReport.reportCrash(e, "PHOpenRequest - prefetchDone", PHCrashReport.Urgency.low);
		}

	}
	
	@Override
    public Hashtable<String, String> getAdditionalParams(Context context) {
	    Hashtable<String, String> params = new Hashtable<String, String>();
	    
	    params.put("ssum",      String.valueOf(session.getTotalTime()));
	    params.put("scount",    String.valueOf(session.getSessionCount()));

        // if we should pre-cache, we need to tell the server
        if (shouldPrecache)
            params.put("precache",  "1");
	    
	    return params;
	}

}
