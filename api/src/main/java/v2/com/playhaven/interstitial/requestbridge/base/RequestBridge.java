package v2.com.playhaven.interstitial.requestbridge.base;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import v2.com.playhaven.utils.PHStringUtil;

/**
 * <p>
 * Bridge between an individual {@link v2.com.playhaven.interstitial.requestbridge.base.ContentDisplayer}
 * and its originating {@link ContentRequester}. It handles the message passing between the requester
 * and the displayer.
 * </p>
 *
 * <p>
 * You should extend this class to implement specific interfaces between these two components.
 * This class provides the base methods to create a bi-directional bridge using {@see BroadcastReceiver}s.
 * </p>
 *
 * <p>
 * We are effectively using the {@link BroadcastReceiver}s to ensure proper synchronization between activities and avoid threading
 * errors since {@link BroadcastReceiver}s "dehydrate" and copy resources via {@link android.os.Parcelable}s before
 * using them again. This decision is a continuation of the existing architecture but remains open to improvemnt.
 * </p>
 *
 * <p>
 * Either the requester or displayer has to open the bridge before the other "end" completes it. For example,
 * a {@link v2.com.playhaven.requests.content.PHContentRequest} creates the bridge and attaches itself
 * before the {@link v2.com.playhaven.interstitial.PHInterstitialActivity} opens and attaches itself.
 * </p>
 */
public abstract class RequestBridge {


    public static enum Message {
        Event		("event_contentview"),
        Tag			("content_tag");

        private String key;

        public String getKey() {
            return key;
        }

        private Message(String key) {
            this.key = key;
        }
    }

    /** the tag used to uniquely identify this bridge*/
    private String tag;

    /** the content requester half of this bridge */
    private ContentRequester requester;

    /** the displayer half of this bridge */
    private ContentDisplayer displayer;

    /** the key used for passing additional data via a broadcast receiver */
    public static final String BROADCAST_METADATA_KEY = "v2.com.playhaven.notification";

    /** the broadcast receiver for the requester that receives messages
     * from the displayer.
     */
    private BroadcastReceiver requesterReceiver;

    /** the broadcast receiver for the displayer that receives
     * messages from the requester.
     */
    private BroadcastReceiver displayerReceiver;

    /**
     * Creates a new request bridges with the specified
     * unique tag.
     * @param tag a unique tag.
     */
    public RequestBridge(String tag) {
        this.tag = tag;
    }

    /** Sets the requester for the requester half of the bridge.
     * @param requester the requester half of the bridge
     */
    public void attachRequester(ContentRequester requester) {
        this.requester = requester;

        // register this side of the bridge
        registerRequesterReceiver();

        // let the subclass handle it specially
        onRequesterAttached(requester);
    }

    /**
     * Removes the requester for the requester half of the bridge.
     */
    public void detachRequester() {
        if (requester != null)
            requester.getContext().unregisterReceiver(this.requesterReceiver);

        this.requester = null;
    }

    /**
     * Gets the requester half of the bridge.
     * @return the requester.
     */
    public ContentRequester getRequester() {
        return this.requester;
    }

    /**
     * Adds the displayer half of the bridge.
     * @param displayer the displayer half of the bridge.
     */
    public void attachDisplayer(ContentDisplayer displayer) {
        this.displayer = displayer;

        // register this side of the bridge
        registerDisplayerReceiver();

        // let the subclass handle it specially
        onDisplayerAttached(displayer);
    }

    /**
     * Removes the displayer half of the bridge
     */
    public void detachDisplayer() {
        if (displayer != null)
            displayer.getContext().unregisterReceiver(this.displayerReceiver);

        this.displayer = null;
    }

    /**
     * Gets the displayer half of the bridge
     * @return the content displayer.
     */
    public ContentDisplayer getDisplayer() {
        return this.displayer;
    }

    /**
     * Closes this bridge by un-registering both receivers
     * for the content requester and content displayer.
     */
    public void close() {

        detachRequester();

        detachDisplayer();

        cleanup();

    }

    /** utility method for registering the receiver
     * for the displayer to receive messages from the requester.
     */
    private void registerDisplayerReceiver() {

        if (displayer.getContext() == null) return;

        displayerReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {

                // get the additional notification from the request
                Bundle md 	     = intent.getBundleExtra(BROADCAST_METADATA_KEY);

                // get the actual event title
                String event     = md.getString(Message.Event.getKey());

                // the tag used for identifying the PHInterstitialActivity
                String tag      = md.getString(Message.Tag.getKey());

                // ignore the notification if it comes from an
                // activity we didn't create
                if (tag == null || !tag.equals(RequestBridge.this.tag)) return; // only process if it is relevant to us


                PHStringUtil.log("Receiving message from requester: " + event);

                onRequesterSentMessage(event, md);

            }
        };

        displayer.getContext().registerReceiver(displayerReceiver, new IntentFilter(getDisplayerIntentFilter()));
    }

    /**
     * Utility method to register the receiver for the requester
     * to receive messages from the displayer.
     */
    private void registerRequesterReceiver() {

        if (requester.getContext() == null) return;

        requesterReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {

                // get the additional notification from the request
                Bundle md 	     = intent.getBundleExtra(BROADCAST_METADATA_KEY);

                // get the actual event title
                String event     = md.getString(Message.Event.getKey());

                // the tag used for identifying the PHInterstitialActivity
                String tag      = md.getString(Message.Tag.getKey());

                // ignore the notification if it comes from an
                // activity we didn't create
                if (tag == null || !tag.equals(RequestBridge.this.tag)) return; // only process if it is relevant to us

                PHStringUtil.log("Receiving message from displayer: " + event);

                onDisplayerSentMessage(event, md);

            }
        };

        requester.getContext().registerReceiver(requesterReceiver, new IntentFilter(getRequesterIntentFilter()));
    }

    /**
     *  Handles the event of tag change.
     * @param new_tag the new tag which has been changed externally.
     */
    public void onTagChanged(String new_tag) {
        this.tag = new_tag;

        if (requester != null)
            requester.onTagChanged(new_tag);

        if (displayer != null)
            displayer.onTagChanged(new_tag);
    }

    public String getTag() {
        return tag;
    }

    /** Optional for subclasses to override*/
    public void onRequesterAttached(ContentRequester requester) { }

    /** Optional for subclasses to override*/
    public void onDisplayerAttached(ContentDisplayer displayer) { }

    /** returns the intent filter string for the requester
     * to receive messages.
     * @return a unique intent filter that allows the requester to receive messages.
     */
    public abstract String getRequesterIntentFilter();

    /**
     * Returns the intent filter string for the displayer
     * to receive messages
     * @return a unique intent filter that allows the displayer to receive messages.
     */
    public abstract String getDisplayerIntentFilter();

    /**
     * Any cleanup the subclass would like to complete.
     */
    public abstract void cleanup();

    /**
     * Event handler for the requester sending messages
     * to the displayer
     * @param event the event that the requester sent
     * @param messageData the payload that the requester sent
     */
    public abstract void onRequesterSentMessage(String event, Bundle messageData);

    /**
     * Event handler for the displayer sending messages
     * to the requester
     * @param event the event that the displayer sent
     * @param messageData the payload that the displayersent
     */
    public abstract void onDisplayerSentMessage(String event, Bundle messageData);
}
