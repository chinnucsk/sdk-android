package v2.com.playhaven.cache;

import android.content.Context;
import com.playhaven.src.utils.PHStringUtil;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.zip.GZIPInputStream;

/**
 * Simple disk-based cache for caching resources from HttpRequests. PHPrefetchTaskMakes extensive use of it.
 * This class basically provides a mapping between URLs and FS data.
 * Note that this class is not thread safe so users must be careful.
 *
 * We need some way to handle cache refresh? Some sort of cache expiry?
 *
 * We need some way of handling an automatic cleanup?
 *
 * Users must call {@link PHCache#installCache(android.content.Context)} before
 * calling {@link v2.com.playhaven.cache.PHCache#getSharedCache()} to initiate the Singleton instance.
 *
 * This class simulates a special caching directory for the
 * Some other possibilities at the PHAsyncRequest level:
 * http://pivotallabs.com/users/tyler/blog/articles/1754-android-image-caching
 * Or overriding the {@link android.webkit.WebViewClient#shouldInterceptRequest(android.webkit.WebView, String)}
 * but this is only available in API Level 11.
 * @author samstewart
 *
 */
public class PHCache {

    /**
     * The file URL prefix.
     */
    private final String FILE_PREFIX = "file://";

    /**
     * Subdirectory of the Application cache for PlayHaven use
     */
    private static final String CACHE_DIR = "playhaven.cache";

    /**
     * Singleton instance of PHCache
     */
    private static PHCache sharedCache;

    /**
     * Size of buffer when copying into the cache
     */
    private static final Integer BUFFER_SIZE = 1024;

    /**
     * The location of the cache directory.
     */
    private File cacheDirectory;

    /** In memory mapping of urls to file names. If the mapping doens't exist
     * in this hash map, we check the file system directly. This merely serves
     * as a small performance boost.
     */
    private HashMap<URL, File> cachedMapping = new HashMap<URL, File>();

    public static PHCache getSharedCache() {
        return sharedCache;
    }

    /** Simple method for injecting a testing cache.
     * @param testing_cache The testing cache we should be using. Must be
     *                      a subclass of {@link PHCache}.
     */
    public static void useTestingCache(PHCache testing_cache) {
        sharedCache = testing_cache;
    }

    /**
     * Initializes the cache by creating a new directory.
     * @param context
     */
    public static void installCache(Context context) {
        File dir = new File(context.getCacheDir() + File.separator + CACHE_DIR);

        // if the cache directory doesn't exist, create it
        if (! dir.exists())
            dir.mkdir();

        sharedCache = new PHCache(dir);
    }

    /** Clients should pass in a cache directory */
    public PHCache(File cacheDirectory) {
        this.cacheDirectory = cacheDirectory;
    }

    /** gets the location of the cache */
    public File getRootCacheDirectory() {
        return this.cacheDirectory;
    }

    /** Checks whether or not the shared cache instance has been initialized */
    public static boolean hasBeenInstalled() {
        return (sharedCache != null);
    }

    /** Checks to see whether or not the shared cache has been initialized */
    public static boolean hasNotBeenInstalled() {
        return !hasBeenInstalled();
    }

    /**
     * Converts a url to a file name by stripping out illegal characters
     * and appending the cache directory. This method is only used
     * internally but we make it public for unit testing.
     * @param url The URL key we wish to convert to the file name
     * @return A valid file pointer.
    */
    public File convertToFilename(URL url) {
        // replace any slashes which might cause problems in the file name
        String clean_slug = url.toString().replace(File.separator, "_");

        // attach cache directory
        File new_file = new File(sharedCache.getRootCacheDirectory().getAbsolutePath() + File.separator +  clean_slug);

        // we're good to go!
        return new_file;
    }


    /**
     * Cache's the contents of the file described by the input stream.
     * Often comes off of network request.
     * This method can decompress gzip if it was used.
     * We always overwrite any existing data that happen to have the same name.
     * @param requestUrl The url of the request.
     * @param content The input stream of the file
     * @param isCompressed whether or not the interstitial is <em>gzip</em> compressed
     *
     */
    public void cacheFile(URL requestUrl, InputStream content, boolean isCompressed) {
        File outputFile = convertToFilename(requestUrl);

        PHStringUtil.log("Caching url: " + requestUrl + " to local file: " + outputFile);
        try {

            // decide if we need to wrap in decoder in case the interstitial is compressed
            if (isCompressed)
                content = new GZIPInputStream(content);


            // shove bytes from the input stream to the output file
            // we also overwrite any existing data
            BufferedOutputStream cachedFile = new BufferedOutputStream(new FileOutputStream(outputFile, false));

            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;

            while ((bytesRead = content.read(buffer)) != -1) {
                cachedFile.write(buffer, 0, bytesRead);
            }

            cachedFile.flush();
            cachedFile.close();

            content.close();

        } catch (IOException e) {

        }
    }

    /**
     * Gets the cached file corresponding to the request url.
     * @param url The URL of the item we wish to cache.
     * @return A reference to the file if it exists, null otherwise.
     */
    public File getCachedFile(URL url) {
        // first check our in-memory map to see if it exists
        File file = cachedMapping.get(url);

        PHStringUtil.log("Checking cache for URL: " + url);

        if (file != null)
            return file;

        // doesn't exist in the map? Let's check the disk

        // convert the url into a file path
        file = convertToFilename(url);

        PHStringUtil.log("Checking cache for file: " + file);
        if (file.exists()) {

            // note for next time
            cachedMapping.put(url, file);
            return file;
        }


        return null;
    }

    /**
     * Gets the file corresponding to the request url with the
     * file:/// prefix appended. Used by the webview.
     * @param url The URL of the item we wish to retrieve from the cache.
     *            This URL should be the same as the one used in {@link PHCache#getCachedFile(String)}.
     * @return a file URL for the webview
     */
    public String getCachedFile(String url) {
        try {
            File file = getCachedFile(new URL(url));

            // might not exist
            if (file == null) return null;

            // convert to path with 'file://' in front
            return FILE_PREFIX + file.getAbsolutePath();

        } catch (MalformedURLException e) {
            // DO SOMETHING
            return null;
        }

    }
}
