package v2.com.playhaven.utils;

import android.content.Context;
import android.util.DisplayMetrics;

public class PHConversionUtils {
	
	public static float dipToPixels(Context context, float dips) {
        DisplayMetrics dm 	= context.getResources().getDisplayMetrics();
		return dips * dm.density;
	}
}
