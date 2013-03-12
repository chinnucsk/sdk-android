package com.playhaven.src.publishersdk.content;

import android.net.Uri;
import org.json.JSONObject;

/**
 * A simple facade for the real PHContent class.
 */
public class PHContent extends v2.com.playhaven.model.PHContent {

    /** simple wrapper constructor for converting a new PHContent into an old one */
    public PHContent(v2.com.playhaven.model.PHContent content) {
        this.transition         = content.transition;
        this.closeURL           = content.closeURL;
        this.context            = content.context;
        this.url                = content.url;
        this.closeButtonDelay   = content.closeButtonDelay;
        this.preloaded          = content.preloaded;
        this.setFrames          (content.getFrames());
    }
}
