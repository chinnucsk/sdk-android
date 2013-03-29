package v2.com.playhaven.interstitial.requestbridge.base;

import android.content.Context;
import v2.com.playhaven.model.PHContent;

/**
 * <p>
 * Abstract interface for displaying a content response
 * from the server. Currently only {@link v2.com.playhaven.interstitial.PHInterstitialActivity}
 * implements this interface.
 *
 * </p>
 *
 * <p>
 *  This interface completes one half of a {@link RequestBridge}.
 * </p>
 */
public interface ContentDisplayer {

    /** Closes the displayer */
    public void dismiss();

    /** Gets the underlying {@link PHContent} that this
     * displayer is displaying.
     * @return a non-null instance of {@link PHContent} that this displayer
     *         is currently displaying.
     */
    public PHContent getContent();

    /** Gets the tag that the corresponding {@link v2.com.playhaven.requests.content.PHContentRequest}
     * uses to uniquely identify it.
     * @return a unique tag that indicates the appropriate {@link RequestBridge} for this displayer.
     */
    public String getTag();

    /**
     * Gets the context this displayer is in.
     * @return a valdi context for this displayer.
     */
    public Context getContext();

    /**
     * Allows the content displayer to handle the event in which someone changes
     * the tag of the {@link RequestBridge}.
     * @param new_tag the new tag.
     */
    public void onTagChanged(String new_tag);

}
