package v2.com.playhaven.views.badge;

import android.content.Context;
import android.util.DisplayMetrics;
import org.json.JSONObject;

import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.NinePatchDrawable;

import v2.com.playhaven.resources.types.PHNinePatchResource;
import v2.com.playhaven.resources.PHResourceManager;
import v2.com.playhaven.utils.PHConversionUtils;

/** Represents a renderer for drawing a badge.*/
public class PHBadgeRenderer extends AbstractBadgeRenderer {

    /** The background badge image of the badge */
	private NinePatchDrawable badgeImage;
	
	private final float TEXT_SIZE = 17.0f;
		
	private final float TEXT_SIZE_REDUCE = 8.0f;

	private Paint whitePaint;


	////////////// Badge Renderer Methods //////////
	////////////////////////////////////////////////

    public void loadResources(Context context, Resources res) {
        PHNinePatchResource ninePatchRes = (PHNinePatchResource)PHResourceManager.sharedResourceManager().getResource("badge_image");

        DisplayMetrics dm 	= context.getResources().getDisplayMetrics();

        // we are using the density *class* not the actual ratio here
        badgeImage = ninePatchRes.loadNinePatchDrawable(res, dm.densityDpi);
        badgeImage.setFilterBitmap(true);
    }

	@Override
	public void draw(Context context, Canvas canvas, JSONObject notificationData) {
		int value = requestedValue(notificationData);
		if(value == 0) return;
		
		Rect size = size(context, notificationData);
		badgeImage.setBounds(size);
		
		badgeImage.draw(canvas);
		
		canvas.drawText(Integer.toString(value), PHConversionUtils.dipToPixels(context, 10.0f),
                                                 PHConversionUtils.dipToPixels(context, 17.0f),
                                                 getTextPaint(context));
		
	}
	
	private Paint getTextPaint(Context context) {
		if (whitePaint == null) {
			whitePaint = new Paint();
			whitePaint.setStyle(Paint.Style.FILL);
			whitePaint.setAntiAlias(true);
			whitePaint.setTextSize(PHConversionUtils.dipToPixels(context, TEXT_SIZE));
			whitePaint.setColor(Color.WHITE);
		}
		return whitePaint;
	}
	
	/** Grabs the actual value to display in this notification. If the "value" key doesn't exist, we return 0.*/
	private int requestedValue(JSONObject notificationData) {
		if(notificationData == null ||
		   notificationData.isNull("value")) return 0;
			
		return notificationData.optInt("value", -1);
	}
	
	@Override
	public Rect size(Context context, JSONObject data) {
		//the width and 
		float width = badgeImage.getMinimumWidth(); // unlike the iOS SDK, we must start with baseline width to ensure image scales correctly.
		float height = badgeImage.getMinimumHeight();
		
		int value = requestedValue(data);
		if(value == 0) return new Rect(0,0,0,0);
		
		float valueWidth = getTextPaint(context).measureText(String.valueOf(requestedValue(data)));
		width = (width + valueWidth) - PHConversionUtils.dipToPixels(context, TEXT_SIZE_REDUCE);
		
		return new Rect(0, 0, (int)width, (int)height);
	}

}
