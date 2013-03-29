package v2.com.playhaven.interstitial.requestbridge;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import v2.com.playhaven.interstitial.requestbridge.base.ContentDisplayer;
import v2.com.playhaven.interstitial.requestbridge.base.ContentRequester;
import v2.com.playhaven.interstitial.requestbridge.base.RequestBridge;
import v2.com.playhaven.utils.PHStringUtil;

import java.util.HashMap;

/**
 * Special static class that manages a series of "bridges" or "connections"
 * between a {@link ContentRequester} and {@link ContentDisplayer}. It's a singleton
 * because it must exist independently of either "end."
 *
 * Note: although {@link BridgeManager} is a kind of singleton, only the "endpoints"
 * or the {@link ContentRequester} and {@link ContentDisplayer} should be sending messages over
 * <em>their</em> bridge. Third parties should <em><strong>not</strong></em> interrupt. Of course,
 * the currently (flawed) architecture for {@link v2.com.playhaven.model.PHPurchase} resolution reporting
 * violates this policy but in general one should not.
 */
public class BridgeManager {

    /** The bridges between content displayers and requesters indexed by tag name */
    public static HashMap<String, RequestBridge> bridges = new HashMap<String, RequestBridge>();

    /** Creates a new request bridge with the specified tag.
     * @param tag the <em>unique</em> tag for this bridge
     * @param bridge the bridge between a content requester and a content displayer
     */
    public static void openBridge(String tag, RequestBridge bridge) {
        bridges.put(tag, bridge);
    }

    /** Closes the bridge <em>uniquely</em> identified by the given tag */
    public static void closeBridge(String tag) {
        RequestBridge bridge = bridges.get(tag);

        if (bridge == null) return;
        
        // close() unregisters the broadcast receivers attached to the bridge, 
        // this was preventing the dismiss callback from being received when 
        // orientation changes. requesterReceiver and displayerReceiver are 
        // both members of the given RequestBridge. 
        
        //bridge.close();

        bridges.remove(tag);
    }

    /**
     * Attaches a {@link ContentRequester} to one side of the bridge identified
     * by the tag.
     * @param tag the tag uniquely identifying the bridge
     * @param requester the requester we wish to attach to the bridge
     */
    public static void attachRequester(String tag, ContentRequester requester) {
        RequestBridge bridge = bridges.get(tag);

        if (bridge == null) return;

        bridge.attachRequester(requester);
    }

    public static void detachRequester(String tag) {
        RequestBridge bridge = bridges.get(tag);

        if (bridge == null) return;

        bridge.detachRequester();
    }

    public static ContentRequester getRequester(String tag) {
        RequestBridge bridge = bridges.get(tag);

        if (bridge == null) return null;

        return bridge.getRequester();
    }

    public static void attachDisplayer(String tag, ContentDisplayer displayer) {
        RequestBridge bridge = bridges.get(tag);

        if (bridge == null) return;

        bridge.attachDisplayer(displayer);
    }

    /**
     * Transfers an existing bridge to a new unique tag identifier. Either
     * side of the bridge can request an ID transfer so we have to notify both
     * sides of the id change so that they can update their tags appropriately.
     * We also notify the bridge itself that it now possesses a new tag.
     * @param old_tag the tag we are transferring from
     * @param new_tag the tag we are transferring to
     */
    public static void transferBridge(String old_tag, String new_tag) {
        RequestBridge bridge = bridges.get(old_tag);

        if (bridge == null) return;

        bridges.put(new_tag, bridge);

        bridges.remove(old_tag);

        // notify the bridge and the bridge will notify both sides
        bridge.onTagChanged(new_tag);


    }
    public static void detachDisplayer(String tag) {
        RequestBridge bridge = bridges.get(tag);

        if (bridge == null) return;

        bridge.detachDisplayer();
    }

    public static ContentDisplayer getDisplayer(String tag) {
        RequestBridge bridge = bridges.get(tag);

        if (bridge == null) return null;

        return bridge.getDisplayer();
    }

    public static RequestBridge getBridge(String tag) {
        return bridges.get(tag);
    }


    private static void sendBroadcast(String intentFilter, String tag, String event, Bundle message, Context context) {


        Intent intent = new Intent(intentFilter);

        // attach the necessary notification
        message.putString(RequestBridge.Message.Tag.getKey(),   tag); // the tag for uniquely identifying ourselves
        message.putString(RequestBridge.Message.Event.getKey(), event); // what is the actual event?

        // attach the message payload to the intent
        intent.putExtra(RequestBridge.BROADCAST_METADATA_KEY, message);

        // send the broadcast
        context.getApplicationContext().sendBroadcast(intent);
    }

    /**
     * Allows the requester to send a message to the displayer
     *
     * @param tag the tag uniquely identifying the bridge
     * @param event the event the requester wishes to send to the displayer
     * @param message the message the requester wishes to send to the displayer
     * @param context the context the used for sending the broadcast event
     */
    public static void sendMessageFromRequester(String tag, String event, Bundle message, Context context) {
        RequestBridge bridge = bridges.get(tag);

        if (bridge == null) return;

        // send a message *to* the displayer receiver
        sendBroadcast(bridge.getDisplayerIntentFilter(), tag, event, message, context);
    }

    /**
     * Allows the displayer to send a message to the requester
     *
     * @param tag the tag uniquely identifying the bridge
     * @param event the event the requester wishes to send to the requester
     * @param message the message the requester wishes to send to the requester
     * @param context the context the used for sending the broadcast event
     */
    public static void sendMessageFromDisplayer(String tag, String event, Bundle message, Context context) {

        RequestBridge bridge = bridges.get(tag);

        if (bridge == null) return;

        PHStringUtil.log("Sending message from displayer: " + event);

        // send a message *to* the displayer receiver
        sendBroadcast(bridge.getRequesterIntentFilter(), tag, event, message, context);
    }

}
