# billing-android
RxJava wrapper for Android Play Billing Library
## Usage
Make sure that your root project build.gradle file has this section

    ext {
        compileSdkVersion = 25
        buildToolsVersion = '25.0.3'
        androidBillingLibraryVersion = 'dp-1'
        rxJavaVersion = '2.1.1'
    }

Extend [BillingManager](src/main/java/billing/BillingManager.kt) with your payment requests

    fun BillingManager.startBuyingCoins(activity: Activity) {
        initiatePurchaseFlow(activity, "buy_coins", BillingClient.SkuType.INAPP)
    }

Instantiate [BillingModel](src/main/java/billing/BillingModel.kt) once as singleton or via Dagger2.
In every Android component where you will use billing, add the next lines:

    @Inject
    AppBillingModel billingModel;
    private Disposable billingSubscription;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ...
        billingSubscription = billingModel.subscribe(purchases -> {
            //  Here our purchases come
        });
        ...
    }
    
    @Override
    public void onDestroy() {
        ...
        billingModel.unsubscribe(billingSubscription);
        super.onDestroy();
    }

To perform a purchaise, just add

    BillingManager.startBuyingCoins(this)
