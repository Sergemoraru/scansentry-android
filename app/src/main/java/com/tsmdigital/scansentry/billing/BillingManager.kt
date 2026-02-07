package com.tsmdigital.scansentry.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class BillingManager private constructor(context: Context) : PurchasesUpdatedListener {

    companion object {
        private const val SUBSCRIPTION_ID = "scansentry_pro"
        private const val BASE_PLAN_MONTHLY = "monthly"
        private const val BASE_PLAN_YEARLY = "yearly"

        @Volatile private var INSTANCE: BillingManager? = null

        fun get(context: Context): BillingManager {
            return INSTANCE ?: synchronized(this) {
                val mgr = BillingManager(context.applicationContext)
                INSTANCE = mgr
                mgr
            }
        }
    }

    private val _isPro = MutableStateFlow(false)
    val isPro: StateFlow<Boolean> = _isPro

    private val _productDetails = MutableStateFlow<Map<String, ProductDetails>>(emptyMap())
    val productDetails: StateFlow<Map<String, ProductDetails>> = _productDetails

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val billingClient: BillingClient = BillingClient.newBuilder(context.applicationContext)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build()
        )
        .build()

    fun startConnection() {
        if (billingClient.isReady) {
            queryProducts()
            queryActivePurchases()
            return
        }
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    queryProducts()
                    queryActivePurchases()
                } else {
                    _error.value = "Billing setup failed: ${result.debugMessage}"
                }
            }

            override fun onBillingServiceDisconnected() {
                // Will retry next time startConnection is called
            }
        })
    }

    private fun queryProducts() {
        val products = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(SUBSCRIPTION_ID)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(products)
            .build()

        billingClient.queryProductDetailsAsync(params) { result, detailsList ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                _error.value = "Failed to load products: ${result.debugMessage}"
                return@queryProductDetailsAsync
            }
            _productDetails.value = detailsList.associateBy { it.productId }
        }
    }

    fun launchPurchase(activity: Activity, basePlanId: String) {
        _error.value = null

        val details = _productDetails.value[SUBSCRIPTION_ID]
        if (details == null) {
            _error.value = "Product not loaded"
            return
        }

        val offerToken = selectOfferToken(details, basePlanId)
        if (offerToken == null) {
            _error.value = "No offer found for $basePlanId"
            return
        }

        val productParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(details)
            .setOfferToken(offerToken)
            .build()

        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productParams))
            .build()

        val result = billingClient.launchBillingFlow(activity, flowParams)
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            _error.value = "Purchase flow failed: ${result.debugMessage}"
        }
    }

    fun restorePurchases() {
        queryActivePurchases()
    }

    private fun queryActivePurchases() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()

        billingClient.queryPurchasesAsync(params) { result, purchases ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                _error.value = "Failed to restore purchases: ${result.debugMessage}"
                return@queryPurchasesAsync
            }
            handlePurchases(purchases)
        }
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                if (!purchases.isNullOrEmpty()) handlePurchases(purchases)
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                // no-op
            }
            else -> {
                _error.value = "Purchase error: ${result.debugMessage}"
            }
        }
    }

    private fun handlePurchases(purchases: List<Purchase>) {
        var pro = false

        purchases.forEach { purchase ->
            if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                pro = pro || purchase.products.contains(SUBSCRIPTION_ID)
                if (!purchase.isAcknowledged) {
                    acknowledge(purchase)
                }
            }
        }

        _isPro.value = pro
    }

    private fun acknowledge(purchase: Purchase) {
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()

        billingClient.acknowledgePurchase(params) { result ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                _error.value = "Acknowledge failed: ${result.debugMessage}"
            }
        }
    }

    private fun selectOfferToken(details: ProductDetails, basePlanId: String): String? {
        val offers = details.subscriptionOfferDetails ?: return null
        // Find an offer whose basePlanId matches. If none match, fall back to first.
        val matching = offers.firstOrNull { it.basePlanId == basePlanId }
        return (matching ?: offers.firstOrNull())?.offerToken
    }

    fun priceText(basePlanId: String): String? {
        val details = _productDetails.value[SUBSCRIPTION_ID] ?: return null
        val offers = details.subscriptionOfferDetails ?: return null
        val offer = offers.firstOrNull { it.basePlanId == basePlanId } ?: offers.firstOrNull() ?: return null
        val phases = offer.pricingPhases.pricingPhaseList
        val first = phases.firstOrNull() ?: return null
        return first.formattedPrice
    }

    fun basePlansAvailable(): Set<String> {
        val details = _productDetails.value[SUBSCRIPTION_ID] ?: return emptySet()
        val offers = details.subscriptionOfferDetails ?: return emptySet()
        return offers.map { it.basePlanId }.toSet()
    }

    fun subscriptionId(): String = SUBSCRIPTION_ID
    fun basePlanMonthly(): String = BASE_PLAN_MONTHLY
    fun basePlanYearly(): String = BASE_PLAN_YEARLY
}
