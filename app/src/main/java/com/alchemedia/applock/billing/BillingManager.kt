package com.alchemedia.applock.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.alchemedia.applock.BuildConfig
import com.alchemedia.applock.data.SettingsRepository
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** What the Pro screen needs to know about the store. */
data class BillingUiState(
    /** Price as formatted by Google Play for the user's country, e.g. "£4.99"; null until loaded. */
    val priceText: String? = null,
    val storeReady: Boolean = false,
    val purchasing: Boolean = false,
    val message: BillingMessage? = null,
)

enum class BillingMessage { Unavailable, Cancelled, Pending, Failed, Restored, NothingToRestore }

/**
 * One-time "AppLock Pro" purchase through Google Play Billing. The result is cached in settings so
 * Pro keeps working offline; it is re-checked with Play every time the app opens.
 *
 * Client-side checks can be bypassed on a rooted device. For stronger protection, verify purchase
 * tokens on a server with the Google Play Developer API.
 */
class BillingManager(
    context: Context,
    private val settings: SettingsRepository,
) : PurchasesUpdatedListener {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val client: BillingClient = BillingClient.newBuilder(context.applicationContext)
        .setListener(this)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .enableAutoServiceReconnection()
        .build()

    private val _state = MutableStateFlow(BillingUiState())
    val state: StateFlow<BillingUiState> = _state.asStateFlow()

    private var productDetails: ProductDetails? = null

    /** Connects to Google Play (safe to call repeatedly), loads the price and re-checks ownership. */
    fun start() {
        if (client.isReady) {
            scope.launch { refresh() }
            return
        }
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    scope.launch { refresh() }
                } else {
                    Log.w(TAG, "Billing setup failed: ${result.debugMessage}")
                    _state.update { it.copy(storeReady = false) }
                }
            }

            override fun onBillingServiceDisconnected() {
                _state.update { it.copy(storeReady = false) }
            }
        })
    }

    fun clearMessage() = _state.update { it.copy(message = null) }

    /** Opens Google Play's purchase sheet. */
    fun purchase(activity: Activity) {
        val details = productDetails
        if (details == null || !client.isReady) {
            _state.update { it.copy(message = BillingMessage.Unavailable) }
            return
        }
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(details).build()),
            )
            .build()
        _state.update { it.copy(purchasing = true, message = null) }
        val result = client.launchBillingFlow(activity, params)
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            _state.update { it.copy(purchasing = false, message = BillingMessage.Failed) }
        }
    }

    /** Re-reads the user's purchases from Google Play (for a new phone or after reinstalling). */
    fun restore() {
        if (!client.isReady) {
            _state.update { it.copy(message = BillingMessage.Unavailable) }
            return
        }
        scope.launch {
            val owned = checkOwnership()
            _state.update {
                it.copy(message = if (owned) BillingMessage.Restored else BillingMessage.NothingToRestore)
            }
        }
    }

    /** Debug builds only: flips Pro on so the photo feature can be tried without a Play listing. */
    fun debugUnlock() {
        if (!BuildConfig.DEBUG) return
        scope.launch { settings.setProUnlocked(true) }
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: List<Purchase>?) {
        _state.update { it.copy(purchasing = false) }
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> scope.launch { purchases.orEmpty().forEach { handle(it) } }
            BillingClient.BillingResponseCode.USER_CANCELED -> _state.update { it.copy(message = BillingMessage.Cancelled) }
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> scope.launch { checkOwnership() }
            else -> _state.update { it.copy(message = BillingMessage.Failed) }
        }
    }

    private suspend fun refresh() {
        loadProductDetails()
        checkOwnership()
    }

    private suspend fun loadProductDetails() {
        val product = QueryProductDetailsParams.Product.newBuilder()
            .setProductId(PRO_PRODUCT_ID)
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        val result = client.queryProductDetails(
            QueryProductDetailsParams.newBuilder().setProductList(listOf(product)).build(),
        )
        productDetails = result.productDetailsList?.firstOrNull()
        _state.update {
            it.copy(
                storeReady = productDetails != null,
                priceText = productDetails?.oneTimePurchaseOfferDetails?.formattedPrice,
            )
        }
    }

    /** True if Google Play says this account owns Pro; also updates the cached entitlement. */
    private suspend fun checkOwnership(): Boolean {
        val result = client.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build(),
        )
        if (result.billingResult.responseCode != BillingClient.BillingResponseCode.OK) return false
        val owned = result.purchasesList.any { it.products.contains(PRO_PRODUCT_ID) && it.purchaseState == Purchase.PurchaseState.PURCHASED }
        result.purchasesList.forEach { handle(it) }
        // Debug builds keep a manual unlock, so don't let an empty Play query wipe it.
        if (owned || !BuildConfig.DEBUG) settings.setProUnlocked(owned)
        return owned
    }

    private suspend fun handle(purchase: Purchase) {
        if (!purchase.products.contains(PRO_PRODUCT_ID)) return
        when (purchase.purchaseState) {
            Purchase.PurchaseState.PURCHASED -> {
                settings.setProUnlocked(true)
                // Unacknowledged purchases are refunded by Google after three days.
                if (!purchase.isAcknowledged) {
                    client.acknowledgePurchase(AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build())
                }
            }
            Purchase.PurchaseState.PENDING -> _state.update { it.copy(message = BillingMessage.Pending) }
            else -> Unit
        }
    }

    companion object {
        /** Create this one-time (managed) product in Play Console and price it at £4.99. */
        const val PRO_PRODUCT_ID = "applock_pro"
        private const val TAG = "BillingManager"
    }
}
