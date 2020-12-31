
## Installation
[![](https://jitpack.io/v/applogistdev/easybilling-android.svg)](https://jitpack.io/#applogistdev/easybilling-android)    
```gradle 
allprojects {  
	allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
}
dependencies {
	implementation 'com.github.applogistdev:easybilling-android:Tag'
}
``` 
## What is Consume and Acknowledge
[https://developer.android.com/google/play/billing/billing_library_overview#acknowledge](https://developer.android.com/google/play/billing/billing_library_overview#acknowledge)
### Consume Example
Candy Crush Buy Candy Bomb $1

Purchase -> onPurchasesUpdated -> consumePurchase

You can buy again
### Acknowledge Example
Candy Crush Remove Ads $5

Purchase -> onPurchasesUpdated -> acknowledgePurchase

You can't buy again

## What is Developer Payload
https://developer.android.com/google/play/billing/billing_library_overview#attach_a_developer_payload

Some example: Developer Payload = User Email Address

Candy Crush Buy Candy Bomb $1 OR Candy Crush Remove Ads $5

Purchase -> onPurchasesUpdated -> consumePurchase(Developer Payload) OR acknowledgePurchase(Developer Payload)
```kotlin
override fun onInAppPurchases(purchases: List<Purchase>) {  
    Log.d(purchases[0].developerPayload)
}
OR
override fun onSubsPurchases(purchases: List<Purchase>) {  
    Log.d(purchases[0].developerPayload)
}
```

## Flow
İnit -> 
- onBillingInitialized() OR onBillingInitializedError()
	- onBillingInitializedError -> Finish
	- onBillingInitialized -> onInAppPurchases() AND onSubsPurchases()
		- onInAppPurchases -> returned all BillingClient.SkuType.INAPP
		- onSubsPurchases -> returned all BillingClient.SkuType.SUBS
- billingViewModel.purchase(Context, PRODUCT_ID(Google Play Store), BillingClient.SkuType.INAPP or BillingClient.SkuType.SUBS) -> onPurchasesUpdated (Only returns the items that were just now purchased or billed.)
- billingViewModel.consumePurchase(Purchase Token) -> onPurchaseConsumed()
- billingViewModel.acknowledgePurchase(Purchase Token, AcknowledgePurchaseResponseListener, Devloper Payload) -> https://developer.android.com/google/play/billing/billing_library_overview#acknowledge

## Usage
```kotlin  
billingViewModel = ViewModelProvider(this).get(EasyBillingViewModel::class.java)  
  
billingViewModel.billingListener = object : EasyBillingListener {  
  
    override fun onBillingInitialized() {  
        Log.d("onBillingInitialized")
    }  
  
    override fun onBillingInitializedError(errorCode: Int) {  
        Log.e("onBillingInitializedError", errorCode)  
    }  
  
    override fun onInAppPurchases(purchases: List<Purchase>) {  
        Log.d("onInAppPurchases")  
    }  
  
    override fun onSubsPurchases(purchases: List<Purchase>) {  
        Log.e("subsPurchases", purchases)  
    }  
  
    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {  
        Log.e("onPurchasesUpdated")
    }  
  
    override fun onPurchaseConsumed(billingResult: BillingResult, purchaseToken: String) {  
		Log.e("onPurchaseConsumed")
    }  
}
```
## Payment
```kotlin
billingViewModel.purchase(Context, IN_APP_PRODUCT_ID)
```
## Test Payments
```kotlin
billingViewModel.testPurchased(Context)

billingViewModel.testPurchaseCanceled(Context)

billingViewModel.testPurchaseItemUnavailable(Context)
```

## Get Items
```kotlin
override fun onBillingInitialized() {   
    billingViewModel.getSkuDetails(  
            arrayListOf(IN_APP_PRODUCT_ID),  
            BillingClient.SkuType.INAPP,  
            SkuDetailsResponseListener { billingResult, skuDetailsList ->  {
	            Log.d("getSkuDetails", skuDetailsList[0].price)
            }
  )  
}
or
if(billingViewModel.isBillingReady()){
    billingViewModel.getSkuDetails(  
            arrayListOf(IN_APP_PRODUCT_ID),  
            BillingClient.SkuType.INAPP,  
            SkuDetailsResponseListener { billingResult, skuDetailsList ->  {
	            Log.d("getSkuDetails", skuDetailsList[0].price)
            }
  )
}
```
