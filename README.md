# billing-android
RxJava wrapper for Android Play Billing Library
## Usage
Make sure that your root project build.gradle file has this section

    ext {
        compileSdkVersion = 28
        androidBillingLibraryVersion = ‘1.2.2’
        rxJavaVersion = ‘2.2.3’
    }

Extend [BillingManager](src/main/java/billing/BillingManager.kt) with your payment requests

    fun BillingManager.startBuyingCoins(activity: Activity) {
        initiatePurchaseFlow(activity, "buy_coins", BillingClient.SkuType.INAPP)
    }

Instantiate [BillingModel](src/main/java/billing/BillingModel.kt) once as singleton or via Dagger2.
In every Android component where you are going to use billing, add the next lines:

    @Inject
    BillingModel billingModel;
    private Disposable billingSubscription;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ...
        billingSubscription = billingModel.purchases.subscribe(purchases -> {
            //  Here our purchases come
        });
        ...
    }
    
    @Override
    public void onDestroy() {
        ...
        billingSubscription.dispose();
        super.onDestroy();
    }

To perform a purchaise, just add

    BillingManager.startBuyingCoins(this)
