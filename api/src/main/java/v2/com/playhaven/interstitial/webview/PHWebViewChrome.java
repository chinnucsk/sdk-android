package v2.com.playhaven.interstitial.webview;

/**
 *
 */

import android.net.Uri;
import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;
import v2.com.playhaven.requests.crashreport.PHCrashReport;
import v2.com.playhaven.utils.PHStringUtil;

/**
 * Extends WebChromeClient just for logging purposes. (have to if greater
 * than Android 2.1)
 */
public class PHWebViewChrome extends WebChromeClient {

    @Override
    public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
        try {
            String fname = "(no file)";

            if (consoleMessage.sourceId() != null) {
                fname = Uri.parse(consoleMessage.sourceId()).getLastPathSegment();
            }

            PHStringUtil.log("Javascript: " + consoleMessage.message()
                    + " at line (" + fname + ") :"
                    + consoleMessage.lineNumber());

        } catch (Exception e) { // swallow all exceptions
            PHCrashReport.reportCrash(e, "PHWebViewChrome - onConsoleMessage", PHCrashReport.Urgency.low);
        }

        return true;
    }
}
