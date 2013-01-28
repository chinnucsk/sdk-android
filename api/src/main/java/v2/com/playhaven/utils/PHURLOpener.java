package v2.com.playhaven.utils;

import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.util.LinkedHashSet;
import java.util.List;

import v2.com.playhaven.configuration.PHConfiguration;
import v2.com.playhaven.model.PHError;
import v2.com.playhaven.listeners.PHHttpRequestListener;
import v2.com.playhaven.requests.base.PHAsyncRequest;
import org.json.JSONObject;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;

/** 
 * 	A simple utility class that can download and open URLs on the device. It subclasses
 * 	{@link ProgressDialog} to demonstrate the URL opener is at work.
 *
 * 	We maintain references to the listeners only with weak references because our listeners
 * 	might disappear at any pointer and are often contexts so we don't wish to leak memory.
 */
public class PHURLOpener extends ProgressDialog  implements PHHttpRequestListener {

    /** The URL we wish to download and open */
	private String targetURL;

    /** the loading message we display when launching this PHURLLoader */
    private static final String LOADING_MESSAGE = "Loading...";

    /** the market URL used for displaying a game */
	private final String MARKET_URL_TEMPLATE = "http://play.google.com/marketplace/apps/details?id=%s";

    /** a flag indicating whether or not we should open
     * the resulting URL after re-directions via an external app.
     */
	private boolean shouldOpenUrl;

    /**
     * we maintain a weak reference to the listener because it is a context
     * and we don't want to leak the context.
     */
	private WeakReference<Listener> listener;

    /** a reference to the configuration values */
    private PHConfiguration config;

    /** a reference to the url connection for the request */
	private PHAsyncRequest conn;

    /** a flag indicating whether or not we are currently loading
     * the url.
     */
	private boolean isLoading = false;

    /**
     * the maximum of number of redirects
     * we can handler from our {@link #targetURL} before canceling.
     */
	private final int MAXIMUM_REDIRECTS = 10;
	
    /** the name of the callback for the webview callback template */
	private String contentTemplateCallback;


    /** The listener interface used for notifying
     * anyone interested in the progress of this URL opener.
     */
	public static interface Listener {
		public void onURLOpenerFinished(PHURLOpener loader);
		public void onURLOpenerFailed(PHURLOpener loader);
	}

    /** Constructor that creates a new opener with a listener
     * @param context a valid context for starting activities. Should
     *                be an {@link android.app.Activity}
     * @param listener a valid listener which is most often an {@link android.app.Activity}.
     */
	public PHURLOpener(Context context, Listener listener) {
		this(context); // call other constructor
		
		this.listener = new WeakReference<Listener>(listener);
	}

    /** Constructor that creates a new opener without a listener
     * @param context a valid context for starting activities. Should
     *                be an {@link android.app.Activity}
     */
	public PHURLOpener(Context context) {
        super(context);
		shouldOpenUrl = true; // open the final via the system

        config = new PHConfiguration();
	}

    /**
     * Allows the user to set a flag indicating whether
     * or not this URL opener should try to launch the
     * final url out of series of redirects via an external
     * intent.
     * @param shouldOpen a flag indicating whether or not we
     *                   should try to launch the final url via
     *                   an external intent.
     */
    public void setShouldOpenFinalURL(boolean shouldOpen) {
        this.shouldOpenUrl = shouldOpen;
    }

    /**
     * Determines whether or not this URL opener is currently
     * loading a URL.
     * @return true if loading false if otherwise.
     */
    public boolean isLoading() {
        return isLoading;
    }

	///////////////////////////////////////////////////////////
    /** Sets the callback for the webview content templates */
	public void setContentTemplateCallback(String callback) {
		this.contentTemplateCallback = callback;
	}

    /** Gets the callback for the webview content template*/
	public String getContentTemplateCallback() {
		return contentTemplateCallback;
	}
	
	///////////////////////////////////////////////////////////
	////////////// Mostly Utilized by Unit Tests //////////////
	
	public PHAsyncRequest getConnection() {
		return conn;
	}
	
	public void setConnection(PHAsyncRequest conn) {
		this.conn = conn;
	}
	
	///////////////////////////////////////////////////////////

    /** Sets the URL we wish to download */
	public void setTargetURL(String url) {
		this.targetURL = url;
	}

    /** Gets the URL we wish to download */
	public String getTargetURL() {
		return this.targetURL;
	}



	
	///////////////////////////////////////////////////////////

    /** Opens the {@link #targetURL} and shows the {@link ProgressDialog}.
     * @param url the url we wish to open.
     */
	public void open(String url) {

        this.targetURL = url;

		if( ! JSONObject.NULL.equals(targetURL) &&
			  targetURL.length() > 0		      ) {

			
			PHStringUtil.log(String.format("Opening url in PHURLOpener: %s", targetURL));
			
			isLoading = true;

            // kick off a request
			conn                = new PHAsyncRequest(this);
			conn.setMaxRedirects(MAXIMUM_REDIRECTS);
			conn.request_type   = PHAsyncRequest.RequestType.Get;
			conn.execute        ( Uri.parse(targetURL) );


            this.setMessage(LOADING_MESSAGE);

            // show the progress dialog
            show();

			return;
		}
		
		// finish immediately if no target url
		if(listener != null && listener.get() != null)
			listener.get().onURLOpenerFinished(this);
	}

    /** Force-cancels this URL opener and closes the dialog.*/
	private void finish() {

		if( isLoading ) {
			isLoading = false;
			
			targetURL = conn.getLastRedirectURL();
			
			PHStringUtil.log("PHURLOpener - final redirect location: " + targetURL);
			
			if(shouldOpenUrl &&
			   targetURL != null 	 && 
			   !targetURL.equals("")) {
				
				if (targetURL.startsWith("market:"))

                    openMarketURL(targetURL); // I'll take a market please!
				
				else
                    launchActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(targetURL))); // fine, just handle default!
				
			}
				
			if(listener != null  && listener.get() != null)
				listener.get().onURLOpenerFinished(this);
			
			invalidate();
		}
	}

    /**
     * simple utility method for launching an activity.
     * @oaram intent the intent used for launching the activity
    */
	private void launchActivity(Intent intent) {

		if (config.getIsRunningUITests(getContext())) return; // never launch when UI testing...

        PHStringUtil.log("PHURLOpener just launched intent: " + intent.getData());

        getContext().startActivity(intent);
	}

    /**
    * Opens the given market URL. This method also
    * checks to ensure that the android market is installed.
    * @param url the market url we wish to open.
    */
	private void openMarketURL(String url) {

		PHStringUtil.log("Got a market:// URL, verifying market app is installed");

		// check if market is valid
		// (http://android-developers.blogspot.com/2009/01/can-i-use-this-intent.html)

		Intent intent = new Intent(Intent.ACTION_VIEW);
		intent.setData(Uri.parse(url));

		// do we have the market app installed?
        // we check to make sure the market app is installed
		PackageManager packageManager = getContext().getPackageManager();
		List<ResolveInfo> resolveInfo = packageManager.queryIntentActivities(
				intent, PackageManager.MATCH_DEFAULT_ONLY);

		if (resolveInfo.size() == 0) {
			PHStringUtil.log("Market app is not installed and market:// not supported!");
			
			// since no market:// just open in the web browser!
			Uri uri = Uri.parse(
								String.format(MARKET_URL_TEMPLATE,
								Uri.parse(url).getQueryParameter("id"))
								);

			intent = new Intent(Intent.ACTION_VIEW, uri);
		}

        PHStringUtil.log("PHURLOpener is trying to launch: " + url);

		launchActivity(intent);
	}

    /**
     * cancels any underlying requests and closes this
     * URL opener.
     */
	private void invalidate() {
		listener = null;
		
		if (conn == null) return; // failed to start
		
		synchronized (this) {
			conn.cancel(true);
		}
		
		dismiss();
	}

    /**
     * Notifies the listener of failure and closes
     * this url loader.
     */
	private void fail() {
		if(listener != null && listener.get() != null)
			listener.get().onURLOpenerFailed(this);
		
		invalidate();
	}
	
	////////////////////////////////////////////////////
	/////// PHAsyncRequest listener methods ////////////
	////////////////////////////////////////////////////

    /**
     * Called when the underlying {@link #conn} finishes successfully.
     * @param response the raw http response
     * @param responseCode the response code from the webserver
     */
	@Override
	public void onHttpRequestSucceeded(ByteBuffer response, int responseCode) {
		if(responseCode < 300) {
			PHStringUtil.log("PHURLOpener finishing from initial url: " + targetURL);
			finish();
		} else {
			PHStringUtil.log("PHURLOpener failing from initial url: " + targetURL + " with error code: "+responseCode);
			fail();
		}
	}

    /**
     * Called when the underlying {@link #conn} fails.
     * @param e the error causing the failure.
     */
	@Override
	public void onHttpRequestFailed(PHError e) {
		PHStringUtil.log("PHURLOpener failed with error: " + e);
		fail();
	}


}
