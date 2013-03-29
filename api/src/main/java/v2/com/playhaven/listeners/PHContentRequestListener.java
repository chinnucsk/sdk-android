package v2.com.playhaven.listeners;

import v2.com.playhaven.model.PHError;
import v2.com.playhaven.requests.content.PHContentRequest;
import v2.com.playhaven.requests.content.PHContentRequest.PHDismissType;
import v2.com.playhaven.model.PHContent;

/**
 * Interface that publishers should implement when making a interstitial request.
 * @author samstewart
 *
 */
public interface PHContentRequestListener {
	public void onSentContentRequest	 (PHContentRequest request					   );
	public void onReceivedContent		 (PHContentRequest request, PHContent content );
	public void onWillDisplayContent 	 (PHContentRequest request, PHContent content );
	public void onDisplayedContent 		 (PHContentRequest request, PHContent content );
	public void onDismissedContent 		 (PHContentRequest request, PHDismissType type);
	public void onFailedToDisplayContent (PHContentRequest request, PHError error);
	public void onNoContent				 (PHContentRequest request);
}
