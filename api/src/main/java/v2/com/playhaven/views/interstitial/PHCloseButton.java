package v2.com.playhaven.views.interstitial;

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.StateListDrawable;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import v2.com.playhaven.resources.PHResourceManager;
import v2.com.playhaven.resources.data.PHCloseActiveImageResource;
import v2.com.playhaven.resources.data.PHCloseImageResource;

/**
 * Represents a single tearDownBridge button with a small X. You can set
 * custom images for the button's states.
 */
public class PHCloseButton extends ImageButton {


    /** the listener all clients must implement */
    private Listener listener = null;

    public static interface Listener {
        public void onClose(PHCloseButton button);
    }

    public static enum CloseButtonState {
        Down    ( android.R.attr.state_pressed ),

        Up      ( android.R.attr.state_enabled );

        private int android_state;

        private CloseButtonState(int android_state) {
            this.android_state = android_state;
        }

        public int getAndroidState() {
            return this.android_state;
        }

    };

    public PHCloseButton(Context context, Listener listener) {

        super                               ( context                    );

        this.setContentDescription          ( "closeButton"              );

        this.setScaleType                   ( ImageView.ScaleType.FIT_XY );

        // make transparent
        this.setBackgroundDrawable          ( null                       );

        loadDefaultStateImages              ();

        this.listener                = listener;

        final Listener finalListener = listener;

        // setup the listener
        setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {

                finalListener.onClose(PHCloseButton.this);

            }

        });
    }

    /**
     * Special constructor which allows the client to customize
     * the up and down states of this tearDownBridge button.
     * @param context a valid context
     * @param customActive The custom image used when the button is clicked.
     * @param customInactive The custom image used when the button is inactive
     */
    public PHCloseButton(Context context, Listener listener, BitmapDrawable customActive, BitmapDrawable customInactive) {

        this(context, listener);

        // override the default images
        setActiveAndInactive(       customActive,   customInactive        );
    }

    /** Loads the default image for the states
     * of this button from the Base64 encoded resources.
     * These can be overridden later through the special constructor.
     */
    private void loadDefaultStateImages() {
        DisplayMetrics dm 	                  = this.getResources().getDisplayMetrics();

        PHResourceManager rm                  = PHResourceManager.sharedResourceManager();

        // load the images from the static classes where they are encoded
        // in base 64
        PHCloseImageResource inactiveRes      = (PHCloseImageResource)       rm.getResource("close_inactive");

        PHCloseActiveImageResource active_res = (PHCloseActiveImageResource) rm.getResource("close_active");

        BitmapDrawable inactive               = new BitmapDrawable(getResources(), inactiveRes.loadImage(dm.densityDpi));

        BitmapDrawable active                 = new BitmapDrawable(getResources(), active_res.loadImage(dm.densityDpi));

        setActiveAndInactive(active, inactive);
    }

    /** Sets the images representing the "clicked" and "unclicked" state */
    private void setActiveAndInactive(BitmapDrawable active, BitmapDrawable inactive) {

        StateListDrawable states = new StateListDrawable();

        states.addState( new int[] { CloseButtonState.Down.getAndroidState() }, active   );

        states.addState( new int[] { CloseButtonState.Up.getAndroidState()   }, inactive );

        this.setImageDrawable(states);
    }
}
