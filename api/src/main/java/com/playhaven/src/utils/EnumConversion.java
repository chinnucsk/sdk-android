package com.playhaven.src.utils;

import v2.com.playhaven.requests.content.PHContentRequest;
import v2.com.playhaven.views.interstitial.PHCloseButton;
import com.playhaven.src.publishersdk.content.PHContentView;
import com.playhaven.src.publishersdk.content.PHPublisherContentRequest;
import com.playhaven.src.publishersdk.content.PHPurchase;
import com.playhaven.src.publishersdk.purchases.PHPublisherIAPTrackingRequest;

/**
 * Simple utility class for converting between enums. Basically this is an
 * isomorphism.
 */
public class EnumConversion {
    /** For internal use when converting between dismiss types. */
    public static PHPublisherContentRequest.PHDismissType convertToOldDismiss(PHContentRequest.PHDismissType type) {
    	if (type == null) return null;
    	
        switch(type) {
            case AdSelfDismiss:
                return PHPublisherContentRequest.PHDismissType.ContentUnitTriggered;
            case CloseButton:
                return PHPublisherContentRequest.PHDismissType.CloseButtonTriggered;
            case ApplicationBackgrounded:
                return PHPublisherContentRequest.PHDismissType.ApplicationTriggered;
        }

        return null;
    }

    /** Converts between an old billing resolution and a new one*/
    public static v2.com.playhaven.model.PHPurchase.AndroidBillingResult convertToNewBillingResult(PHPurchase.Resolution resolution) {
    	if (resolution == null) return null;
    	
        switch (resolution) {
            case Buy:
                return v2.com.playhaven.model.PHPurchase.AndroidBillingResult.Bought;
            case Cancel:
                return v2.com.playhaven.model.PHPurchase.AndroidBillingResult.Cancelled;
            case Error:
                return v2.com.playhaven.model.PHPurchase.AndroidBillingResult.Failed;
        }

        return null;
    }

    /** converts from a new billing resolution to an old one */
    public static PHPurchase.Resolution convertToOldBillingResult(v2.com.playhaven.model.PHPurchase.AndroidBillingResult resolution) {
    	if (resolution == null) return null;
    	
        switch (resolution) {
            case Bought:
                return PHPurchase.Resolution.Buy;
            case Cancelled:
                return PHPurchase.Resolution.Cancel;
            case Failed:
                return PHPurchase.Resolution.Error;
        }

        return null;
    }

    /** Converts an old {@link com.playhaven.src.publishersdk.content.PHContentView.ButtonState}
     * to {@link v2.com.playhaven.views.interstitial.PHCloseButton.CloseButtonState}.*/
    public static PHCloseButton.CloseButtonState convertToNewButtonState(PHContentView.ButtonState state) {
    	if (state == null) return null;
    	
        switch(state) {
            case Up:
                return PHCloseButton.CloseButtonState.Up;
            case Down:
                return PHCloseButton.CloseButtonState.Down;
        }

        return null;
    }

    /** Converts from an old {@link com.playhaven.src.publishersdk.purchases.PHPublisherIAPTrackingRequest.PHPurchaseOrigin}
     * to a new {@link v2.com.playhaven.model.PHPurchase.PHMarketplaceOrigin}.
     */
    public static v2.com.playhaven.model.PHPurchase.PHMarketplaceOrigin convertToNewOrigin(PHPublisherIAPTrackingRequest.PHPurchaseOrigin origin) {
    	if (origin == null) return null;
    	
        switch(origin) {
            case Google:
                return v2.com.playhaven.model.PHPurchase.PHMarketplaceOrigin.Google;
            case Amazon:
                return v2.com.playhaven.model.PHPurchase.PHMarketplaceOrigin.Amazon;
            case Motorola:
               return v2.com.playhaven.model.PHPurchase.PHMarketplaceOrigin.Motorola;
            case Paypal:
                return v2.com.playhaven.model.PHPurchase.PHMarketplaceOrigin.Paypal;
            case Crossmo:
                return v2.com.playhaven.model.PHPurchase.PHMarketplaceOrigin.Crossmo;
        }

        return null;
    }

    /** Converts from a new PHMarketplaceOrigin to PHPurchaseOrigin*/
    public static PHPublisherIAPTrackingRequest.PHPurchaseOrigin convertToOldOrigin(v2.com.playhaven.model.PHPurchase.PHMarketplaceOrigin origin) {
    	if (origin == null) return null;
    	
    	switch(origin) {
            case Google:
                return PHPublisherIAPTrackingRequest.PHPurchaseOrigin.Google;
            case Amazon:
                return PHPublisherIAPTrackingRequest.PHPurchaseOrigin.Amazon;
            case Motorola:
                return PHPublisherIAPTrackingRequest.PHPurchaseOrigin.Motorola;
            case Paypal:
                return PHPublisherIAPTrackingRequest.PHPurchaseOrigin.Paypal;
            case Crossmo:
                return PHPublisherIAPTrackingRequest.PHPurchaseOrigin.Crossmo;
        }

        return null;
    }
}
