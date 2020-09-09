package com.applogist.easybilling

import android.app.Activity
import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import com.android.billingclient.api.*

/*
*  Created by Mustafa Ürgüplüoğlu on 23.03.2020.
*  Copyright © 2020 Mustafa Ürgüplüoğlu. All rights reserved.
*/

class EasyBillingViewModel(application: Application) : AndroidViewModel(application) {

    lateinit var billingListener: EasyBillingListener

    private var playStoreBillingClient: BillingClient = BillingClient.newBuilder(application)
        .enablePendingPurchases() // required or app will crash
        .setListener { billingResult, purchases -> billingListener.onPurchasesUpdated(billingResult, purchases) }
        .build()

    init {
        playStoreBillingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingServiceDisconnected() {
                Log.d("BillingViewModel", "onBillingServiceDisconnected")
                billingListener.onBillingServiceDisconnected()
            }

            override fun onBillingSetupFinished(billingResult: BillingResult) {
                when (billingResult.responseCode) {
                    BillingClient.BillingResponseCode.OK -> {
                        billingListener.onBillingInitialized()
                        billingListener.onInAppPurchases(getPurchases(BillingClient.SkuType.INAPP))
                        billingListener.onSubsPurchases(getPurchases(BillingClient.SkuType.SUBS))
                    }
                    else -> {
                        //do nothing. Someone else will connect it through retry policy.
                        //May choose to send to server though
                        billingListener.onBillingInitializedError(billingResult.responseCode)
                    }
                }
            }
        })
    }

    /**
     * Checks if the user's device supports subscriptions
     */
    fun isSubscriptionSupported(): Boolean {
        val billingResult = playStoreBillingClient.isFeatureSupported(BillingClient.FeatureType.SUBSCRIPTIONS)
        return billingResult.responseCode == BillingClient.BillingResponseCode.OK
    }

    /**
     * Checks if the client is currently connected to the service, so that requests to other methods
     * will succeed.
     */
    fun isBillingReady(): Boolean {
        return playStoreBillingClient.isReady
    }

    /**
     * When you make an Google Play Billing request with this product ID,
     * Google Play responds as though you successfully purchased an item.
     * The response includes a JSON string, which contains fake purchase information
     * (for example, a fake order ID).
     */
    fun testPurchased(activity: Activity, @BillingClient.SkuType skuType: String = BillingClient.SkuType.INAPP) {
        purchase(activity, "android.test.purchased", skuType)
    }

    /**
     * When you make an Google Play Billing request with this product ID
     * Google Play responds as though the purchase was canceled.
     * This can occur when an error is encountered in the order process,
     * such as an invalid credit card, or when you cancel a user's order before it is charged.
     */
    fun testPurchaseCanceled(activity: Activity, @BillingClient.SkuType skuType: String = BillingClient.SkuType.INAPP) {
        purchase(activity, "android.test.canceled", skuType)
    }

    /**
     * When you make an Google Play Billing request with this product ID,
     * Google Play responds as though the item being purchased
     * was not listed in your application's product list.
     */
    fun testPurchaseItemUnavailable(activity: Activity, @BillingClient.SkuType skuType: String = BillingClient.SkuType.INAPP) {
        purchase(activity, "android.test.item_unavailable", skuType)
    }

    /**
     * Get purchases details for all the items bought within your app. This method uses a cache of
     * Google Play Store app without initiating a network request.
     */
    fun getPurchases(@BillingClient.SkuType skuType: String = BillingClient.SkuType.INAPP): List<Purchase> {
        val result = playStoreBillingClient.queryPurchases(skuType)
        return result.purchasesList ?: listOf()
    }

    /**
     * Perform a network query to get SKU details and return the result asynchronously.
     */
    fun getSkuDetails(skuList: List<String>, @BillingClient.SkuType skuType: String, skuDetailsResponseListener: SkuDetailsResponseListener) {
        val params = SkuDetailsParams.newBuilder().setSkusList(skuList).setType(skuType).build()
        playStoreBillingClient.querySkuDetailsAsync(params) { billingResult, skuDetailsList ->
            skuDetailsResponseListener.onSkuDetailsResponse(billingResult, skuDetailsList!!)
        }
    }

    /**
     * Initiate the billing flow for an in-app purchase or subscription.
     * @param developerPayload If you pass this value,
     * Google Play can use it to detect irregular activity, such as many devices making purchases
     * on the same account in a short period of time. Do not use this field to store any Personally
     * Identifiable Information (PII) such as emails in cleartext. Attempting to store PII in this
     * field will result in purchases being blocked. Google Play recommends that you use either
     * encryption or a one-way hash to generate an obfuscated identifier to send to Google Play.
     */
    fun purchase(activity: Activity, productId: String, @BillingClient.SkuType skuType: String = BillingClient.SkuType.INAPP, developerPayload : String? = null) {
        val params = SkuDetailsParams.newBuilder().setSkusList(arrayListOf(productId)).setType(skuType).build()
        playStoreBillingClient.querySkuDetailsAsync(params) { billingResult, skuDetailsList ->
            when (billingResult.responseCode) {
                BillingClient.BillingResponseCode.OK -> {
                    if (!skuDetailsList.isNullOrEmpty()) {
                        val purchaseParams = BillingFlowParams.newBuilder().setSkuDetails(skuDetailsList[0])
                        if(!developerPayload.isNullOrEmpty()){
                            purchaseParams.setObfuscatedAccountId(developerPayload)
                        }
                        playStoreBillingClient.launchBillingFlow(activity, purchaseParams.build())
                    }
                }
                else -> {
                    Log.e("BillingViewModel", billingResult.debugMessage)
                }
            }
        }
    }

    /*
     * Consumes a given in-app product. Consuming can only be done on an item that's owned, and as a
     * result of consumption, the user will no longer own it.
     */
    fun consumePurchase(purchaseToken: String) {
        val consumeParams = ConsumeParams.newBuilder().setPurchaseToken(purchaseToken)
        playStoreBillingClient.consumeAsync(consumeParams.build()) { _billingResult, _purchaseToken ->
            billingListener.onPurchaseConsumed(_billingResult, _purchaseToken)
        }
    }

    /*
     * Consumes a test in-app product. Consuming can only be done on an item that's owned, and as a
     * result of consumption, the user will no longer own it.
     */
    fun consumeTestPurchase(packageName : String) {
        val purchaseToken = "inapp:$packageName:android.test.purchased"
        val consumeParams = ConsumeParams.newBuilder().setPurchaseToken(purchaseToken).build()
        playStoreBillingClient.consumeAsync(consumeParams) { _billingResult, _purchaseToken ->
            billingListener.onPurchaseConsumed(_billingResult, _purchaseToken)
        }
    }

    /*
    * The Purchase object now includes an isAcknowledged() method that indicates whether a purchase
    * has been acknowledged. In addition, the Google Play Developer API includes acknowledgement
    * boolean values for both Purchases.products and Purchases.subscriptions.
    * Before acknowledging a purchase, be sure to use these methods to determine
    * if the purchase has already been acknowledged.
    * For products that aren't consumed, use acknowledgePurchase(), found in the client API.
    */
    fun acknowledgePurchase(purchaseToken : String, listener : AcknowledgePurchaseResponseListener, developerPayload : String? = null){
        val params = AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchaseToken)
        playStoreBillingClient.acknowledgePurchase(params.build(), listener)
    }

    override fun onCleared() {
        super.onCleared()
        Log.d("BillingViewModel", "onCleared")
        playStoreBillingClient.endConnection()
    }
}