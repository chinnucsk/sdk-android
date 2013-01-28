package com.playhaven.src.publishersdk.open;

import android.content.Context;
import v2.com.playhaven.configuration.PHConfiguration;
import v2.com.playhaven.requests.open.PHOpenRequest;
import com.playhaven.src.common.PHAPIRequest;
import com.playhaven.src.common.PHConfig;

import java.lang.ref.WeakReference;

/**
 * Part of shim layer to ensure backward compatibility with older games. This class
 * is an adapter for {@link v2.com.playhaven.requests.open.PHOpenRequest}.
 * @deprecated
 */
public class PHPublisherOpenRequest extends PHOpenRequest implements PHAPIRequest {

    /** We need to hang onto the context since the old interface
     * requires it passed to the constructor and not to the send()
     * method.
     */
    private WeakReference<Context> context;

    /** Pre-fetch listener interface.
     * @deprecated As of 1.12.2 you should use {@link v2.com.playhaven.listeners.PHPrefetchListener}. */
    public static interface PrefetchListener {
        public void prefetchFinished(PHPublisherOpenRequest request);
    }

    /**
     * Sets the prefetch listener
     * @deprecated As of 1.12.2 you should use {@link v2.com.playhaven.listeners.PHPrefetchListener}
     */
    public void setPrefetchListener(PrefetchListener delegate) {
        super.setPrefetchListener(new PrefetchDelegateAdapter(delegate));
    }

    /**
     * Constructs a new open request
     * @deprecated As of 1.12.2 you should use {@link PHOpenRequest} and the corresponding callback interfaces.
     */
    public PHPublisherOpenRequest(Context context, PHAPIRequest.Delegate delegate) {
        this(context);
        this.setDelegate(delegate);
    }

    public PHPublisherOpenRequest(Context context) {
        super();

        this.context = new WeakReference<Context>(context);
    }

    @Override
    /** Sends the content request.
     * @deprecated As of 1.12.2. You should use {@link PHOpenRequest} instead.
     */
    public void send() {

        // we need to pull the token/secret in
        PHConfiguration config = new PHConfiguration();

        config.setCredentials(context.get(), PHConfig.token, PHConfig.secret);

        super.send(context.get());
    }


    public void setDelegate(Delegate delegate) {
        // use the adapter to plug directly into the new interface
        this.setOpenRequestListener(new APIRequestDelegateAdapter(delegate));
    }

}
