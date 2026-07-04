package gt.guardian.cadejo.domain.monetization

import kotlinx.coroutines.flow.Flow

/** Outcome of showing a rewarded ad. */
enum class RewardOutcome { EARNED, DISMISSED, UNAVAILABLE }

/**
 * Rewarded ads only — no interstitials. Used to offer an optional revive or a
 * coin double. Kept Android-free here; the implementation in :core:data grabs the
 * current Activity from a holder, so view models never touch an Activity.
 */
interface RewardedAdService {
    /** Begin loading an ad so it's ready when the player asks. Safe to call repeatedly. */
    fun preload()

    /** Show the ad and suspend until it resolves. Returns [RewardOutcome.UNAVAILABLE] if none is ready. */
    suspend fun showRewarded(): RewardOutcome
}

/** A purchasable product (from Play Billing). */
data class Product(val id: String, val title: String, val formattedPrice: String)

/** Well-known product ids. Must match the Play Console SKUs. */
object ProductIds {
    const val REMOVE_ADS = "remove_ads"
    const val COIN_PACK_SMALL = "coins_small"
}

/**
 * Play Billing wrapper. Exposes owned products and lets the UI launch a purchase.
 * The "remove ads" entitlement, once acknowledged, flips the profile's adsRemoved
 * flag through the progress repository.
 */
interface BillingRepository {
    val products: Flow<List<Product>>
    val ownedProductIds: Flow<Set<String>>

    /** Launch the purchase flow for [productId]; returns true if it completed & was acknowledged. */
    suspend fun purchase(productId: String): Boolean

    /** Re-query and re-apply entitlements (e.g. after reinstall). */
    suspend fun restorePurchases()
}

/**
 * UMP (User Messaging Platform) consent. GDPR/CCPA form must be resolved BEFORE
 * ads initialise. [canRequestAds] reflects whether consent allows ad requests.
 */
interface ConsentManager {
    val canRequestAds: Boolean

    /** Gather consent if required (shows the form when necessary). Idempotent. */
    suspend fun ensureConsent()
}
