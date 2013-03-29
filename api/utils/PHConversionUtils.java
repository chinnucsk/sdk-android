package v2.com.playhaven.utils;

import v2.com.playhaven.configuration.PHConfiguration;

public class PHConversionUtils {
	
	public static float dipToPixels(float pixels) {
		return pixels * PHConfiguration.screen_density;
	}
}
