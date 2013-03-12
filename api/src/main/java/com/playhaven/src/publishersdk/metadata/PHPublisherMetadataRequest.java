package com.playhaven.src.publishersdk.metadata;

import android.content.Context;
import v2.com.playhaven.configuration.PHConfiguration;
import v2.com.playhaven.requests.badge.PHBadgeRequest;
import com.playhaven.src.common.PHAPIRequest;
import com.playhaven.src.common.PHConfig;

import java.lang.ref.WeakReference;

/**
 * An adapter for the {@link v2.com.playhaven.requests.badge.PHBadgeRequest}.
 */
public class PHPublisherMetadataRequest extends PHBadgeRequest implements PHAPIRequest {

    /** we need a reference to the context for use in the .send()*/
    private WeakReference<Context> context;

    public PHPublisherMetadataRequest(Context context, String placement) {
        super(placement);

        this.context = new WeakReference<Context>(context);
    }

    public PHPublisherMetadataRequest(Context context, PHAPIRequest.Delegate delegate, String placement) {
        this(context, placement);

        super.setMetadataListener(new MetadataDelegateAdapter(delegate));
    }

    @Override
    public void send() {
        // we need to pull the token/secret in
        PHConfiguration config = new PHConfiguration();

        config.setCredentials(context.get(), PHConfig.token, PHConfig.secret);

        super.send(context.get());
    }

    @Override
    public void setDelegate(Delegate delegate) {
        super.setMetadataListener(new MetadataDelegateAdapter(delegate));
    }
}
