package v2.com.playhaven.views.badge;

import java.util.HashMap;

import v2.com.playhaven.listeners.PHBadgeRequestListener;
import v2.com.playhaven.requests.badge.PHBadgeRequest;
import org.json.JSONObject;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.view.View;

import v2.com.playhaven.requests.crashreport.PHCrashReport;
import v2.com.playhaven.model.PHError;
import v2.com.playhaven.utils.PHStringUtil;

/** This class is a generic view which manages a dictionary of notification renderers and handles the actual rendering.
 * It uses the java reflection api to allow you add classes extending PHNotificationRender so that you can pick which
 * renderer you want. This also allows the interstitial templates to pick which render they want by setting the "type"
 * parameter in the notification data.
 * This notification view is also attached to a
 *
 * TODO: overkill to have the dynamically configurable badge renderers.
 * @author samuelstewart
 *
 */
@SuppressWarnings("rawtypes")
public class PHBadgeView extends View implements PHBadgeRequestListener {

	private static HashMap<String, Class> renderMap = new HashMap<String, Class>();
	
	private AbstractBadgeRenderer notificationRenderer;
	
	private JSONObject notificationData;
	
	public PHBadgeRequest request;
	
	private String placement;
	
	
	public PHBadgeView(Context context, String placement) {
		super(context);
		this.placement = placement;
	}
	
	///////////////////////////////////////////////////////////
	
	public String getPlacement() {
		return placement;
	}
	
	public PHBadgeRequest getRequest() {
		return request;
	}
	
	public JSONObject getNotificationData() {
		return notificationData;
	}
	
	public AbstractBadgeRenderer getNotificationRenderer() {
		return notificationRenderer;
	}
	
	public static HashMap<String, Class> getRenderMap() {
		return renderMap;
	}
	
	///////////////////////////////////////////////////////////
	public void refresh() {
		request = new PHBadgeRequest(this, placement);
		
		request.send(getContext());
	}
	
	public void clear() {
		this.request = null;
		this.notificationData = null;
	}

	/** Should call this method to update the view, renderer, etc when notification data changes. */
	public void updateBadgeData(JSONObject data) {
		if (data == null) return;
		
		try {
		this.notificationData = data;
		
		//create the renderer based on the server response..
		notificationRenderer = createBadgeRenderer(data);
		
		//re-layout and redraw..
		requestLayout();
		
		invalidate();
		} catch (Exception e) { // swallow all exceptions
			PHCrashReport.reportCrash(e, "PHBadgeView - updateBadgeData", PHCrashReport.Urgency.critical);
		}
	}
	
	//////////////////////////////////////////////////////////////////////////////
	///////////////////////////// Rendering Method ///////////////////////////////
	
	/** Creates a renderer based on the "type" field of the server response. Returns null if no renderer exists.*/
	public AbstractBadgeRenderer createBadgeRenderer(JSONObject data) {
		if (renderMap.size() == 0) PHBadgeView.initRenderers();
		
		String type = data.optString("type", "badge");
		
		AbstractBadgeRenderer renderer = null;
		try {
			Class render_class = renderMap.get(type);
			renderer = (AbstractBadgeRenderer)render_class.newInstance();

            // now load the resources
            renderer.loadResources(getContext(), getContext().getResources());

		} catch (Exception e) {
			PHCrashReport.reportCrash(e, "PHBadgeView - createBadgeRenderer", PHCrashReport.Urgency.critical);
			renderer = null;
		}
		
		PHStringUtil.log("Created subclass of PHNotificationRenderer of type: " + type);
		return renderer;
		
	}
	/** Adds a renderer class to the map. We use reflection to ensure it extends the PHNotificationRender.*/
	public static void setBadgeRenderer(Class renderer, String type) {
		
		Class superclass = renderer.getSuperclass();
		
		if(superclass != AbstractBadgeRenderer.class)
			throw new IllegalArgumentException("Cannot create a new renderer of type " + type + " because it does not implement the PHNotificationRenderer interface");
		
		renderMap.put(type, renderer);
	}
	
	public static void initRenderers() {
		renderMap.put("badge", PHBadgeRenderer.class);
	}
	
	////////////////////////////////////////////////////////////////////////
	/////////////////////////////// View override methods //////////////////
	
	@Override
	protected void onDraw(Canvas canvas) {
		try {
		if (notificationRenderer == null) return;
		
		notificationRenderer.draw(getContext(), canvas, notificationData);
		} catch (Exception e) { // swallow all errors
			PHCrashReport.reportCrash(e, "PHBadgeView - onDraw", PHCrashReport.Urgency.critical);
		}
	}
	
	@Override
	protected void onMeasure(int widthSpec, int heightSpec) {
		try {
		Rect size = new Rect(0, 0, widthSpec, heightSpec);
		if(notificationRenderer != null)
			size = notificationRenderer.size(getContext(), notificationData);
		
		this.setMeasuredDimension(size.width(), size.height());
		} catch (Exception e) { // swallow all errors
			PHCrashReport.reportCrash(e, "PHBadgeView - onDraw", PHCrashReport.Urgency.critical);
		}
		
		return;
	}
	
	//////////////////////////////////////////////////
	/////////// Request Listener ////////////////////


    @Override
    public void onBadgeRequestSucceeded(PHBadgeRequest request, JSONObject responseData) {
        this.request = null; // request is done

        JSONObject notification = responseData.optJSONObject("notification");

        if ( ! JSONObject.NULL.equals(notification) &&
                notification.length() > 0)
            updateBadgeData(notification);
    }

    @Override
    public void onBadgeRequestFailed(PHBadgeRequest request, PHError error) {
        this.request = null; // request is done
        updateBadgeData(null);
    }
}
