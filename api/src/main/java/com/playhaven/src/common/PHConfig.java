package com.playhaven.src.common;

import v2.com.playhaven.configuration.Version;
import android.content.Context;

/**
 * Adapter for the newly revised {@link v2.com.playhaven.configuration.PHConfiguration} class.
 */
public class PHConfig {
    /** a shim layer for the token. The adapters for the requests will
     * pull these into the appropriate parent class via explicit setters.
     */
    public static String token;

    /** a shim layer for the secret. The adapters for the requests will
     * pull these into the appropriate parent class via explicit setters.
     */
    public static String secret;

    public static String api = "";

    public static String sdk_version = Version.PROJECT_VERSION;

    public static boolean precache = true;

    public static boolean runningTests;
    
	/** 
	 * Does nothing, was needed to build Unity wrapper. 02/25/2013 
	 */
	public static void cacheDeviceInfo(Context context) { }
}
