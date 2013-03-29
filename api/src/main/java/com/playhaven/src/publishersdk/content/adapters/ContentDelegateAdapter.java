package com.playhaven.src.publishersdk.content.adapters;

import v2.com.playhaven.listeners.PHContentRequestListener;
import v2.com.playhaven.model.PHContent;
import v2.com.playhaven.model.PHError;
import v2.com.playhaven.requests.content.PHContentRequest;

import com.playhaven.src.common.PHAPIRequest;
import com.playhaven.src.publishersdk.content.PHPublisherContentRequest;
import com.playhaven.src.utils.EnumConversion;

/**
 * Object based adapter for content listener. It joins together the ContentDelegate, PHAPIRequest.Listener, and
 * FailureDelegate. You can pass in null for any of the above delegates to indicate that that listener isn't set.
 */
public class ContentDelegateAdapter implements PHContentRequestListener {

    /** our content listener adaptee */
    private PHPublisherContentRequest.ContentDelegate content_delegate;

    /** our failure listener adaptee */
    private PHPublisherContentRequest.FailureDelegate failure_delegate;

    /** just the plain old request listener adaptee */
    private PHAPIRequest.Delegate request_delegate;


    public ContentDelegateAdapter(PHPublisherContentRequest.ContentDelegate content_delegate,
                                  PHPublisherContentRequest.FailureDelegate failure_delegate,
                                  PHAPIRequest.Delegate delegate) {

         this.content_delegate   = content_delegate;
         this.failure_delegate   = failure_delegate;
         this.request_delegate   = delegate;
    }

    public boolean hasFailureDelegate() {
        return (failure_delegate != null);
    }

    public boolean hasContentDelegate() {
        return (content_delegate != null);
    }


    public boolean hasRequestDelegate() {
        return (request_delegate != null);
    }

    public PHPublisherContentRequest.ContentDelegate getContentDelegate() {
        return content_delegate;
    }

    public PHPublisherContentRequest.FailureDelegate getFailureDelegate() {
        return failure_delegate;
    }

    public PHAPIRequest.Delegate getRequestDelegate() {
        return request_delegate;
    }

    @Override
    public void onSentContentRequest(PHContentRequest request) {
        if (content_delegate != null)
            content_delegate.willGetContent((PHPublisherContentRequest) request);
    }

    @Override
    public void onReceivedContent(PHContentRequest request, PHContent content) {
        // we prefer the base request listener over the
        // more robust content listener.
        if (request_delegate != null)
            request_delegate.requestSucceeded((PHAPIRequest) request, content.context);

        else if (content_delegate != null)
            content_delegate.requestSucceeded((PHAPIRequest) request, content.context);
    }

    @Override
    public void onWillDisplayContent(PHContentRequest request, PHContent content) {
        if (content_delegate != null)
            content_delegate.willDisplayContent((PHPublisherContentRequest) request, new com.playhaven.src.publishersdk.content.PHContent(content));
    }

    @Override
    public void onDisplayedContent(PHContentRequest request, PHContent content) {
        if (content_delegate != null)
            content_delegate.didDisplayContent((PHPublisherContentRequest) request, new com.playhaven.src.publishersdk.content.PHContent(content));
    }

    @Override
    public void onDismissedContent(PHContentRequest request, PHContentRequest.PHDismissType type) {

        if (content_delegate != null)
            content_delegate.didDismissContent((PHPublisherContentRequest) request, EnumConversion.convertToOldDismiss(type));
    }

    @Override
    public void onFailedToDisplayContent(PHContentRequest request, PHError error) {
        // if the failure listener is set, we prefer it.
        // Otherwise, we use the content listener.
        // since the difference between contentDidFail and didFail are not clear
        // we call both.)
        if (failure_delegate != null) {
            failure_delegate.didFail((PHPublisherContentRequest) request, error.getMessage());
            failure_delegate.contentDidFail((PHPublisherContentRequest) request, new Exception(error.getMessage()));

        } else if(request_delegate != null)

            request_delegate.requestFailed((PHPublisherContentRequest) request, new Exception(error.getMessage()));

        else if (content_delegate != null)

            content_delegate.requestFailed((PHPublisherContentRequest) request, new Exception(error.getMessage()));
    }

    @Override
    public void onNoContent(PHContentRequest request) {
        if (content_delegate != null)
            content_delegate.didDismissContent((PHPublisherContentRequest) request, PHPublisherContentRequest.PHDismissType.NoContentTriggered);
    }
}
