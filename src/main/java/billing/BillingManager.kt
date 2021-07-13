package billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import io.reactivex.functions.Consumer

/**
 * Created by berezkin on 30/06/17.
 */

private val TAG = BillingManager::class.java.simpleName

class BillingManager(context: Context, private val purchaseConsumer: Consumer<List<Purchase>>) : PurchasesUpdatedListener {
    private val billingClient: BillingClient = BillingClient.newBuilder(context).enablePendingPurchases().setListener(this).build()
    private var isServiceConnected = false

    init {
        startServiceConnection(Runnable {
            queryPurchases()
        })
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        if (result.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            purchaseConsumer.accept(purchases)
        }
    }

    fun destroy() {
        if (billingClient.isReady) {
            billingClient.endConnection()
        }
    }

    fun consumeAsync(purchaseToken: String, listener: ConsumeResponseListener) {
        val consumeRequest = Runnable {
            val params = ConsumeParams.newBuilder().setPurchaseToken(purchaseToken).build()
            billingClient.consumeAsync(params) { purchaseToken_, resultCode ->
                Log.d(TAG, "consumeAsync $resultCode")
                listener.onConsumeResponse(purchaseToken_, resultCode)
            }
        }
        executeServiceRequest(consumeRequest)
    }

    private fun startServiceConnection(executeOnSuccess: Runnable?) {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingServiceDisconnected() {
                isServiceConnected = false
            }

            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    isServiceConnected = true
                    executeOnSuccess?.run()
                }
            }
        })
    }

    private fun executeServiceRequest(runnable: Runnable) {
        if (isServiceConnected) {
            runnable.run()
        } else {
            startServiceConnection(runnable)
        }
    }

    private fun queryPurchases() {
        executeServiceRequest(Runnable {
            val time = System.currentTimeMillis()
            billingClient.queryPurchasesAsync(BillingClient.SkuType.INAPP, object : PurchasesResponseListener {
                override fun onQueryPurchasesResponse(
                    purchasesResult: BillingResult,
                    purchasesList: MutableList<Purchase>
                ) {
                    Log.d(TAG, "Querying purchases elapsed time: ${System.currentTimeMillis() - time} ms")
                    onPurchasesUpdated(purchasesResult, purchasesList)
                }
            })
        })
    }

    fun initiatePurchaseFlow(activity: Activity, skuId: String, @BillingClient.SkuType billingType: String) {
        val purchaseFlowRequest = Runnable {
            Log.d(TAG, "Launching in-app purchase flow")
            val skuDetails = SkuDetailsParams.newBuilder().setSkusList(listOf(skuId)).setType(billingType).build()
            billingClient.querySkuDetailsAsync(skuDetails, object : SkuDetailsResponseListener {
                override fun onSkuDetailsResponse(result: BillingResult, details: MutableList<SkuDetails>?) {
                    details?.firstOrNull()?.let {
                        val purchaseParams = BillingFlowParams.newBuilder().setSkuDetails(it).build()
                        billingClient.launchBillingFlow(activity, purchaseParams)
                    }
                }
            })
        }
        executeServiceRequest(purchaseFlowRequest)
    }
}
