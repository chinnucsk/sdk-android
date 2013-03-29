package v2.com.playhaven.requests.open;

import android.os.AsyncTask;
import v2.com.playhaven.cache.PHCache;
import v2.com.playhaven.requests.crashreport.PHCrashReport;
import v2.com.playhaven.listeners.PHPrefetchTaskListener;
import v2.com.playhaven.utils.PHStringUtil;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Simple task to download a resource and store it in the {@link PHCache}.
 */
public class PHPrefetchTask extends AsyncTask<Integer, Integer, Integer> {
	
	public URL url;

    private static final String GZIP_ENCODING = "gzip";

	public PHPrefetchTaskListener listener;
	
	///////////////////////////////////////////
	//////////////// Accessors ///////////////

	public void setPrefetchListener(PHPrefetchTaskListener listener) {
		this.listener = listener;
	}

    public PHPrefetchTaskListener getPrefetchListener() {
        return this.listener;

    }
		
	public URL getURL() {
		return url;
	}
	
	public void setURL(String url) {
		
		try {
			this.url = new URL(url);
		} catch (MalformedURLException e) {
			this.url = null;
			PHStringUtil.log("Malformed URL in PHPrefetchTask: " + url);
		}
	}
	
	@Override
	protected Integer doInBackground(Integer... dummys) {

		int responseCode = HttpStatus.SC_BAD_REQUEST;

        if (! PHCache.hasBeenInstalled()) return responseCode;

		// Note: while HttpURLConnection might be simpler, we use the Apache
		// libraries for easier testing with Robolectric

		try {
            // we carefully lock to ensure we're alone when using the cache
			synchronized (PHCache.getSharedCache()) {
                if (url == null) {
                    return HttpStatus.SC_BAD_REQUEST;
                }
                
                DefaultHttpClient client = new DefaultHttpClient();
                HttpGet request = new HttpGet(url.toString());
                request.addHeader("Accept-Encoding", "gzip");
                
                HttpResponse response = client.execute(request);
                
                responseCode = response.getStatusLine().getStatusCode();
                if (responseCode != HttpStatus.SC_OK) {
                    return responseCode;
                }
                
                HttpEntity entity = response.getEntity();

                // determine if the interstitial is compressed
                Header contentEncoding = null;
                try {
                	contentEncoding = entity.getContentEncoding();
                } catch (UnsupportedOperationException e){
                	// robolectric (for testing) doesn't support this
                }

                boolean isCompressed = (contentEncoding == null) ? false : contentEncoding.getValue().equals(GZIP_ENCODING);

                // stick this thing in the cache
                PHStringUtil.log("Prefetch done....caching file");

                PHCache.getSharedCache().cacheFile(url, entity.getContent(), isCompressed);
			}
		} catch (Exception e) { // swallow all exceptions
			PHCrashReport.reportCrash(e, "PHPrefetchTask - doInBackground", PHCrashReport.Urgency.low);
		}
		
		return responseCode;
	}
	
	@Override
	protected void onProgressUpdate(Integer... progress) {
		// Don't actually do anything...
	}
	
	@Override
	protected void onPostExecute(Integer result) {
		
		// don't catch exceptions from listener
		if (listener != null) listener.onPrefetchDone(result);
	}
}
