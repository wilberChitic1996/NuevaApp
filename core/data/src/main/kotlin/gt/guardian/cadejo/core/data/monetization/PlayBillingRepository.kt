package gt.guardian.cadejo.core.data.monetization

import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import dagger.hilt.android.qualifiers.ApplicationContext
import gt.guardian.cadejo.domain.monetization.BillingRepository
import gt.guardian.cadejo.domain.monetization.Product
import gt.guardian.cadejo.domain.monetization.ProductIds
import gt.guardian.cadejo.domain.progress.ProgressRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Play Billing (v7) wrapper. Queries product details, launches purchases, and
 * applies entitlements (acknowledge non-consumables, consume coin packs). The
 * "remove ads" purchase flips the profile's adsRemoved flag via the progress
 * repository. Designed for a future server-side receipt check: today it verifies
 * with the official client library and acknowledges — the natural place to add a
 * backend token check later.
 */
@Singleton
class PlayBillingRepository @Inject constructor(
    @ApplicationContext context: Context,
    private val progressRepository: ProgressRepository,
    private val activityHolder: CurrentActivityHolder,
) : BillingRepository,
    PurchasesUpdatedListener {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _products = MutableStateFlow<List<Product>>(emptyList())
    override val products: Flow<List<Product>> = _products.asStateFlow()

    private val _owned = MutableStateFlow<Set<String>>(emptySet())
    override val ownedProductIds: Flow<Set<String>> = _owned.asStateFlow()

    private val productDetails = mutableMapOf<String, ProductDetails>()

    private val client: BillingClient =
        BillingClient
            .newBuilder(context)
            .setListener(this)
            .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
            .build()

    private val managedIds = listOf(ProductIds.REMOVE_ADS, ProductIds.COIN_PACK_SMALL)

    init {
        connect()
    }

    private fun connect() {
        client.startConnection(
            object : BillingClientStateListener {
                override fun onBillingSetupFinished(result: BillingResult) {
                    if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                        scope.launch {
                            queryProducts()
                            restorePurchases()
                        }
                    }
                }

                override fun onBillingServiceDisconnected() {
                    // Left to the next call to reconnect; a production app would back off and retry.
                }
            },
        )
    }

    private suspend fun queryProducts() {
        val params =
            QueryProductDetailsParams
                .newBuilder()
                .setProductList(
                    managedIds.map { id ->
                        QueryProductDetailsParams.Product
                            .newBuilder()
                            .setProductId(id)
                            .setProductType(BillingClient.ProductType.INAPP)
                            .build()
                    },
                ).build()

        val result = client.queryProductDetails(params)
        val details = result.productDetailsList.orEmpty()
        details.forEach { productDetails[it.productId] = it }
        _products.value =
            details.map { pd ->
                Product(
                    id = pd.productId,
                    title = pd.name,
                    formattedPrice = pd.oneTimePurchaseOfferDetails?.formattedPrice ?: "",
                )
            }
    }

    override suspend fun purchase(productId: String): Boolean {
        // Purchases must be launched from the foreground Activity. The actual
        // entitlement is applied asynchronously in onPurchasesUpdated; this returns
        // whether the flow launched successfully.
        val details = productDetails[productId] ?: return false
        val activity = activityHolder.current ?: return false
        val params =
            BillingFlowParams
                .newBuilder()
                .setProductDetailsParamsList(
                    listOf(
                        BillingFlowParams.ProductDetailsParams
                            .newBuilder()
                            .setProductDetails(details)
                            .build(),
                    ),
                ).build()
        val result = client.launchBillingFlow(activity, params)
        return result.responseCode == BillingClient.BillingResponseCode.OK
    }

    override suspend fun restorePurchases() {
        val result =
            client.queryPurchasesAsync(
                QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build(),
            )
        result.purchasesList.forEach { handlePurchase(it) }
    }

    override fun onPurchasesUpdated(
        result: BillingResult,
        purchases: MutableList<Purchase>?,
    ) {
        if (result.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            purchases.forEach { handlePurchase(it) }
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) return
        scope.launch {
            when {
                ProductIds.REMOVE_ADS in purchase.products -> {
                    if (!purchase.isAcknowledged) {
                        client.acknowledgePurchase(
                            AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build(),
                        ) {}
                    }
                    _owned.value = _owned.value + ProductIds.REMOVE_ADS
                    progressRepository.setAdsRemoved(true)
                }
                ProductIds.COIN_PACK_SMALL in purchase.products -> {
                    client.consumeAsync(
                        ConsumeParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build(),
                    ) { _, _ -> }
                    progressRepository.addCoins(COIN_PACK_AMOUNT)
                }
            }
        }
    }

    private companion object {
        const val COIN_PACK_AMOUNT = 1_000L
    }
}
