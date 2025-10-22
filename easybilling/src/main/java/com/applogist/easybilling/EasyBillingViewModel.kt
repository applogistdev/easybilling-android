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

    var billingListener: EasyBillingListener?=null

    private lateinit var playStoreBillingClient: BillingClient

    fun init() {
        playStoreBillingClient = BillingClient.newBuilder(getApplication())
            .setListener { billingResult, purchases ->
                billingListener?.onPurchasesUpdated(billingResult, purchases)
            }
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder()
                    .enableOneTimeProducts()
                    .build()
            )
            .build()


        playStoreBillingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingServiceDisconnected() {
                Log.d("BillingViewModel", "onBillingServiceDisconnected")
                billingListener?.onBillingServiceDisconnected()
            }

            override fun onBillingSetupFinished(billingResult: BillingResult) {
                when (billingResult.responseCode) {
                    BillingClient.BillingResponseCode.OK -> {
                        billingListener?.onBillingInitialized()
                        getPurchases(
                            QueryPurchasesParams.newBuilder()
                                .setProductType(BillingClient.ProductType.INAPP)
                                .build()
                        ) { p0, p1 ->
                            billingListener?.onInAppPurchases(p1)
                        }
                        getPurchases(
                            QueryPurchasesParams.newBuilder()
                                .setProductType(BillingClient.ProductType.SUBS)
                                .build()
                        ) { p0, p1 ->
                            billingListener?.onSubsPurchases(p1)
                        }
                    }
                    else -> {
                        //do nothing. Someone else will connect it through retry policy.
                        //May choose to send to server though
                        billingListener?.onBillingInitializedError(billingResult.responseCode)
                    }
                }
            }
        })
    }

    /**
     * Checks if the user's device supports subscriptions
     */
    fun isSubscriptionSupported(): Boolean {
        val billingResult =
            playStoreBillingClient.isFeatureSupported(BillingClient.FeatureType.SUBSCRIPTIONS)
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
    fun testPurchased(
        activity: Activity,
        @BillingClient.ProductType productType: String = BillingClient.ProductType.INAPP
    ) {
        purchaseWithQuery(activity, "android.test.purchased", productType)
    }

    /**
     * When you make an Google Play Billing request with this product ID
     * Google Play responds as though the purchase was canceled.
     * This can occur when an error is encountered in the order process,
     * such as an invalid credit card, or when you cancel a user's order before it is charged.
     */
    fun testPurchaseCanceled(
        activity: Activity,
        @BillingClient.ProductType productType: String = BillingClient.ProductType.INAPP
    ) {
        purchaseWithQuery(activity, "android.test.canceled", productType)
    }

    /**
     * When you make an Google Play Billing request with this product ID,
     * Google Play responds as though the item being purchased
     * was not listed in your application's product list.
     */
    fun testPurchaseItemUnavailable(
        activity: Activity,
        @BillingClient.ProductType productType: String = BillingClient.ProductType.INAPP
    ) {
        purchaseWithQuery(activity, "android.test.item_unavailable", productType)
    }

    /**
     * Get purchases details for all the items bought within your app. This method uses a cache of
     * Google Play Store app without initiating a network request.
     */
    fun getPurchases(
        queryPurchasesParams: QueryPurchasesParams = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build(),
        listener: PurchasesResponseListener
    ) {
        playStoreBillingClient.queryPurchasesAsync(queryPurchasesParams, listener)
    }

    /**
     * Perform a network query to get Product details and return the result asynchronously.
     */
    fun getProductDetails(
        productList: List<QueryProductDetailsParams.Product>,
        productDetailsResponseListener: ProductDetailsResponseListener
    ) {
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()
        playStoreBillingClient.queryProductDetailsAsync(params) { billingResult, skuDetailsList ->
            productDetailsResponseListener.onProductDetailsResponse(billingResult, skuDetailsList)
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
    fun purchaseWithQuery(
        activity: Activity,
        productId: String,
        @BillingClient.ProductType productType: String = BillingClient.ProductType.INAPP,
        developerPayload: String? = null
    ) {

        val product = QueryProductDetailsParams.Product.newBuilder()
            .setProductId(productId)
            .setProductType(productType)
            .build()

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(listOf(product))
            .build()
        playStoreBillingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            when (billingResult.responseCode) {
                BillingClient.BillingResponseCode.OK -> {
                    // Eğer parametre nullable ise: productDetailsList?.isNotEmpty() kullan
                    if (productDetailsList != null && productDetailsList.productDetailsList.isNotEmpty()) {
                        // İlk ProductDetails'i al
                        val pd = productDetailsList.productDetailsList.first() // veya firstOrNull() ile güvenle al
                        val offerToken = pd.subscriptionOfferDetails?.firstOrNull()?.offerToken ?: ""
                        val productDetailsParamsList =
                            listOf(
                                BillingFlowParams.ProductDetailsParams.newBuilder()
                                    .setProductDetails(pd)
                                    .setOfferToken(offerToken)
                                    .build()
                            )
                        val purchaseParams = BillingFlowParams.newBuilder()
                            .setProductDetailsParamsList(productDetailsParamsList)
                        if (!developerPayload.isNullOrEmpty()) {
                            purchaseParams.setObfuscatedAccountId(developerPayload)
                        }
                        playStoreBillingClient.launchBillingFlow(activity, purchaseParams.build())
                    } else {
                        Log.e("BillingViewModel", "Product details list empty")
                    }
                }
                else -> {
                    Log.e("BillingViewModel", billingResult.debugMessage)
                }
            }
        }

    }

    fun purchase(
        activity: Activity,
        productDetails: ProductDetails,
        developerPayload: String? = null
    ) {
        val offerToken = productDetails.subscriptionOfferDetails?.firstOrNull()?.offerToken ?: ""
        val productDetailsParamsList =
            listOf(
                BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(productDetails)
                    .setOfferToken(offerToken)
                    .build()
            )
        val purchaseParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
        if (!developerPayload.isNullOrEmpty()) {
            purchaseParams.setObfuscatedAccountId(developerPayload)
        }
        playStoreBillingClient.launchBillingFlow(activity, purchaseParams.build())
    }

    /*
     * Consumes a given in-app product. Consuming can only be done on an item that's owned, and as a
     * result of consumption, the user will no longer own it.
     */
    fun consumePurchase(purchaseToken: String) {
        val consumeParams = ConsumeParams.newBuilder().setPurchaseToken(purchaseToken)
        playStoreBillingClient.consumeAsync(consumeParams.build()) { _billingResult, _purchaseToken ->
            billingListener?.onPurchaseConsumed(_billingResult, _purchaseToken)
        }
    }

    /*
     * Consumes a test in-app product. Consuming can only be done on an item that's owned, and as a
     * result of consumption, the user will no longer own it.
     */
    fun consumeTestPurchase(packageName: String) {
        val purchaseToken = "inapp:$packageName:android.test.purchased"
        val consumeParams = ConsumeParams.newBuilder().setPurchaseToken(purchaseToken).build()
        playStoreBillingClient.consumeAsync(consumeParams) { _billingResult, _purchaseToken ->
            billingListener?.onPurchaseConsumed(_billingResult, _purchaseToken)
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
    fun acknowledgePurchase(
        purchaseToken: String,
        listener: AcknowledgePurchaseResponseListener
    ) {
        val params = AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchaseToken)
        playStoreBillingClient.acknowledgePurchase(params.build(), listener)
    }

    override fun onCleared() {
        super.onCleared()
        Log.d("BillingViewModel", "onCleared")
        playStoreBillingClient.endConnection()
    }
}