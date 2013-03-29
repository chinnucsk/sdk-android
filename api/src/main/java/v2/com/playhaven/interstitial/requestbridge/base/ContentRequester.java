package v2.com.playhaven.interstitial.requestbridge.base;

import android.content.Context;
import v2.com.playhaven.model.PHContent;

/**
 * Abstract interface for requesting content. Currently only
 * {@link v2.com.playhaven.requests.content.PHContentRequest} implements this interface.
 *
 * <p>
 *     This interface represents one of a {@link RequestBridge}
 * </p>
 */
public interface ContentRequester {

    /** The unique tag the bridge uses to identify this request and
     * pair it with a {@link ContentDisplayer}.
     * @return a unique tag that identifies the appropriate bridge
     * for this requester.
     */
    public String getTag();

    /** The content this class has downloaded or "requested"
     * from the server.
     * @return a non-null instance of {@link PHContent}
     */
    public PHContent getContent();

    /**
     * Gets the context of this requester
     * @return a valid context.
     */
    public Context getContext();

    /**
     * Allows the content requester to handle the event of the {@link RequestBridge}
     * tag changing.
     * @param new_tag the new tag value
     */
    public void onTagChanged(String new_tag);
}
