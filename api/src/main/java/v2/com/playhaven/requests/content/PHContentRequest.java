package v2.com.playhaven.requests.content;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Random;

import org.json.JSONObject;

import v2.com.playhaven.cache.PHCache;
import v2.com.playhaven.configuration.PHConfiguration;
import v2.com.playhaven.interstitial.PHContentEnums;
import v2.com.playhaven.interstitial.PHInterstitialActivity;
import v2.com.playhaven.interstitial.requestbridge.BridgeManager;
import v2.com.playhaven.interstitial.requestbridge.base.ContentRequester;
import v2.com.playhaven.interstitial.requestbridge.bridges.ContentRequestToInterstitialBridge;
import v2.com.playhaven.listeners.PHContentRequestListener;
import v2.com.playhaven.listeners.PHPurchaseListener;
import v2.com.playhaven.listeners.PHRewardListener;
import v2.com.playhaven.model.PHContent;
import v2.com.playhaven.model.PHError;
import v2.com.playhaven.requests.base.PHAPIRequest;
import v2.com.playhaven.requests.crashreport.PHCrashReport;
import v2.com.playhaven.requests.open.PHSession;
import v2.com.playhaven.views.interstitial.PHCloseButton;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.util.Log;

import com.playhaven.src.utils.PHStringUtil;

/** 
 * Represents a request for an actual advertisement or "interstitial". We handle the tearDownBridge button and actual logistics such as rewards, etc.
 * Each instance makes a request to the server for the interstitial template "data" and then "pushes" (displays) a PHInterstitialActivity. The PHInterstitialActivity
 * is an Activity which in turn can display other PHInterstitialActivity if the interstitial template makes a subrequest, etc.
 * 
 * @author Sam Stewart
 *  
 */
public class PHContentRequest extends PHAPIRequest implements ContentRequester {


    /** We need a long-lasting reference to the application context
     * so that our handlers can perform certain actions.
     */
	private WeakReference<Context> applicationContext;

    /** We need a reference to an activity context to start the interstitial
     * once we receive a response.
     */
    private WeakReference<Activity> activityContext;

    /**
     * The bridge between this request and the eventual
     * {@link PHInterstitialActivity}.
     */
    private ContentRequestToInterstitialBridge bridge;
	
	private static int PREVIOUS_AD_RANGE = 2000; // the time range in which we should look for ad views shown
	
	public String placement;
	
	private PHContent content;
	
	public String contentTag; 

    private boolean shouldPrecache;

	private Bitmap close_button_up;

	private Bitmap close_button_down;

    private PHConfiguration config;


	/** List of states the interstitial can be in. */
	public enum PHRequestState {
		Initialized,
		Preloading,
		Preloaded,
		DisplayingContent,
		Done
	};
	
	/** The different types of dismiss events. */
	public enum PHDismissType {
		AdSelfDismiss, // interstitial unit sent a dismiss request from javascript
		CloseButton, // called from tearDownBridge button
		ApplicationBackgrounded, //application currentStatet
	};

    /** The current state of the interstitial. This allows us to separate
     * the preloading from displaying the interstitial.
     */
	private PHRequestState currentContentState;

    /** The target or final state of the interstitial. This is our
     * goal such as preloading, displaying immediately, etc.
     */
	private PHRequestState targetState;
	
	private PHContentRequestListener content_listener 	= null;

    ////////////////////////////////////////////////
    /////////////// Constructors //////////////////
    public PHContentRequest(String placement) {
        super();

        this.placement = placement;

        setCurrentContentState(PHRequestState.Initialized);

        config = new PHConfiguration();

        // we need to setup the bridge so that
        // we can accept listeners
        createBridge();
    }

    public PHContentRequest(PHContentRequestListener listener, String placement) {
        this(placement);
        this.content_listener = listener;
    }

    /** This getter is mostly used by unit tests */
    public PHContentRequestListener getContentListener() {
        return content_listener;
    }

	public void setOnContentListener(PHContentRequestListener content_listener) {
		this.content_listener = content_listener;
		
		if (bridge != null)
			bridge.attachContentListener(content_listener);
	}
	
	private PHRewardListener reward_listener 		= null;

    /** This getter is mostly used by unit tests */
    public PHRewardListener getRewardListener() {
        return reward_listener;
    }

	public void setOnRewardListener(PHRewardListener reward_listener) {
		this.reward_listener = reward_listener;
		
		if (bridge != null)
			bridge.attachRewardListener(reward_listener);
	}

    public void setOnPurchaseListener(PHPurchaseListener purchase_listener) {
        this.purchase_listener = purchase_listener;

        if (bridge != null)
            bridge.attachPurchaseListener(purchase_listener);
    }

	private PHPurchaseListener purchase_listener 	= null;

    /** This getter is mostly used by unit tests */
    public PHPurchaseListener getPurchaseListener() {
        return purchase_listener;
    }
	
	///////////////////////////////////////////////////
	///////////// Tracking code for showing ads ///////
	
	private static Long mLastDismissed;
	
	/** 
	 * Checks to see if we have dismissed a interstitial view within range number of milliseconds.
	 */
	public static boolean didDismissContentWithin(Long range) {
		return (mLastDismissed == null) ? false : (mLastDismissed > System.currentTimeMillis() - range);
	}
	
	/** 
	 * Checks to see if we have dismissed a interstitial view within PREVIOUS_AD_RANGE of milliseconds.
	 * Added in 1.12.3. 
	 */
	public static boolean didJustShowAd() {
		return (mLastDismissed == null) ? false : (mLastDismissed > System.currentTimeMillis() - PREVIOUS_AD_RANGE);
	}
	
	/** Utility method for PHInterstitialActivity to log a dismiss*/
	public static void updateLastDismissedAdTime() {
		mLastDismissed = System.currentTimeMillis();
	}
	
	public void setCloseButton(Bitmap image, PHCloseButton.CloseButtonState state) {

        if (state == PHCloseButton.CloseButtonState.Up) {
			close_button_up = image;
		} else if (state == PHCloseButton.CloseButtonState.Down) {
			close_button_down = image;
		}
	}

	@Override
	public String baseURL(Context context) {
		return super.createAPIURL(context, "/v3/publisher/content/");
	}

	public void setCurrentContentState(PHRequestState state) {
	    if (state      == null) return;
		if (this.currentContentState == null) this.currentContentState = state; //guard against null edge case..
		
		// only set currentContentState forward in time! (if set above, will just ignore)
		if (state.ordinal() > this.currentContentState.ordinal()) {
			this.currentContentState = state;
		}
	}
	
	public PHRequestState getCurrentState() {
		return currentContentState;
	}
	
	public PHRequestState getTargetState() {
		return targetState;
	}

    /**
     * This setter is mostly used by unit tests
     */
    public void setTargetState(PHRequestState state) {
        this.targetState = state;
    }

    public void setCurrentState(PHRequestState state) {
        currentContentState = state;
    }


    @Override
    public String getTag() {
        return contentTag;
    }

    public PHContent getContent() {
		return content;
	}

    @Override
    public Context getContext() {
        return applicationContext.get();
    }

    @Override
    public void onTagChanged(String new_tag) {
        this.contentTag = new_tag;
    }

    /////////////////////////////////////////////////
	/////////////////////////////////////////////////

    /**
     * Allows the publisher to separate
     * the API request from displaying the interstitial
     * @param activity The context for making the actual API request. It must
     *                 be an activity because we will use it to start a new activity later.
     */
	public void preload(Activity activity) {

        if (activity == null) return;

        // we cache this value since we don't have a context later
        shouldPrecache = config.getShouldPrecache(activity);

        // we use the Application Context to ensure decent stability (exists throughout application's life)
        this.applicationContext = new WeakReference<Context>(activity.getApplicationContext());

        this.activityContext    = new WeakReference<Activity>(activity);

	    if (shouldPrecache) {
    	    synchronized (PHCache.class) {
                PHCache.installCache(activity);
    	    }
	    }

	    targetState = PHRequestState.Preloaded;
	    continueToNextContentState(activity);
	}

    /**
     * Actually sends the interstitial request for the interstitial
     */
	private void loadContent(Context context) {
		setCurrentContentState(PHRequestState.Preloading);
		super.send(context); // now actually send the request
		
		// We kick off the background network request and
		// then notify the listener that we hvae sent the interstitial request.
		if(content_listener != null)
			content_listener.onSentContentRequest(this);

	}

    /**
     * Utility method for launching a new {@link PHInterstitialActivity}
    */
    public static void displayInterstitialActivity(PHContent content, Activity context, HashMap<String, Bitmap> customCloseImages, String tag) {
    	
        if (context != null) {
            // we have to use the shim class to work with older SDKs
            Intent contentViewIntent = new Intent(context, com.playhaven.src.publishersdk.content.PHContentView.class);

            // attach the interstitial object
            contentViewIntent.putExtra(PHContentEnums.IntentArgument.Content.getKey(), content);

            // attach custom closeImages
            if (customCloseImages != null && customCloseImages.size() > 0)
                contentViewIntent.putExtra(PHContentEnums.IntentArgument.CustomCloseBtn.getKey(), customCloseImages);

            // add the unique identifier for communicating with the main
            // interstitial request.
            contentViewIntent.putExtra(PHContentEnums.IntentArgument.Tag.getKey(), tag);

            PHStringUtil.log("Added all relevant arguments now starting activity through context: " + context.getClass().getSimpleName());

            context.startActivity(contentViewIntent);

        }
    }

    /** 
     * Creates the content requester to displayer bridge.
     * It also generates the content tag to uniquely identify this bridge. 
    */
    private void createBridge() {
        contentTag 	= generateContentActivityTag();

        // establish a bridge between the listeners and the interstitial activity
        bridge 		= new ContentRequestToInterstitialBridge(contentTag);

        // attach the appropriate listeners
        bridge.attachContentListener    (   this.content_listener   );
        bridge.attachRewardListener     (   this.reward_listener    );
        bridge.attachPurchaseListener   (   this.purchase_listener  );
    }
    
    /**
     * Starts a new instance of {@link PHInterstitialActivity} and
     * attaches this content requester as one side of the bridge with
     * the new activity.
     * @param activity the context we'll use for launching the new interstitial activity.
    */
	private void showContentActivityIfReady(Activity activity) {

		if (targetState == PHRequestState.DisplayingContent || targetState == PHRequestState.Done) {

            PHStringUtil.log("Attempting to show content interstitial");

            // tell the listener we're about to display the interstitial
			if (content_listener != null)
				content_listener.onWillDisplayContent(this, content);
			
			// the currentContentState might have been 'Done'
			setCurrentContentState(PHRequestState.DisplayingContent);
			
			// package up the custom buttons before displaying the new PHInterstitialActivity
			HashMap<String, Bitmap> customClose = new HashMap<String, Bitmap>();

            // use the custom close button state images if they have been
            // specified.
			if (close_button_down != null && close_button_up != null) {

				customClose.put(PHCloseButton.CloseButtonState.Up.name(),    close_button_up        );
				customClose.put(PHCloseButton.CloseButtonState.Down.name(),  close_button_down      );
			}

			// create a new bridge
	        BridgeManager.openBridge		(contentTag,  bridge);

	        // now actually create our half of the bridge with the content displayer (interstitial)
	        // we are about to launch
	        BridgeManager.attachRequester	(contentTag,  this);
	        
            // actually kick off the other activity
			displayInterstitialActivity(content, activity, customClose, contentTag);

		}
	}
	
	/**
	 * Generates a unique string for a interstitial view so that we can address
	 * multiple interstitial views. We generate a random tag to ensure that
     * we do not have overlaps. This method is non-deterministic
     * so be sure to save the result because it never returns exactly the same result.
	 * @return A unique tag representing the interstitial view.
	 */
	private String generateContentActivityTag() {
        Random random = new Random(System.currentTimeMillis() / 1000);

		return "PHInterstitialActivity: " + this.hashCode() + " ~ " + random.nextInt();
	}

    /** Manages the transitions between the different states.
     * You can pass in null for the context if the interstitial
     * has already been preloaded because we no longer need device info.
     * @param activity The context used for gathering device info.
     */
	private void continueToNextContentState(Activity activity) {
		switch (currentContentState) {
			case Initialized:
				loadContent(activity);
				break;
			case Preloaded:
				showContentActivityIfReady(activity);
				break;
			default:
				break;
		}
	}

    /**
     * Actually sends the request.
     * @param context Thus *must* be an Activity or we can't launch the final
     *                interstitial activity. We can't require the activity type
     *                explicitly because we are overriding but it is important
     *                that you pass in an Activity.
     */
	@Override
	public void send(Context context) {
        Activity activity = (Activity)context;

        // we need to cache this because we don't have a context later
        shouldPrecache = config.getShouldPrecache(activity);

        this.applicationContext = new WeakReference<Context>(activity.getApplicationContext());

        this.activityContext    = new WeakReference<Activity>(activity);

		try {
			targetState = PHRequestState.DisplayingContent;
		
			continueToNextContentState(activity);
		
		} catch(Exception e) { // swallow all exceptions
			PHCrashReport.reportCrash(e, "PHContentRequest - send", PHCrashReport.Urgency.critical);
		}
	}

	@Override
	public void finish() {
		setCurrentContentState(PHRequestState.Done);
		
		super.finish();
	}
	
	
	/////////////////////////////////////////////////
	///////// PHAPIRequest Override Methods /////////
	@Override
	public Hashtable<String, String> getAdditionalParams(Context context) {
		Hashtable<String, String> table = new Hashtable<String, String>();
		
		table.put("placement_id", (placement != null ? placement : ""));
		
		table.put("preload", (targetState == PHRequestState.Preloaded ? "1" : "0"));

		PHSession session = PHSession.getInstance(context);
		
		table.put("stime", String.valueOf(session.getSessionTime()));
	
		return table;
	}
	
	@Override
	public void handleRequestFailure(PHError error) {
		if (content_listener != null)
			content_listener.onFailedToDisplayContent(this, new PHError("Could not get interstitial because: " + error.getMessage()));
	}
	
	@Override
	public void handleRequestSuccess(JSONObject response) {
		// response might be empty. This isn't an error, the server simply might not have something to display.
	    if (JSONObject.NULL.equals(response) || response.length() == 0 || response.equals("undefined")) {
	    	
	    	// notify the listener we have no interstitial available
	    	if (content_listener != null)
	    		content_listener.onNoContent(this);
	    	
	    	return;
	    }
	    
	    content = new PHContent(response);

        // if the interstitial has no template URL then we are done.
		if (content.url == null) {
            setCurrentContentState(PHRequestState.Done);
            return;
        }

        // we've received the interstitial and thus have preloaded
        setCurrentContentState(PHRequestState.Preloaded);

        // if we should be precaching the interstitial, download the images in the interstitial
        // since we will have downloaded the interstitial template in the open request.
        // we also need to redirect the image paths to the local copies.

        /**
        // As of build 1.12.2 we have disabled image caching because of the same origin
        // restriction for loading cached images from javascript into the webview.

		if (shouldPrecache) {

              if (PHImageCache.hasBeenInstalled()) {
                   // we cache the interstitial and redirect the image paths
                   content = PHImageCache.getSharedCache().cacheImages(content);
              }

		}
        */
		
	    // notify the listeners that we actually received the interstitial.
		// we wait for them to respond before continuing to the next stage
	    if (content_listener != null)
	    	content_listener.onReceivedContent(this, content);

        // use our reference to the activity context
        if (activityContext != null && activityContext.get() != null)
		    continueToNextContentState(activityContext.get());
	}
}
