package v2.com.playhaven.requests.base;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.util.DisplayMetrics;
import v2.com.playhaven.configuration.PHConfiguration;
import v2.com.playhaven.utils.PHConnectionUtils;
import v2.com.playhaven.requests.crashreport.PHCrashReport;
import v2.com.playhaven.model.PHError;
import v2.com.playhaven.listeners.PHHttpRequestListener;
import v2.com.playhaven.utils.PHStringUtil;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Locale;

/**
 * @class PHAPIRequest Nicer wrapper for {@link PHAsyncRequest} and base class
 *        for making API calls. We make extensive use of the "templating"
 *        pattern in this class since it is designed primarily to be overridden.
 *        Subclasses should override the getters to customize the behavior of
 *        this class instead of calling a million setters. The main purpose of this class
 *        is to provide one additional level of abstraction to the base HTTP request.
 *
 *        This class does not have its own listener, so each subclass must have its own for code clarity.
 */
public class PHAPIRequest implements PHHttpRequestListener {

    /** The underlying HTTP request */
	private PHAsyncRequest conn;

	private HashMap<String, String> signedParams;

	private Hashtable<String, String> additionalParams;

    private PHConfiguration config;

	private String urlPath;

	private int requestTag;

    /** We save the last JSON response (mostly for unit testing)*/
    private JSONObject lastResponse;

    /** We save the last {@link v2.com.playhaven.model.PHError} (mostly for unit testing)*/
    private PHError lastError;

    /** A simple flag indicating whether or not we should complain about
     * un-overridden methods. By default, we always do. If unit testing,
     * this flag is set to false.
     */
    public boolean shouldComplainAboutNonOverridden = true;

	private final String SESSION_PREFERENCES = "com_playhaven_sdk_session";

	/** the "subkey" index within each precache entry (we only need 1) */
	public static final Integer PRECACHE_FILE_KEY_INDEX = 0;

	public static final String API_CACHE_SUBDIR = "apicache";

	public static final Integer APP_CACHE_VERSION = 100;

	// protected so subclasses can access it. You shouldn't.
	protected String fullUrl;

    public PHAPIRequest() {
        config = new PHConfiguration();
    }

    private void checkTokenAndSecret(String token, String secret) {
        if (token == null || secret == null
                || token.length() == 0
                || secret.length() == 0)
            throw new IllegalArgumentException("You must provide a token and secret from the Playhaven dashboard");
    }

	///////////////////////////////////////////////

	public PHAsyncRequest getConnection() {
		return conn;
	}

	///////////////////////////////////////////////

	public void setRequestTag(int requestTag) {
		this.requestTag = requestTag;
	}

	public int getRequestTag() {
		return this.requestTag;
	}

	///////////////////////////////////////////////////////
	///////////// Generating Signed Parameters //////////

	/**
	 * Creates a base "authed" URL + any additional parameters (usually from
	 * subclass).
	 * 
	 * @throws NoSuchAlgorithmException
	 * @throws UnsupportedEncodingException
	 * @return HashMap Mapping from query parameters to values
	 * */
	public HashMap<String, String> getSignedParams(Context context) throws UnsupportedEncodingException, NoSuchAlgorithmException {
		if (signedParams == null) {
			String device, 
					nonce, 
					sigHash, 
					sig, 
					appId, 
					appVersion, 
					hardware, 
					os, 
					idiom, 
					sdk_version, 
					width, 
					height, 
					sdk_platform, 
					orientation, 
					screen_density, 
					connection;

            // get the device id
            device              = Settings.System.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);

            if (device == null)
                device = "null";
			
			idiom 				= String.valueOf(context.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK);
			
			orientation 		= "0"; // TODO: use actual orientation?

			// make sure we generate the device id before doing the sighash!
			// you like that formatting do you?

			nonce = PHStringUtil.base64Digest(PHStringUtil.generateUUID());

			sig = String.format("%s:%s:%s:%s", config.getToken(context),
					                           (device != null ? device : ""), // in the future we'll add session
					                           (nonce != null ? nonce : ""),
                                               config.getSecret(context));

			sigHash 			= PHStringUtil.hexDigest(sig);

            try {
                PackageInfo pinfo   = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);

                appId 				= pinfo.packageName;

                appVersion 			= pinfo.versionName;

            } catch (PackageManager.NameNotFoundException e) {
                // If it doesn't work, leave this field off
                appId = "";

                appVersion = "";
            }

			hardware 			= Build.MODEL;
			
			os 					= String.format("%s %s", Build.VERSION.RELEASE, Build.VERSION.SDK_INT);

			sdk_version 		= config.getCleanSDKVersion();
			
			sdk_platform 		= "android";

            DisplayMetrics dm 	= context.getResources().getDisplayMetrics();

			width 				= String.valueOf(dm.widthPixels);
			
			height 				= String.valueOf(dm.heightPixels);
			
			screen_density 		= String.valueOf(dm.densityDpi);

            PHConfiguration.ConnectionType type  = PHConnectionUtils.getConnectionType(context);

			connection 			= ((type == PHConfiguration.ConnectionType.NO_PERMISSION) ? null : String.valueOf(type.ordinal()));

			// decide if we add to existing params.
			Hashtable<String, String> additionalParams = getAdditionalParams(context); // only
																				       // call
																				       // *once*
																				       // since
																				       // might
																				       // have
																				       // side
																				       // effects

			// make a copy of the additional parameters so that we don't
			// modify the original
			HashMap<String, String> add_params = (additionalParams != null ? new HashMap<String, String>(
					additionalParams) : new HashMap<String, String>());

			signedParams = new HashMap<String, String>();

			signedParams.put("device", 			device);

			signedParams.put("token", 			config.getToken(context));
			
			signedParams.put("signature", 		sigHash);

			signedParams.put("nonce", 			nonce);
			
			signedParams.put("app", 			appId);

			signedParams.put("app_version", 	appVersion);
			
			signedParams.put("hardware", 		hardware);

			signedParams.put("os", 				os);
			
			signedParams.put("idiom", 			idiom);

			signedParams.put("width", 			width);
			
			signedParams.put("height", 			height);

			signedParams.put("sdk_version", 	sdk_version);
			
			signedParams.put("sdk_platform", 	sdk_platform);

			signedParams.put("orientation", 	orientation);
			
			signedParams.put("dpi", 			screen_density);

			signedParams.put("languages", 		Locale.getDefault().getLanguage());

			if (connection != null)
				signedParams.put("connection", 	connection);

			add_params.putAll(signedParams);
			signedParams = add_params;
		}

		return signedParams;
	}

	/** Sets the additional parameters (mostly for testing) */
	public void setAdditionalParameters(Hashtable<String, String> params) {
		this.additionalParams = params;
	}

	/**
	 * Gets the additional parameters. Declared as an accessor so it can be
	 * overrode
     * @param context the context we'll use for accessing settings.
	 */
	public Hashtable<String, String> getAdditionalParams(Context context) {
		return additionalParams;
	}

	/**
	 * Produces a query string with the signed parameters attached.
	 * 
	 * @throws NoSuchAlgorithmException
	 * @throws UnsupportedEncodingException
	 */
	public String signedParamsStr(Context context) throws UnsupportedEncodingException,
			NoSuchAlgorithmException {

		return PHStringUtil.createQuery(getSignedParams(context));
	}

    /** Helper method to ensure that we have a valid token/secret
     * @param context a valid context used for accessing the settings.
     */
    private boolean hasValidTokenAndSecret(Context context) {
        String token    = config.getToken(context);
        String secret   = config.getSecret(context);

        if (token != null && token.length() > 0)
            if (secret != null && secret.length() > 0)
                return true;

        return false;
    }

    /** Actually sends off the API request. You must
     * provide an appropriate context so that we can gather
     * device information.
     * @param context A valid context.
     */
	public void send(Context context) {

        // if we don't have a valid token and secret, fail silently
        // to fix "rattlesnake" bug.
        if ( ! hasValidTokenAndSecret(context)) {
            PHStringUtil.log("Either the token or secret has not been properly set");
            return;
        }

        // if we have a valid token and secret, full steam ahead!
		conn = new PHAsyncRequest(this);

		if (config.getStagingUsername(context) != null && config.getStagingPassword(context) != null) {
			conn.setUsername(config.getStagingUsername(context));
			conn.setPassword(config.getStagingPassword(context));
		}

		conn.request_type = getRequestType();

        // make certain the token and secret are valid
        checkTokenAndSecret(config.getToken(context), config.getSecret(context));

		send(context, conn);
	}

	/** Actually kicks off the request. Can also be used for unit testing.
	 * 
	 *  @param client The HTTP client we should use for making the request
	 */
	private void send(Context context, PHAsyncRequest client) {
		try { // all encompassing crash report before we spawn new thread
			this.conn = client;

			// explicitly set the post params if a post request
			if (conn.request_type == PHAsyncRequest.RequestType.Post)
				conn.addPostParams(getPostParams());

			PHStringUtil.log("Sending PHAPIRequest of type: "
					+ getRequestType().toString());
			PHStringUtil.log("PHAPIRequest URL: " + getURL(context));

			conn.execute(Uri.parse(getURL(context)));

		} catch (Exception e) {
			PHCrashReport.reportCrash(e, "PHAPIRequest - send()",
                    PHCrashReport.Urgency.critical);
		}
	}

    ///////////////////////////////////////////////////
    ///////////// Unit Testing ////////////////////////

    public JSONObject getLastResponse() {
        return lastResponse;
    }

    public PHError getLastError() {
        return lastError;
    }

    public void resetLastError() {
        lastError = null;
    }

    public void resetLastResponse() {
        lastResponse = null;
    }

	//////////////////////////////////////////////////
	/////////// Request Detail Differences /////////////
	
	/**
	 * Gets the request type. This should be overriden by subclasses to control
	 * the type of request which gets sent.
	 */
	public PHAsyncRequest.RequestType getRequestType() {
		return PHAsyncRequest.RequestType.Get;
	}

	/**
	 * Gets the post parameters if the request type is POST. Subclasses should
	 * override to provide parameters.
	 */
	public Hashtable<String, String> getPostParams() {
		return null; // just empty
	}

	/** Should only be overridden and not called directly from external class. */
	protected void finish() {
		conn.cancel(true);

	}

    /** Stops this api request */
	public void cancel() {
		PHStringUtil.log(this.toString() + " canceled!");
		finish();
	}

	/**
	 * Gets url along with parameters attached (plus auth parameters).
	 * Subclasses should override for additional customization.
	 * 
	 * @throws NoSuchAlgorithmException
	 * @throws UnsupportedEncodingException
	 */
	public String getURL(Context context) throws UnsupportedEncodingException,
			NoSuchAlgorithmException {
		if (fullUrl == null)
			fullUrl = String.format("%s?%s", this.baseURL(context), signedParamsStr(context));

		return fullUrl;
	}

	/** Appends the endpoint path to the base api url */
	public String createAPIURL(Context context, String slug) {
		return config.getAPIUrl(context) + slug;
	}

	/**
	 * The base url (or slug) with no parameters attached. Subclasses should
	 * override this method to provide the specific api request slug.
	 */
	public String baseURL(Context context) {
		return urlPath;
	}

	/** Sets the base url. */
	public void setBaseURL(String url) {
		this.urlPath = url;
	}

	///////////////////////////////////////////////////////
	//////////// Response Handling ///////////////////////

	/** 
	 * Override point for subclasses (template pattern) to handle requests.
	 * @param res The JSON 'response' field from the server
	 */
	public void handleRequestSuccess(JSONObject res) {
        if (shouldComplainAboutNonOverridden)
            throw new RuntimeException("Request succeeded and subclass has not override handleRequestSuccess");
	}
	
	/**
	 * Override point for subclasses (template pattern) to handle request failures.
	 * The default implementation merely calls our own listener
	 * @param error The error explaining why this result failed
	 */
	public void handleRequestFailure(PHError error) {
        if (shouldComplainAboutNonOverridden)
		    throw new RuntimeException("Request failed and subclass has not override handleRequestFailure");
	}

	///////////////////////////////////////////////////////////////
	/////////////// PHAsyncRequest PHHttpRequestListener Methods ///////////////


    /**
     * Handles the success from the underlying HTTP request we've sent.
     */
	@Override
	public void onHttpRequestSucceeded(ByteBuffer response, int responseCode) {
		PHStringUtil.log("Received response code: " + responseCode);

		// if the request was fundamentally flawed, we need to inform our listener
		if (responseCode != 200) {
			
			handleRequestFailure(new PHError("Request failed with code: " + responseCode));
			
			return;
		}

		// if we see an empty response, it might be an open request.
		// it's none of our business so we just pass it along.
        // At this level, PHAPIRequest should not do significant validation
        // of API responses.
		if (response == null || response.array() == null) {

            // since it's empty, don't bother parsing, simply pass
            // in an empty object.
			JSONObject json = new JSONObject();

			// continue processing the request response
			processRequestResponse(json);
		}


        // if it isn't empty, try parsing it.
		try {
			// convert the raw bytes into a workable string
			String res_str = new String(response.array(), "UTF8");
			
			PHStringUtil.log("Unparsed JSON: " + res_str);

			// try to parse the JSON and see if we get stuck
			JSONObject json = new JSONObject(res_str);

			// process the actual JSON data
			processRequestResponse(json);

		} catch (UnsupportedEncodingException e) {
			
			// problems parsing the JSON encoding
			handleRequestFailure(new PHError("Unsupported encoding when parsing JSON"));
			
		} catch (JSONException e) {
			
			// we had a JSON error
			handleRequestFailure(new PHError("Could not parse JSON because: " + e.getMessage()));

		} catch (Exception e) {
			e.printStackTrace();
			// we had some unknown error
			handleRequestFailure(new PHError("Unknown error during API request: " + e.getMessage()));
			
		}

	}

	/**
	 * Processes the parsed JSON response. Does extensive checking
	 * to ensure logically consistent and notifies the listener.
	 */
	public void processRequestResponse(JSONObject response) {
		
		String errmsg 		= response.optString("error");
		
		JSONObject errobj 	= response.optJSONObject("errobj");
		

		/**
		 * Check to see if the response contains an explicit error
		 */
		if ((!JSONObject.NULL.equals(errobj)  && errobj.length() > 0)
				|| (!response.isNull("error") && errmsg.length() > 0)) {

			
			// we call the often overridden method to allow subclasses to handle the error
            // By default we simply notify the listener of our error.
            lastError = new PHError("Server sent error message: " + errmsg);
			handleRequestFailure(lastError);
			return;
		}

		lastResponse = response.optJSONObject("response");


		// if all the above conditions pass, we actually handle the request
		// Subclasses often override this method as a form of "templating"
		// The default implementation simply notifies the listener
		handleRequestSuccess(lastResponse);
	}
	
	
	/**
	 * Handle a direct failure from the underlying HTTP request.
	 */
	@Override
	public void onHttpRequestFailed(PHError e) {
        lastError = e;
		// the underlying http request informed us of failure
		handleRequestFailure(e);
	}

}
