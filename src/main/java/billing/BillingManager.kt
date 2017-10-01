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

class BillingManager(context: Context, val purchaseConsumer: Consumer<List<Purchase>>) : PurchasesUpdatedListener {
    private val billingClient: BillingClient = BillingClient.newBuilder(context).setListener(this).build()
    private var isServiceConnected = false

    init {
        startServiceConnection(Runnable {
            queryPurchases()
        })
    }

    override fun onPurchasesUpdated(responseCode: Int, purchases: MutableList<Purchase>?) {
        if (responseCode == BillingClient.BillingResponse.OK && purchases != null) {
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
            billingClient.consumeAsync(purchaseToken) { purchaseToken_, resultCode ->
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

            override fun onBillingSetupFinished(resultCode: Int) {
                if (resultCode == BillingClient.BillingResponse.OK) {
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
            val purchasesResult = billingClient.queryPurchases(BillingClient.SkuType.INAPP)
            Log.d(TAG, "Querying purchases elapsed time: ${System.currentTimeMillis() - time} ms")
            onPurchasesUpdated(purchasesResult.responseCode, purchasesResult.purchasesList)
        })
    }

    fun initiatePurchaseFlow(activity: Activity, skuId: String, @BillingClient.SkuType billingType: String) {
        val purchaseFlowRequest = Runnable {
            Log.d(TAG, "Launching in-app purchase flow")
            val purchaseParams = BillingFlowParams.newBuilder()
                    .setSku(skuId).setType(billingType).build()
            billingClient.launchBillingFlow(activity, purchaseParams)
        }
        executeServiceRequest(purchaseFlowRequest)
    }
}
