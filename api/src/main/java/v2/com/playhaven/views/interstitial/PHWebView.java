package v2.com.playhaven.views.interstitial;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import v2.com.playhaven.cache.PHCache;
import v2.com.playhaven.configuration.PHConfiguration;
import v2.com.playhaven.interstitial.webview.PHWebViewChrome;
import v2.com.playhaven.interstitial.webview.PHWebViewClient;
import v2.com.playhaven.model.PHContent;
import v2.com.playhaven.utils.PHStringUtil;

/**
 * Custom web view that simply encapsulates the many custom settings we need.
 *
 * It also handles its own caching.
 */
public class PHWebView extends WebView {



    /** A simple flag that determines whether or not we should
     * enable the webview cache.
     */
    private boolean doCache;

    /** Our handle to the SDK configuration data */
    private PHConfiguration config;

    /** A reference to the content we are displaying */
    private PHContent content;

    /**
     * A constructor that sets all of the different properties
     * of this webview explicitly. You should only call this
     * constructor. The number of parameters we must set are numerous
     * because we have to support so many different kinds of platforms.
     * @param context A valid context
     * @param doCache A flag indicating whether or not the webview should cache
     */
    public PHWebView(Context context, boolean doCache, PHWebViewClient client, PHWebViewChrome chrome, PHContent content) {
        super(context);

        this.doCache        =        doCache;

        this.content        =        content;

        config              = new PHConfiguration();

        String cachePath    = getContext().getApplicationContext().getCacheDir().getAbsolutePath();

        ////////////////////////////////
        ////// Handle caching //////////
        if (doCache) {

            getSettings()   .setCacheMode(WebSettings.LOAD_NO_CACHE);

        } else {

            getSettings()	.setCacheMode			(   WebSettings.LOAD_DEFAULT   );

            getSettings()	.setAppCacheMaxSize 	(   config.getPrecacheSize()   );

            getSettings()	.setAppCachePath		(   cachePath      );

            getSettings()	.setAllowFileAccess 	(   true           );

            getSettings()	.setAppCacheEnabled  	(   true           );

            getSettings()	.setDomStorageEnabled	(   true           );

            getSettings()	.setDatabaseEnabled		(   true           );
        }

        //////////////////////////////////////////////////
        ////// Other settings to ensure consistency //////

        // Enables the mobile WebKit <viewport> tag
        getSettings()       .setUseWideViewPort(true);

        // the user should be able to zoom
        getSettings()       .setSupportZoom(true);

        // set the initial zoom to zero
        getSettings()       .setLoadWithOverviewMode(true);

        // ensure javascript is enabled
        getSettings()       .setJavaScriptEnabled(true);


        // strange hack that fixes the scale
        setInitialScale(0);
        // remove annoying scroll bar padding
        setScrollBarStyle   (   View.SCROLLBARS_INSIDE_OVERLAY  );

        setBackgroundColor  (   Color.TRANSPARENT               );

        /////////////////////////////////////////////////
        ////////////// Set the custom handlers //////////

        setWebViewClient    (   client  );

        setWebChromeClient  (   chrome  );
    }



    /** Simple method to cleanup our dependencies */
    public void cleanup() {
        setWebChromeClient(null);
        setWebViewClient(null);

        stopLoading();
    }



    /**
     * Initiates the load of the content template.
     * It attempts to load the content from the cache
     * before loading it remotely.
     */
    public void loadContentTemplate() {
        stopLoading();

        if ( ! content.url.toString().startsWith("http://")) return;

        try {

            String cached_url = null;

            PHStringUtil.log("Should we access the cache: " + doCache + "....and has it been installed: " + PHCache.hasBeenInstalled());

            // try to find a cached copy
            if (doCache && PHCache.hasBeenInstalled())
                cached_url = PHCache.getSharedCache().getCachedFile(content.url.toString());

            PHStringUtil.log("the cached template url returned: " + cached_url      );

            PHStringUtil.log("the original template url: " + content.url.toString() );

            // load the cached copy if exists, otherwise just
            // load the regular URL
            if (cached_url != null)
                loadUrl(cached_url);
            else
                loadUrl(content.url.toString());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
