package v2.com.playhaven.interstitial;

public class PHContentEnums {

    /** The various errors */
    public static enum Error {

        NoBoundingBox("The interstitial you requested was not able to be shown because it is missing required orientation data."),
        CouldNotLoadURL("Ad was unable to load URL"),
        FailedSubrequest("Sub-request started from ad unit failed"),
        NoResponseField("No 'response' field in JSON resposne");

        private String error;

        public String getMessage() {
            return error;
        }

        private Error(String message) {
            this.error = message;
        }
    }

	/** The arguments for starting {@link PHInterstitialActivity}.*/
	public static enum IntentArgument {
		CustomCloseBtn	("custom_close"),
		Content			("init_content_contentview"),
		Tag				("content_tag");

		private String key;

		public String getKey() {
			return key;
		}

		private IntentArgument(String key) {
			this.key = key;
		}
	}

	public static enum Reward {
		IDKey			("reward"),
		QuantityKey		("quantity"),
		ReceiptKey		("receipt"),
		SignatureKey	("signature");

		private final String keyName;

		private Reward(String key) {
			this.keyName = key; // one time assignment
		}

		public String key() {
			return keyName;
		}
	};

	public static enum Purchase {
		ProductIDKey	("product"),
		NameKey			("name"),
		ReceiptKey		("receipt"),
		SignatureKey	("signature"),
		CookieKey		("cookie");

		private final String keyName;

		private Purchase(String key) {
			this.keyName = key; //one time assignment
		}

		public String key() {
			return keyName;
		}
	};






}
