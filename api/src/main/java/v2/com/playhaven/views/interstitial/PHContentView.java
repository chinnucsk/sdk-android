package v2.com.playhaven.views.interstitial;

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.os.Handler;
import android.view.View;
import android.widget.RelativeLayout;
import v2.com.playhaven.interstitial.jsbridge.ManipulatableContentDisplayer;
import v2.com.playhaven.interstitial.jsbridge.PHJSBridge;
import v2.com.playhaven.interstitial.webview.PHWebViewChrome;
import v2.com.playhaven.interstitial.webview.PHWebViewClient;
import v2.com.playhaven.model.PHContent;
import v2.com.playhaven.utils.PHConversionUtils;

/**
 * The root <em>view</em> of the {@link v2.com.playhaven.interstitial.PHInterstitialActivity}. This view controls
 * the presentation of the content and contains the webview and tearDownBridge button. However, it only manages
 * the <em>presentation</em> of the content and passes all control up to the webview.
 */
public class PHContentView extends RelativeLayout implements PHCloseButton.Listener {


    /** The special listener this view uses to communicate with its
     * containing {@link v2.com.playhaven.interstitial.PHInterstitialActivity}
     * Currently we only inform the activity when we wish to tearDownBridge.
     */
    public static interface Listener {
        public void onClose(PHContentView contentView);
    }

    /**
     * The listener for this content view. This is most often
     * {@link v2.com.playhaven.interstitial.PHInterstitialActivity}.
     */
    private Listener listener;

    /** The webview used for displaying the content templates*/
    private PHWebView webview;

    /**
     * The handler used as a timer queue for the auto-show tearDownBridge
     * button
     */
    private Handler closeButtonTimerHandler;

    /** A simple runnable we send to the {@link PHContentView#closeButtonTimerHandler}
     * with a delayed time of {@link PHContentView#TIME_BEFORE_SHOW_CLOSE_BUTTON}.
     */
    private Runnable showCloseButtonRunnable;


    /** Distance from upper right hand corner for tearDownBridge button.
     * This effectively acts as a margin on the top and left
     * for the tearDownBridge button. The units are DIPs.
     */
    private final float CLOSE_BUTTON_MARGIN = 2.0f;

    /**
     * The amount of time before we show the tearDownBridge button
     * if the content template has not already shown it. The user
     * should always have the option to tearDownBridge the ad.
     */
    private static final int TIME_BEFORE_SHOW_CLOSE_BUTTON = 4000;

    /**
     * The tearDownBridge button in the upper left-hand corner that allows
     * the user to tearDownBridge the underlying activity.
     */
    private PHCloseButton closeButton;

    /** A reference to the content we are displaying */
    private PHContent content;

    /** The verbose instructor for creating a new content view.
     * @param contentDisplayer the content displayer we will need to manipulate (often an instance
     *                         {@link v2.com.playhaven.interstitial.PHInterstitialActivity}
     * @param context           A valid context needed for inflating ourselves
     * @param content          The content this view will be displaying
     * @parma listener         The listener for this content view
     * @param bridge           The bridge between native code and javascript. We'll attach the webview
     *                         we create to this bridge and the {@link v2.com.playhaven.interstitial.PHInterstitialActivity}
     *                         will create the handlers on the other side.
     * @param custom_active    The custom "down" image for the close button. Can be null.
     * @param custom_inactive  The custom "up" image for the close button. Can be null.
     */
    public PHContentView(ManipulatableContentDisplayer contentDisplayer,
                         Context context,
                         PHContent content,
                         Listener listener,
                         PHJSBridge bridge,
                         BitmapDrawable custom_active,
                         BitmapDrawable custom_inactive) {
        super(context);

        this.listener = listener;
        this.content  = content;

        ///////////////////////////////////
        /////// Setup close button ///////
        //////////////////////////////////

        setupCloseButton(custom_active, custom_inactive);

        ////////////////////////////////////////////
        //////// Setup WebView  ////////////////////
        ////////////////////////////////////////////

        setupWebview(context, bridge, contentDisplayer);
    }

    /** Stops the close button timer and cleans up the webview */
    public void cleanup() {
        // stop timer to show tearDownBridge button
        if (closeButtonTimerHandler != null)

            closeButtonTimerHandler.removeCallbacks(showCloseButtonRunnable);

        webview.setWebChromeClient  (null);
        webview.setWebViewClient    (null);
    }

    /** Simple utility method that adds the tearDownBridge
     * button to the view.
     * @param custom_active the custom image for the active state of the close button.
     * @param custom_inactive the custom image for the inactive state of the close button.
     */
    private void setupCloseButton(BitmapDrawable custom_active, BitmapDrawable custom_inactive) {
        // convert the DIP margin to actual pixels based
        // on the density of the device's screen.
        float marginInPixels = PHConversionUtils.dipToPixels (   getContext(),   CLOSE_BUTTON_MARGIN);



        // create the tearDownBridge button and set ourselves as listener
        // depending on whether or not we are provided with custom state images,
        // pass them into the close button

        if (custom_active == null || custom_inactive == null)
            closeButton = new PHCloseButton(getContext(), this);
        else
            closeButton = new PHCloseButton(getContext(), this, custom_active, custom_inactive);

        RelativeLayout.LayoutParams closeLayoutParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT);

        // the tearDownBridge button should be at the far right hand side of the
        // container view
        closeLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);

        closeLayoutParams.setMargins    (   0,
                (int) marginInPixels,
                (int) marginInPixels,
                0
        );

        closeButton.setLayoutParams(closeLayoutParams);

        addView(closeButton);

        // hide the tearDownBridge button initially.....
        closeButton.setVisibility( View.INVISIBLE );

        // ....but show it by default after a few seconds
        startShowCloseButtonTimer();
    }

    /** Simple utility method that adds the webview to the content view */
    private void setupWebview(Context context, PHJSBridge bridge, ManipulatableContentDisplayer contentDisplayer) {
        PHWebViewClient client = new PHWebViewClient(contentDisplayer, bridge, content);

        PHWebViewChrome chrome = new PHWebViewChrome();

        webview = new PHWebView(this.getContext(), true, client, chrome, content);

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);


        webview.setLayoutParams(params);

        // add the webview to the specified JS-to-java bridge
        bridge.attachWebview(webview);

        addView(webview);


        // load the actual content template which will
        // in turn load the content.
        webview.loadContentTemplate();

    }

    public void close() {
        webview.cleanup();
    }

    /**
     * A callback from the tearDownBridge button when it is clicked.
     */
    @Override
    public void onClose(PHCloseButton button) {
        if (listener != null)
            listener.onClose(this);
    }

    /** Returns whether or not we are showing the close button */
    public boolean closeButtonIsShowing() {
        return (closeButton.getVisibility() == View.VISIBLE);
    }

    /**
     * Display the tearDownBridge button. Usually called after a timeout if the interstitial
     * view doesn't show it.
     */
    public void showCloseButton() {
        // we want the close button on top of everything
        closeButton.bringToFront();

        closeButton.setVisibility(View.VISIBLE);
    }

    /**
     * Explicitly hides the tearDownBridge button and stops
     * the timer for showing the tearDownBridge button by default.
     */
    public void hideCloseButton() {

        // cancel the timer used to show the tearDownBridge button by default
        if (closeButtonTimerHandler != null)

            closeButtonTimerHandler.removeCallbacks(showCloseButtonRunnable);

        closeButton.setVisibility(View.GONE);

    }

    /** Creates a new runnable and posts it to the {@link PHContentView#closeButtonTimerHandler}
     * as a delayed notification. Thus, we are effectively starting a timer
     * before displaying the tearDownBridge button.
     */
    private void startShowCloseButtonTimer() {

        closeButtonTimerHandler = new Handler();

        showCloseButtonRunnable  = new Runnable() {

            public void run() {

                showCloseButton();

            }

        };

        // start the timer before showing the tearDownBridge button
        closeButtonTimerHandler.postDelayed(showCloseButtonRunnable,
                                            TIME_BEFORE_SHOW_CLOSE_BUTTON);
    }
}
