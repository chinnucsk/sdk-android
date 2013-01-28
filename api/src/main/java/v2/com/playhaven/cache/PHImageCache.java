package v2.com.playhaven.cache;

import com.playhaven.src.utils.PHStringUtil;
import v2.com.playhaven.model.PHContent;
import v2.com.playhaven.requests.open.PHPrefetchTask;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.LinkedList;

/**
 * Simple class layered on top of the {@link PHCache} which caches images in a {@link PHContent}.
 * This class searches through an entire API response for "cachable" images.
 * This is also a singleton and should only be used <em>after</em> the
 * PHCache has been installed. A slight note: we have broken much of the functionality into
 * many separate helper methods to avoid one horrific code chunk.
 * @author Sam Stewart
 * TODO: We need a better method for handling things such as a caching "icon_url"s. All we currently cache are the large "image". From the JSON responses from the server.
 */
public class PHImageCache {

    private static PHImageCache sharedImageCache;

    private static String NO_NAME = "<no name>";

    private static String IMAGE_KEY = "image";

    private static String URL_KEY = "url";

    private static String PORTRAIT_KEY = "PH_PORTRAIT";

    private static String LANDSCAPE_KEY = "PH_LANDSCAPE";

    /** Simple container object for breadth-first traversal of the
     * JSON object. This container contains <em>either</em> a JSON
     * object *or* a JSON array. You can also provide a "name" or key for this
     * node which the parent of this node uses to reference it in the original
     * "json" tree.
     */
    private static class JSONNode {

        public JSONArray array;

        public JSONObject object;

        public String name;

        public JSONNode(JSONObject object) {
            this.object = object;
        }

        public JSONNode(JSONArray array) {
            this.array = array;
        }

        public JSONNode(JSONObject object, String name) {
            this.object = object;
            this.name = name;
        }

        public JSONNode(JSONArray array, String name) {
            this.array = array;
            this.name = name;
        }


        /** Determines whether this node is a json array*/
        public boolean isArray() {
            return (array != null);
        }

        /** Determine whether this node is a json object*/
        public boolean isObject() {
            return (object != null);
        }

        public boolean hasName() {
            return (name != null);
        }

        public boolean nameIs(String name) {
            return (hasName() && name.equals(name));
        }

        /** Return the number of "sub nodes" */
        public int children() {
            if (isObject()) return object.length();

            if (isArray()) return array.length();

            return -1;
        }

    }

    public static PHImageCache getSharedCache() {
        if (PHCache.hasBeenInstalled() && sharedImageCache == null)
            sharedImageCache = new PHImageCache();

        return sharedImageCache;
    }

    public static boolean hasBeenInstalled() {
        return PHCache.hasBeenInstalled();
    }

    /**
     * Caches the images specified in this api response and
     * points the urls to the local file system copy.
     * @param content A valid interstitial response from the server
     * @return The same PHContent object but with the image paths pointing
     * at the local cached copies.
     */
    public PHContent cacheImages(PHContent content) {
        PHStringUtil.log("Caching images");

        cacheAllImagesInContent(content.context);

        // we return the interstitial directly because
        // the caching is "in place" in the interstitial.context JSON
        return content;
    }


    /**
     * Helper method that checks the cache for the given url. If it
     * doesn't find the entry in the cache, it kicks off a prefetch task.
     * @param url The url of the resource we wish to cache
     * @return The new local url if it exists in the cache or the original
     * url if it <em>doesn't</em> already exist in the cache (and is now being cached).
     */
    private String cacheResource(String url) {
        if (url == null || PHCache.hasNotBeenInstalled()) return url;


        // check to see if we have a cached version
        String local_url = PHCache.getSharedCache().getCachedFile(url);

        PHStringUtil.log("Checking for image url in cache: " + url + " and finding local URL: " + local_url);

        // we have a cached version! (yay)
        if (local_url != null) return local_url;


        // since we don't have a cached version, kick off a new cache
        // request and return the original url

        // kick off a new pre-cache request
        PHStringUtil.log("Starting new cache request for image: " + url);

        PHPrefetchTask task = new PHPrefetchTask();
        task.setURL(url);
        task.execute();

        return url;
    }

    /** Helper method that caches an "image" entry.
     * We break this code into its own method because
     * we will eventually support different kinds of entries.
     * It replaces the image urls "in place".
     * @param imageEntry The JSON object which represents a standard "image" entry.
     */
    private void cacheImageEntry(JSONObject imageEntry) {
        JSONObject portrait  = imageEntry.optJSONObject(PORTRAIT_KEY);
        JSONObject landscape = imageEntry.optJSONObject(LANDSCAPE_KEY);


        try {
            // get the urls for both images and cache them
            if (portrait != null)
                // simultaneously cache the old resource and update to the new url
                portrait.putOpt(URL_KEY,  cacheResource(portrait.optString(URL_KEY, null)));

            if (landscape != null)
                // simultaneously cache the old resource and update to the new url
                landscape.putOpt(URL_KEY, cacheResource(landscape.optString(URL_KEY, null)));

        } catch (JSONException e) {
            // Do nothing, just skip.
        }
    }
    /**
     * Helper method that decides if a given node is "cachable" (image node).
     * It starts by checking to see if a cached version of the resource already exists.
     * If not, it kicks off a cache request, and updates the underlying JSON code.
     * @param node the node we are trying to cache
     */
    private void convertNodeToCached(JSONNode node) {
        // is it an "image" object?
        if (node.isObject() && node.nameIs("image")) {
            cacheImageEntry(node.object);
        }

    }


    /** Helper method to enqueue a new node's <em>children</em>
     * into a given queue.
     * This lives in a separate method to ensure code readability.
     * We assume the front of the queue is the last element
     * and the back of the queue is the first element
     * @param node The node to enqueue
     * @param queue The queue to insert the node into
     */
    private void enqueueNodeChildren(JSONNode node, LinkedList<JSONNode> queue) {
        if (node == null || node.children() == 0)
            return;


        if (node.isArray()) {
            JSONArray array = node.array;

            // if it's an array we enqueue the children individually
            for (int i = 0; i < array.length(); ++ i) {

                // decide if each child is an object or another array
                JSONObject childObject = array.optJSONObject(i);

                // if the child is *not* a JSONObject then it will be null.
                if (childObject != null) {
                    // of course it has no name because it's just an element in array
                    queue.addFirst(new JSONNode(childObject, NO_NAME));
                    continue;
                }

                // let's try a JSONArray?
                JSONArray childArray = array.optJSONArray(i);

                if (childArray != null) {
                    // of course it has no name because it's merely an element in an array
                    queue.addFirst(new JSONNode(childArray, NO_NAME));
                    continue;
                }

                // if the child is neither a JSON object or a JSON array, ignore it.
            }
            return;
        }

        if (node.isObject()) {
            JSONObject object = node.object;

            // enqueue all the children (array's or objects)
            Iterator<String> keys = object.keys();

            while (keys.hasNext()) {
                String key = keys.next();

                // is it an object?
                JSONObject cur_obj = object.optJSONObject(key);

                if (cur_obj != null) {
                    // since it's an object, just enqueue it with its key as its name
                    queue.addFirst(new JSONNode(cur_obj, key));
                    continue;
                }

                JSONArray cur_array = object.optJSONArray(key);

                // is it an array
                if (cur_array != null) {
                    // enqueue the array with its key as its name
                    queue.addFirst(new JSONNode(cur_array, key));
                    continue;
                }
            }
        }
    }

    /**
     * Uses simple bread-first traversal to find all image urls and convert them
     * to cached images. We treat the JSON interstitial as a tree and skip all primitives
     * such as longs, ints, and strings and instead look for "image object".
     * The first time this method is run, no images exist
     * so we will download all images and leave the urls unchanged. The second time,
     * we'll redirect the images. Unfortunately, this results in the images being downloaded
     * once by us and then again by the webview the first time. This is unavoidable
     * until we can control the caching better.
     * @param contentJSON The interstitial whose images we will cache
     * @todo Handle "icon_url" entries
     */
    private void cacheAllImagesInContent(JSONObject contentJSON) {

        // we might have hit an empty leaf
        if (contentJSON == null || contentJSON.length() == 0)
            return;

        // the queue used for breadth first traversal
        // the first element is the "back" and the last element is the "front"
        LinkedList<JSONNode> queue = new LinkedList<JSONNode>();

        // enqueue the "root node" (of course this node has no name)
        queue.addFirst(new JSONNode(contentJSON, NO_NAME));

        // continue traversing the "tree" until no nodes are left
        while (queue.size() > 0) {

            // dequeue the current node and start working with it
            JSONNode cur_node = queue.removeLast();

            // check to see if this node is cachable (is it an image node?)
            convertNodeToCached(cur_node);

            // now enqueue the node's children
            // we will do this until we explore the entire JSON "tree"
            enqueueNodeChildren(cur_node, queue);
        }

    }
}