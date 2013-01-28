package v2.com.playhaven.views.badge;

import android.content.Context;
import android.content.res.Resources;
import org.json.JSONObject;

import android.graphics.Canvas;
import android.graphics.Rect;

/** Simple interface for drawing the actual notification.  You must return the size
 * of the notification as well as do the actual drawing. The width varies depending
 * on the value determined by the value returned from the server. The server response can
 * pick the render by setting the "type" parameter.*/
public abstract class AbstractBadgeRenderer {
	/** Actually draws the notification to the canvas..*/
	public abstract void draw(Context context, Canvas canvas, JSONObject notificationData);
	/** returns the rectangle (size) in which we'll draw the rect.*/
	public abstract Rect size(Context context, JSONObject data);
    /** Loads the background image */
    public abstract void loadResources(Context context, Resources res);
}
