package billing

import android.content.Context
import android.util.Log
import com.android.billingclient.api.Purchase
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.functions.Consumer

/**
 * Created by berezkin on 01/07/17.
 */
private val TAG = BillingModel::class.java.simpleName

open class BillingModel(private val context: Context) {

    private val billingObservable = Observable.create<List<Purchase>> { emitter ->
        Log.d(TAG, "subscribed")
        billingManager = BillingManager(context, Consumer {
            if (purchasesCache != it) {
                purchasesCache = it
                emitter.onNext(it)
            }
        })
        emitter.setCancellable{
            Log.d(TAG, "unsubscribed")
            billingManager?.destroy()
            billingManager = null
        }
    }.share()

    val purchases = billingObservable.startWith(Observable.create { em ->
        purchasesCache?.let { em.onNext(it) }
        em.onComplete()
    })

    var billingManager: BillingManager? = null
    private var purchasesCache: List<Purchase>? = null
}
