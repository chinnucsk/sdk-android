package com.playhaven.src.common;

import android.content.Context;

import java.lang.ref.WeakReference;

/**
 * Simple facade for {@link v2.com.playhaven.requests.crashreport.PHCrashReport}.
 */
public class PHCrashReport extends v2.com.playhaven.requests.crashreport.PHCrashReport implements PHAPIRequest {
    /** we need a reference to a valid context */
    private WeakReference<Context> context;

    public PHCrashReport() {
        super();
    }

    public PHCrashReport(Exception e, Urgency level) {
        super(e, level); // call default constructor

    }

    public PHCrashReport(Exception e, String tag, Urgency level) {
        super(e, tag, level);
    }

    /////////////////////////////////////////////////
    //////////////// Convenience Creators ///////////
    public static PHCrashReport reportCrash(Exception e, String tag, Urgency level) {
        // no-op

        return null;
    }

    public static PHCrashReport reportCrash(Exception e, Urgency level) {
        // no-op

        return null;
    }

    @Override
    public void setDelegate(Delegate delegate) {
        // no-op
    }

    @Override
    public void send() {
        // no-op
    }
}
