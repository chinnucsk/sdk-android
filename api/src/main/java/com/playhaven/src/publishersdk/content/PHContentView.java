package com.playhaven.src.publishersdk.content;

import v2.com.playhaven.interstitial.PHInterstitialActivity;

/**
 * Simple container class for enums. This class used to be the main interstitial view
 * but has now been replaced with {@link v2.com.playhaven.interstitial.PHInterstitialActivity}.
 */
public class PHContentView extends PHInterstitialActivity {

    /** determines whether or not we should display a light
     * black overlay. Currently does nothing.
     */
    public boolean showsOverlayImmediately;

    /** the various button states for the custom close button images.
     */
    public static enum ButtonState {
        Down(android.R.attr.state_pressed),
        Up(android.R.attr.state_enabled);

        private int android_state;

        private ButtonState(int android_state) {
            this.android_state = android_state;
        }

        public int getAndroidState() {
            return this.android_state;
        }

    };
}
