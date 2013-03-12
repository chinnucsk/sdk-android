package com.playhaven.src.publishersdk.content;

import android.app.Activity;
import android.graphics.Bitmap;
import v2.com.playhaven.configuration.PHConfiguration;
import v2.com.playhaven.requests.content.PHContentRequest;
import v2.com.playhaven.utils.PHStringUtil;
import v2.com.playhaven.views.interstitial.PHCloseButton;
import com.playhaven.src.common.PHAPIRequest;
import com.playhaven.src.common.PHConfig;
import com.playhaven.src.publishersdk.content.adapters.ContentDelegateAdapter;
import com.playhaven.src.publishersdk.content.adapters.PurchaseDelegateAdapter;
import com.playhaven.src.publishersdk.content.adapters.RewardDelegateAdapter;

import java.lang.ref.WeakReference;

/**
 * Class adapter for {@link v2.com.playhaven.requests.content.PHContentRequest}.
 */
public class PHPublisherContentRequest extends PHContentRequest implements PHAPIRequest {

    /** Our handle to the context we'll need to send the request. */
    public WeakReference<Activity> context;

    /** We need a reference to the content adapter to properly handling setting failure/content
     * listener.
     */
    private ContentDelegateAdapter content_adapter;



    public enum PHDismissType {
        ContentUnitTriggered, // content template dismissal
        CloseButtonTriggered, // called from close button
        ApplicationTriggered, // application currentState
        NoContentTriggered    // Usually on error
    };


    public static interface FailureDelegate {
        //these two methods handle the request failing in general, and then the content request failing..
        public void didFail(PHPublisherContentRequest request, String error);
        public void contentDidFail(PHPublisherContentRequest request, Exception e);
    }

    /** We maintain a copy of the customize listener to properly set the custom close button images.
     * We ask for new images right before sending. The method {@link CustomizeDelegate#borderColor(PHPublisherContentRequest, PHContent)}
     * does nothing and never has.
     */
    private CustomizeDelegate customize_delegate;

    public static interface CustomizeDelegate {
        public Bitmap closeButton (PHPublisherContentRequest request, PHContentView.ButtonState state);
        public int borderColor	  (PHPublisherContentRequest request, PHContent content);
    }


    /**
     * Callback interface for content listener.
     */
    public static interface ContentDelegate extends PHAPIRequest.Delegate {
        public void willGetContent		(PHPublisherContentRequest request					  );
        public void willDisplayContent	(PHPublisherContentRequest request, PHContent content );
        public void didDisplayContent	(PHPublisherContentRequest request, PHContent content );
        public void didDismissContent	(PHPublisherContentRequest request, PHDismissType type);
    }

    public static interface RewardDelegate {
        public void unlockedReward(PHPublisherContentRequest request, PHReward reward);
    }

    public static interface PurchaseDelegate {
        public void shouldMakePurchase(PHPublisherContentRequest request, PHPurchase purchase);
    }


    ////////////////////////////////////////////
    ///////////////// Constructors /////////////
    public PHPublisherContentRequest(Activity activity, String placement) {
        super(placement);

        this.context = new WeakReference<Activity>(activity);
    }

    public PHPublisherContentRequest(Activity activity, ContentDelegate delegate, String placement) {
        this(activity, placement);

        setDelegates(delegate);
    }

    ////////////////////////////////////////////
    //////////////// Listener Setters //////////

    public void setOnContentListener(ContentDelegate content_listener) {
        // if we have an existing listener, we need to carry forward the
        // failure listener
        FailureDelegate existing_failure_delegate = null;

        if (content_adapter != null)
            existing_failure_delegate = content_adapter.getFailureDelegate();

        PHAPIRequest.Delegate existing_request_delegate = null;

        if (content_adapter != null)
            existing_request_delegate = content_adapter.getRequestDelegate();

        super.setOnContentListener(content_adapter = new ContentDelegateAdapter(content_listener, existing_failure_delegate, existing_request_delegate));
    }

    public void setOnRewardListener(RewardDelegate reward_listener) {
        super.setOnRewardListener(new RewardDelegateAdapter((RewardDelegate) reward_listener));
    }

    public void setOnPurchaseListener(PurchaseDelegate purchase_listener) {
        super.setOnPurchaseListener(new PurchaseDelegateAdapter((PurchaseDelegate) purchase_listener));
    }

    public void setOnCustomizeListener(CustomizeDelegate customize_listener) {
        customize_delegate = customize_listener;
    }

    public void setOnFailureListener(FailureDelegate failure_listener) {
        // if we have an existing listener, we need to carry forward the
        // content listener
        ContentDelegate existing_content_delegate = null;

        if (content_adapter != null)
            existing_content_delegate = content_adapter.getContentDelegate();

        PHAPIRequest.Delegate existing_request_delegate = null;

        if (content_adapter != null)
            existing_request_delegate = content_adapter.getRequestDelegate();

        super.setOnContentListener(content_adapter = new ContentDelegateAdapter(existing_content_delegate, failure_listener, existing_request_delegate));

    }


    /** Set the different content request delegates using the sneaky 'instanceof' operator. It's a bit hacky but works for our purposes.
     */
    public void setDelegates(Object delegate) {

        if (delegate instanceof RewardDelegate)
            super.setOnRewardListener(new RewardDelegateAdapter((RewardDelegate) delegate));
        else
            PHStringUtil.log("*** RewardDelegate is not implemented. If you are using rewards this needs to be implemented.");

        if (delegate instanceof PurchaseDelegate)
            super.setOnPurchaseListener(new PurchaseDelegateAdapter((PurchaseDelegate) delegate));
        else
            PHStringUtil.log("*** PurchaseDelegate is not implemented. If you are using VGP this needs to be implemented.");

        if (delegate instanceof CustomizeDelegate)
            customize_delegate = (CustomizeDelegate)delegate;
        else {
            customize_delegate = null;
            PHStringUtil.log("*** CustomizeDelegate is not implemented, using Play Haven close button bitmap. Implement to use own close button bitmap.");
        }

        if (delegate instanceof FailureDelegate)
            super.setOnContentListener(new ContentDelegateAdapter(null, (FailureDelegate) delegate, null));
        else
            PHStringUtil.log("*** FailureDelegate is not implemented. Implement if want to be notified of failed content downloads.");

        if (delegate instanceof ContentDelegate)
            super.setOnContentListener(new ContentDelegateAdapter((ContentDelegate) delegate, null, null));
        else
            PHStringUtil.log("*** ContentDelegate is not implemented. Implement if want to be notified of content request states.");

        // if it is only a regular request listener,
        // we should still use it.
        if (delegate instanceof Delegate)
            super.setOnContentListener(new ContentDelegateAdapter(null, null, (Delegate) delegate));

        // if the listener provided implements *both*
        // the ContentDelegate and the FailureDelegate we need to tell
        // the adapter.
        if (delegate instanceof ContentDelegate && delegate instanceof FailureDelegate)
            super.setOnContentListener(new ContentDelegateAdapter((ContentDelegate) delegate, (FailureDelegate) delegate, null));

    }

    /**
     * Forwards the call to {@link PHContentRequest#didJustShowAd()}.
     */
    public static boolean didDismissContentWithin(long range) {
        return PHContentRequest.didJustShowAd();
    }

    /**
     * Shows a light-overlay view before the content is loaded.
     * No longer functional
     * @param doOverlay flag indicating whether or not the overlay should
     *                  show immediately.
     */
    public void setOverlayImmediately(boolean doOverlay) {
         // no - op
    }

    /**
     * Gets whether or not a light-overlay view is shown before the content
     * view is loaded. No longer functional.
     * @return whether or not we show the overlay before we load the content.
     */
    public boolean getOverlayImmediately() {
        return false;
    }

    /** attempts to grab the close button images from the listener. */
    private void getCloseButtonImages() {
        // try to grab the custom close button images

        if (customize_delegate != null) {
            Bitmap active       = customize_delegate.closeButton(this, PHContentView.ButtonState.Down   );
            Bitmap inactive     = customize_delegate.closeButton(this, PHContentView.ButtonState.Up     );

            if (active != null && inactive != null) {
                // pass these onto the new interface
                super.setCloseButton(active,   PHCloseButton.CloseButtonState.Down);

                super.setCloseButton(inactive, PHCloseButton.CloseButtonState.Up  );
            }
        }

    }

    @Override
    public void setDelegate(Delegate delegate) {
        // if we have an existing listener, we need to carry forward the
        // failure listener
        FailureDelegate existing_failure_delegate = null;

        if (content_adapter != null)
            existing_failure_delegate = content_adapter.getFailureDelegate();

        // if the listener is simply a content listener
        // in disguise, handle it normally
        if (ContentDelegate.class.isInstance(delegate)) {
            this.setOnContentListener(new ContentDelegateAdapter((ContentDelegate) delegate, existing_failure_delegate, null));
        } else {
            // it is a standard PHAPIRequest listener. We should use it.
            // we overwrite any existing content listener.
            this.setOnContentListener(new ContentDelegateAdapter(null, existing_failure_delegate, delegate));
        }

    }


    /** Pre-loads this content
     */
    public void preload() {
        // try to grab any custom close button states
        getCloseButtonImages();

        // we need to pull the token/secret in
        PHConfiguration config = new PHConfiguration();

        config.setCredentials(context.get(), PHConfig.token, PHConfig.secret);

        // we need to pull the precaching values in
        config.setShouldPrecache(context.get(), PHConfig.precache);

        super.preload(context.get());
    }


    /**
     * Sends the content request.
     */
    @Override
    public void send() {

        // try to grab any custom close button states
        getCloseButtonImages();

        // we need to pull the token/secret in
        PHConfiguration config = new PHConfiguration();

        config.setCredentials(context.get(), PHConfig.token, PHConfig.secret);

        // we need to pull the precaching values in
        config.setShouldPrecache(context.get(), PHConfig.precache);

        super.send(context.get());
    }
}
