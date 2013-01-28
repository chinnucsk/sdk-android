package com.playhaven.src.common;

import android.content.Context;

/**
 * A facade for {@link v2.com.playhaven.requests.open.PHSession}. Since
 * it consists of mostly static method, this class is more of an adapter.
 * The static methods will be inherited.
 */
public class PHSession extends v2.com.playhaven.requests.open.PHSession {
    private PHSession(Context context) {
        super(context);
    }
}
