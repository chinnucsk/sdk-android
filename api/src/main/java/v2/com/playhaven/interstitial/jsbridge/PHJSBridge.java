package v2.com.playhaven.interstitial.jsbridge;

import java.lang.ref.WeakReference;
import java.util.Hashtable;

import android.net.Uri;
import android.webkit.WebView;
import v2.com.playhaven.interstitial.jsbridge.handlers.AbstractHandler;
import v2.com.playhaven.utils.PHStringUtil;
import org.json.JSONObject;

/**
 * <p>
 * Lightweight manager that handles routing urls from the webview
 * callbacks to native subclasses of {@link AbstractHandler}.
 * </p>
 *
 * <p>
 *  You should call {@link PHJSBridge#addRoute(String, v2.com.playhaven.interstitial.jsbridge.handlers.AbstractHandler)} to add a new route. The route
 *  should match the base url portion (slug) without query variables.
 *  @author samstewart
 * </p>
 */
public class PHJSBridge {

    /** the template used for sending messages to the webview */
    private static final String JAVASCRIPT_CALLBACK_TEMPLATE 	  = "javascript:PlayHaven.nativeAPI.callback(\"%s\", %s, %s)";

    /** the current URL we are examining */
	private Uri mCurUrl;


    /** a map of url routes to handlers */
	private Hashtable<String, AbstractHandler> routers = new Hashtable<String, AbstractHandler>();

    /**
     * A reference to the webview. We do not create
     * the webview but merely reference it.
     */
    private WebView webview;

    /** A reference to the content displayer which enables
     * the handlers to influence the state of the content displayer
     */
    protected WeakReference<ManipulatableContentDisplayer> contentDisplayer;

    public PHJSBridge(ManipulatableContentDisplayer contentDisplayer) {
        this.contentDisplayer = new WeakReference<ManipulatableContentDisplayer>(contentDisplayer);
    }

    /**
     * Attaches the webview for which we are handling routes.
     * @param webview the webview
     */
    public void attachWebview(WebView webview) {
        this.webview = webview;
    }

    /**
     * Determines whether or not the webview has been attached
     * via {@link #attachWebview(android.webkit.WebView)}.
     * @return true if we have a valid webview reference, false otherwise
     */
    public boolean hasWebviewAttached() {
        return (this.webview != null);
    }

    /** loads the specified url with the attached webview.
     *
     * @param url the url to load.
     */
    public void loadUrlInWebView(String url) {

        if (this.webview != null)
            this.webview.loadUrl(url);
    }

    /** sends an explicit javascript command to the webview */
    public void runJavascript(String js) {

        if (this.webview != null)
            this.webview.loadUrl("javascript:" + js);

    }

    /** gets the current url we are examining */
	public String getCurrentURL() {
		synchronized (PHJSBridge.class) {
			return (mCurUrl != null 	? 
					mCurUrl.toString()  : 
					null);
		}
	}

    /** Gets a query variable from the url
     * we're parsing.
     * @param name the name of the query variable
     * @return the value of the query parameter if exists, null otherwise.
     */
	public String getCurrentQueryVar(String name) {
		synchronized (PHJSBridge.class) {
			if (mCurUrl == null) return null;
			
			String param = mCurUrl.getQueryParameter(name);
			
			if (param == null || param.equals("") || param.equals("null")) return null;
			
			return param;
		}
	}

    /** Adds a route and the corresponding handler.
     * @param route the slug to match against.
     * @param handler the handler used for handling the slug.
     */
	public void addRoute(String route, AbstractHandler handler) {
        handler.attachBridge(this);
        handler.attachContentDisplayer(contentDisplayer.get());

		routers.put(route, handler);
	}

    /**
     * Removes the query parameters from the url and returns
     * just the slug.
     * @param url the url off of which to strip the query parameters.
     * @return the slug of the url
     */
	private String stripQuery(String url) {
		return url.substring(0, (url.indexOf("?") > 0 ? 
				 				 url.indexOf("?")     : 
				 				 url.length())
							);
	}

    /** Checks to see if we have a route for the given url.
     * @param url the url we are checking to see if a route exists.
    */
	public boolean hasRoute(String url) {
        PHStringUtil.log("Asking about route: " + url);
		return routers.containsKey(stripQuery(url));
	}
	
	/**
	 * Actually parses and "routes" the given url to the appropriate handler.
	 * @param url Must be a valid URL and can contain query variables. 
	 */
	public void route(String url) {
		synchronized (this) {
			mCurUrl = Uri.parse(url);
			
			if (mCurUrl == null) return;
			
			String stripped = stripQuery(url);
			
			AbstractHandler handler = routers.get(stripped);
			
			if (handler != null)
                handler.handle();;
		}

	}

    /**
     * Sends a message to the content templates in the webview.
     * @param callback the message we wish to send to the webview content template.
     * @param payload the additional data of the message.
     * @param error the error (if any) we wish to send to the content templates.
     */
    public void sendMessageToWebview(String callback, JSONObject payload, JSONObject error) {
        if ( ! hasWebviewAttached()) return;

        String callbackCommand = String.format(
                JAVASCRIPT_CALLBACK_TEMPLATE,
                (callback != null ? callback : "null"),
                (payload != null ? payload.toString() : "null"),
                (error != null ? error.toString() : "null")
        );

        PHStringUtil.log("sending javascript callback to WebView: '" + callbackCommand);
        webview.loadUrl(callbackCommand);
    }

    /**
     * We clear the current URL.
     */
	public void clearCurrentURL() {
		mCurUrl = null;
	}

    /**
     * We remove all existing routes between slugs and handlers.
     */
	public void clearRoutes() {
		routers.clear();
	}
}
