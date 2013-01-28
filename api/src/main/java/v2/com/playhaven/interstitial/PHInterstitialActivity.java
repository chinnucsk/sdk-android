package v2.com.playhaven.interstitial;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.view.MotionEvent;
import android.view.Window;
import android.view.WindowManager;
import android.widget.RelativeLayout;
import v2.com.playhaven.cache.PHCache;
import v2.com.playhaven.configuration.PHConfiguration;
import v2.com.playhaven.interstitial.PHContentEnums.IntentArgument;
import v2.com.playhaven.interstitial.jsbridge.ManipulatableContentDisplayer;
import v2.com.playhaven.interstitial.jsbridge.PHJSBridge;
import v2.com.playhaven.interstitial.jsbridge.handlers.*;
import v2.com.playhaven.interstitial.requestbridge.BridgeManager;
import v2.com.playhaven.interstitial.requestbridge.base.ContentDisplayer;
import v2.com.playhaven.interstitial.requestbridge.bridges.ContentRequestToInterstitialBridge;
import v2.com.playhaven.model.PHContent;
import v2.com.playhaven.model.PHPurchase;
import v2.com.playhaven.requests.content.PHContentRequest;
import v2.com.playhaven.requests.content.PHContentRequest.PHDismissType;
import v2.com.playhaven.requests.content.PHSubContentRequest;
import v2.com.playhaven.requests.crashreport.PHCrashReport;
import v2.com.playhaven.utils.PHStringUtil;
import v2.com.playhaven.utils.PHURLOpener;
import v2.com.playhaven.views.interstitial.PHCloseButton;
import v2.com.playhaven.views.interstitial.PHContentView;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Random;

/**
 * A separate activity for actually displaying the interstitial after the
 * {@link PHContentRequest} has finished loading. When starting this activity,
 * you should attach a "interstitial" via "putExtra" using the CONTENT_START_KEY. You
 * should alos attach
 * 
 * @author samstewart
 * 
 */
public class PHInterstitialActivity extends Activity implements ContentDisplayer, ManipulatableContentDisplayer, PHContentView.Listener  {

    /**
     * The tag that uniquely identifies this content view
     * with the originating content request.
     */
    private String tag;

    /**
     * The suffix we attach to a new subrequest tag
     * when we launch a new sub interstitial activity
     */
    private final static String SUB_INTERSTITIAL_SUFFIX = "SubInterstitial";

    /** the content this interstitial is displaying */
	public PHContent content;

    /** Flag that determines whether or not this view can be canceled
     * by the user hitting the back button.
     */
	private boolean isBackBtnCancelable;

    /** Flag that determines whether or not this view can be canceled
     * by the user touching outside of the dialog area.
     */
	private boolean isTouchCancelable;

    /** The actual view that contains both the ad "chrome" such as the close button
     * and the webview itself.
     */
    private PHContentView contentView;

    /** The bridge between the content templates in the webview and our
     * native code.
     */
	private PHJSBridge jsBridge;

    /** The configuration which is static because the UI automation framework uses it */
    private static PHConfiguration config;

	///////////////////////////////////////////////////////////////
	//////////////////////// Broadcast Constants //////////////////
	
    private HashMap<String, Bitmap> customCloseStates = new HashMap<String, Bitmap>();


    /** Initiates the cache */
	private void initCache() {

        if ( ! config.getShouldPrecache(this)) return;

        synchronized (PHCache.class) {

            PHCache.installCache(this);

        }

    }


	////////////////////////////////////////////////////////
	//////////////// Activity Callbacks ////////////////////

	@Override
	public void onBackPressed() {
		if (this.getIsBackBtnCancelable()) {
			PHStringUtil.log("The interstitial unit was dismissed by the user using back button");
			

			notifyContentRequestOfClose(PHDismissType.CloseButton);

			super.onBackPressed();
		}

		// if we aren't cancelable via the back button, simply consume and ignore..
	}

    /** Simple helper method for notifying the {@link PHContentRequest}
     * that we are closing. More importantly, we are notifying
     * the {@link PHContentRequest} of the <em>type</em> of dismissal
     * which is what publishers really want.
     * @param type the type of dismissal
     */
    private void notifyContentRequestOfClose(PHDismissType type) {

        Bundle args = new Bundle();

        // get the key for the close type parameter
        String close_type_key = ContentRequestToInterstitialBridge.InterstitialEventArgument.CloseType.getKey();

        // add the close type to the message
        args.putString(close_type_key, type.toString());

        // get the event name
        String event = ContentRequestToInterstitialBridge.InterstitialEvent.Dismissed.toString();

        // finally send the event
        BridgeManager.sendMessageFromDisplayer(tag, event, args, this);
    }

    /** Simple helper method for notifying the {@link PHContentRequest}
     *  when we encounter and error.
     */
    private void notifyContentRequestOfFailure(PHContentEnums.Error error) {

        Bundle args = new Bundle();

        // get the key for the close type parameter
        String error_key = ContentRequestToInterstitialBridge.InterstitialEventArgument.Error.getKey();

        // add the no frame error
        args.putString(error_key, error.toString());

        // get the event name
        String event = ContentRequestToInterstitialBridge.InterstitialEvent.Failed.toString();

        // finally send the event
        BridgeManager.sendMessageFromDisplayer(tag, event, args, this);
    }

    /** We force the scope to public so that unit tests can access this method.*/
	@Override
	public void onPause() {
	    super.onPause();

        // if we are finishing because we have dismissed ourselves
        // we should not call dismiss again since it has already been
        // called. onPause should only handle the case
        // where an external app obscures the ad.
        if (this.isFinishing()) return;

	    PHStringUtil.log("The interstitial activity was backgrounded and dismissed itself");
	    
	    // since we have been backgrounded, we dismiss ourselves entirely.
	    // although painful, suicide is often necessary


        // notify the content requester that
        // we are closing via being backgrounded
        notifyContentRequestOfClose(PHDismissType.ApplicationBackgrounded);

        // dismiss ourselves politely
        dismiss();
	}
	
	/**
	 * Creates the view layouts for this activity.
     * We force the scope to public so that unit tests can access this method.
	 */
	@Override
	public void onStart() {
		super.onStart();
		
		// setup the cache
		initCache();

        // setup the content view
		try {

            BitmapDrawable active   = null;
            BitmapDrawable inactive = null;

            String active_key   = PHCloseButton.CloseButtonState.Up.name();
            String inactive_key = PHCloseButton.CloseButtonState.Up.name();

            if (customCloseStates.get(active_key) != null)
                active   = new BitmapDrawable(getResources(), customCloseStates.get(active_key));

            if (customCloseStates.get(inactive_key) != null)
                inactive = new BitmapDrawable(getResources(), customCloseStates.get(inactive_key));


            contentView = new PHContentView(this,
                                            this,
                                            content,
                                            this,
                                            jsBridge,
                                            active,
                                            inactive);

            RelativeLayout.LayoutParams fullscreenLayout = new RelativeLayout.LayoutParams(
                                                                RelativeLayout.LayoutParams.MATCH_PARENT,
                                                                RelativeLayout.LayoutParams.MATCH_PARENT
                                                            );
            // the content view is the root view
            setContentView(contentView, fullscreenLayout);

            fitInterstitialWindowToContent();

		} catch(Exception e) { //swallow all exceptions
			PHCrashReport.reportCrash(e, "PHInterstitialActivity - onStart()", PHCrashReport.Urgency.critical);
		}
	}

    /** We force the scope to public so that unit tests can access this method.*/
    @Override
    public void onStop() {
        super.onStop();
    }

    /**
     * Called from the {@link ContentRequestToInterstitialBridge} when a {@link PHPurchase}
     * created by this Activity has resolved its final purchase status.
     */

    public void onPurchaseResolved(PHPurchase resolvedPurchase) {
        try {
            JSONObject response = new JSONObject();

            response.put                    (   "resolution", resolvedPurchase.resolution.getType() );

            // notify the content templates that the purchase has resolved.
            jsBridge.sendMessageToWebview(resolvedPurchase.callback, response, null);
        } catch (JSONException e) {
            PHCrashReport.reportCrash(e, "PHInterstitialActivity - BroadcastReceiver - onReceive", PHCrashReport.Urgency.critical);
        }
    }

    /** We force the scope to public so that unit tests can access this method.*/
	@Override
	public void onDestroy() {
		super.onDestroy();

        // close the bridge between the content request
        // and ourselves
        BridgeManager.closeBridge(tag);

        contentView.cleanup();

	}

    /** Sets up the various handlers to connect with the javascript
     * in the content templates.
     */
	private void setupWebviewJSBridge() {
        jsBridge = new PHJSBridge(this);

		jsBridge.addRoute("ph://dismiss",        new DismissHandler());
		jsBridge.addRoute("ph://launch",         new LaunchHandler());
		jsBridge.addRoute("ph://loadContext",    new LoadContextHandler());
		jsBridge.addRoute("ph://reward",         new RewardHandler());
		jsBridge.addRoute("ph://purchase",       new PurchaseHandler());
		jsBridge.addRoute("ph://subcontent",     new SubrequestHandler());
		jsBridge.addRoute("ph://closeButton",    new CloseButtonHandler());
	}
	

	/**
	 * 
	 * Note; If the Activity doesn't have a frame we must dismiss
	 * from here or the system won't honor it.
	 */
	@Override
	public void onAttachedToWindow() {
		try {
            super.onAttachedToWindow();

            // we can only dismiss once the window is visible..
            if (!contentHasFrame()) {

                // notify the content requester tha we failed
                notifyContentRequestOfFailure(PHContentEnums.Error.NoBoundingBox);
                return;
            }

            // make background transparent
            // in preparation for webview.
            getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
		} catch (Exception e) {
			PHCrashReport.reportCrash(e, "PHInterstitialActivity - onAttachedToWindow()", PHCrashReport.Urgency.critical);
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public void onCreate(Bundle savedInstanceState) {
		try {
            super.onCreate(savedInstanceState);

            config = new PHConfiguration();


            // load interstitial from intent
            content = getIntent().getParcelableExtra(IntentArgument.Content.getKey());

            // if the content is empty however, we should close ourselves
            if (content.isEmpty())
                this.dismiss();

            // tag to send back to PHContentRequest
            tag     = getIntent().getStringExtra(IntentArgument.Tag.getKey());

            // register ourselves as the other half of the
            // bridge with the content requester
            BridgeManager.attachDisplayer(tag, this);

            if (getIntent().hasExtra(IntentArgument.CustomCloseBtn.getKey()))
                customCloseStates = (HashMap<String, Bitmap>)getIntent().getSerializableExtra(IntentArgument.CustomCloseBtn.getKey());


            // by default, we are cancelable only via back button (false=touch, back btn=true)
            this.setCancelable(false, true);

            // make this activity full-screen
            getWindow().requestFeature(Window.FEATURE_NO_TITLE);

            setupWebviewJSBridge();
		
		} catch (Exception e) { // swallow all exceptions...
			PHCrashReport.reportCrash(e, PHCrashReport.Urgency.critical);
		}
	}

    /** We listen for touch events so that we can cancel the interstitial
     * activity if the user taps outside the view area.
     */
	@Override
	public boolean onTouchEvent(MotionEvent event) {

        try {

            // if the user can dismiss by touching outside the
            // visible region of this interstitial, dismiss.
            if (event.getAction() == MotionEvent.ACTION_OUTSIDE) {

                if (this.getIsTouchCancelable()) {

                    // notify the content requester that
                    // we are closing via user action
                    notifyContentRequestOfClose(PHDismissType.CloseButton);

                    dismiss();
                }


                return true;
            }
        } catch (Exception e) {
            PHCrashReport.reportCrash(e, "PHInterstitialActivity - onTouchEvent()", PHCrashReport.Urgency.critical);
        }
		return false;
	}
    ///////////////////////////////////////////////////////
    //////////// Manipulatale Content Displayer ///////////

    @Override
    public String getTag() {
        return tag;
    }

    @Override
    public PHContent getContent() {
        return content;
    }

    @Override
    public void enableClosable() {
        contentView.showCloseButton();
    }

    @Override
    public void disableClosable() {
        contentView.hideCloseButton();
    }

    @Override
    public boolean isClosable() {
        return contentView.closeButtonIsShowing();
    }

    @Override
    public void launchNestedContentDisplayer(PHContent content) {
        // we transfer the bridge from the current content activity
        // to the new sub interstitial activity we are launching

        // we need some randomness to ensure no clashes with existing tags
        Random random = new Random(System.currentTimeMillis());

        // we append the sub interstitial suffix to notify ourselves in the
        // onTagChanged method not to update our own tag.
        String subcontent_tag = this.tag + SUB_INTERSTITIAL_SUFFIX + random.nextInt();

        // transfer existing bridge to maintain same content request
        // as delegate.
        BridgeManager.transferBridge(tag, subcontent_tag);

        // the new content interstitial will attach itself once launched
        PHContentRequest.displayInterstitialActivity(content, this, null, subcontent_tag);
    }

    @Override
    public String getSecret() {
        return config.getSecret(this);  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String getDeviceID() {
        return Settings.System.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
    }

    @Override
    public void openURL(String url, PHURLOpener.Listener listener) {
        PHURLOpener opener = new PHURLOpener(this, listener);

        // should not launch the response url
        opener.setShouldOpenFinalURL(false);

        opener.open(url);
    }

    @Override
    public void launchURL(String url, PHURLOpener.Listener listener) {
        PHURLOpener opener = new PHURLOpener(this, listener);

        // should launch the response url
        opener.setShouldOpenFinalURL(true);

        opener.open(url);
    }

    @Override
    public void launchSubRequest(PHSubContentRequest request) {
        request.send(this);
    }

    @Override
    public void sendEventToRequester(String event, Bundle message) {
        BridgeManager.sendMessageFromDisplayer(tag, event, message, this);
    }

    @Override
    public Context getContext() {
        return this.getApplicationContext();
    }

    @Override
    public void onTagChanged(String new_tag) {
        // if the new tag is for a subrequest (sub interstitial activity)
        // that we just launched, don't update our tag because that
        // means we just generated a new one for the sub interstitial activity
        if (new_tag.contains(SUB_INTERSTITIAL_SUFFIX))
            return;

        // otherwise the content requester changed the bridge tag,
        // we should update
        this.tag = new_tag;
    }

    @Override
    public void dismiss() {
        // notify the content request timers that we just dismissed an activity
        // so that publishers can distinguish in their .onResume() method.
        PHContentRequest.updateLastDismissedAdTime();

        contentView.close();


        // dismiss the activity
        super.finish();
    }


	///////////////////// Accessors ////////////////////////
	///////////////////////////////////////////////////////

    /** Decide whether or not the user can cancel the content interstitial
     * with the back button or touching outside the interstitial
     * @param touchCancel if the user touches outside the bounds of this interstitial,
     *                    we will close.
     * @param backCancel if the use presses the hardware back button we will close.
     */
	public void setCancelable(boolean touchCancel, boolean backCancel) {
		this.isTouchCancelable = touchCancel;
		this.isBackBtnCancelable = backCancel;
	}

	public boolean setIsBackBtnCancelable(boolean backCancel) {
		return this.isBackBtnCancelable = backCancel;
	}

	public boolean getIsTouchCancelable() {
		return this.isTouchCancelable;
	}
	
	public boolean getIsBackBtnCancelable() {
		return this.isBackBtnCancelable;
	}

	public PHContentView getRootView() {
		return contentView;
	}

	public void setContent(PHContent content) {
		if (content != null) {
			this.content = content;
		}
	}
	
	/**
	 * Checks to see if the {@link PHContent} has a valid frame
     * for the current orientation.
	 */
	private boolean contentHasFrame() {
		if (content == null)
			return false;

		int orientation = getResources().getConfiguration().orientation;

		RectF contentFrame = content.getFrame(orientation);

		// make certain the the frame has actual width and height
		return (contentFrame.width() != 0.0 && contentFrame.height() != 0.0);
	}

    /** Re-sizes the window based on the {@link PHContent}s frame.
     * Allows the server to dynamically specify the size of the window.
    */
	private void fitInterstitialWindowToContent() {
		int orientation = getResources().getConfiguration().orientation;

		// The coordinates have been calculated for us by the server (in our
		// coordinates)
		RectF contentFrame = content.getFrame(orientation);

        // should we be full screen?
		if (contentFrame.right == Integer.MAX_VALUE && contentFrame.bottom == Integer.MAX_VALUE) {
			contentFrame.right = WindowManager.LayoutParams.MATCH_PARENT;
			contentFrame.bottom = WindowManager.LayoutParams.MATCH_PARENT;
			contentFrame.top = 0.0f;
			contentFrame.left = 0.0f;

			getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
			getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
		} else {
            // or should we be non-fullscreen with a specified width and height?
			getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
			getWindow().addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
		}

		getWindow().setLayout((int) contentFrame.width(), (int) contentFrame.height());
	}

    /**
     * Called by the content view and the close button within it.
     * We notify the content requester of this close event.
     */
    @Override
    public void onClose(PHContentView contentView) {
        dismiss();

        // notify the content requester that
        // we are closing via the close button.
        notifyContentRequestOfClose(PHDismissType.CloseButton);
    }
}