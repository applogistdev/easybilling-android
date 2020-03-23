package com.applogist.easybilling

import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase

/*
*  Created by Mustafa Ürgüplüoğlu on 23.03.2020.
*  Copyright © 2020 Mustafa Ürgüplüoğlu. All rights reserved.
*/

interface EasyBillingListener {
    fun onBillingInitialized()
    fun onBillingInitializedError(errorCode: Int)
    /**
     * This method is called by the [playStoreBillingClient] when new purchases are detected.
     * The purchase list in this method is not the same as the one in
     * [queryPurchases][BillingClient.queryPurchases]. Whereas queryPurchases returns everything
     * this user owns, [onPurchasesUpdated] only returns the items that were just now purchased or
     * billed.
     *
     * The purchases provided here should be passed along to the secure server for
     * [verification](https://developer.android.com/google/play/billing/billing_library_overview#Verify)
     * and safekeeping. And if this purchase is consumable, it should be consumed, and the secure
     * server should be told of the consumption. All that is accomplished by calling
     * [queryPurchasesAsync].
     */
    fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?)

    fun onBillingServiceDisconnected() {}
    fun onInAppPurchases(purchases: List<Purchase>) {}
    fun onSubsPurchases(purchases: List<Purchase>) {}
    fun onPurchaseConsumed(billingResult: BillingResult, purchaseToken: String) {}
}