package gt.guardian.cadejo.core.data.monetization

import android.content.Context
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import dagger.hilt.android.qualifiers.ApplicationContext
import gt.guardian.cadejo.domain.monetization.ConsentManager
import gt.guardian.cadejo.domain.monetization.RewardOutcome
import gt.guardian.cadejo.domain.monetization.RewardedAdService
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Rewarded ads via AdMob. Uses Google's official TEST ad unit id — replace with
 * your real unit id (from local.properties/BuildConfig) before release. Only loads
 * when UMP consent allows it. Degrades to [RewardOutcome.UNAVAILABLE] rather than
 * ever crashing when no ad or Activity is available.
 */
@Singleton
class AdMobRewardedAdService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val activityHolder: CurrentActivityHolder,
    private val consent: ConsentManager,
) : RewardedAdService {
    private val initialized = AtomicBoolean(false)
    private var ad: RewardedAd? = null
    private var loading = false

    override fun preload() {
        if (!consent.canRequestAds) return
        ensureInitialized()
        if (ad != null || loading) return
        loading = true
        RewardedAd.load(
            context,
            TEST_REWARDED_UNIT_ID,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(loaded: RewardedAd) {
                    ad = loaded
                    loading = false
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Timber.w("Rewarded ad failed to load: %s", error.message)
                    ad = null
                    loading = false
                }
            },
        )
    }

    override suspend fun showRewarded(): RewardOutcome {
        val activity = activityHolder.current ?: return RewardOutcome.UNAVAILABLE
        val current =
            ad ?: run {
                preload()
                return RewardOutcome.UNAVAILABLE
            }

        return suspendCancellableCoroutine { cont ->
            var earned = false
            current.fullScreenContentCallback =
                object : FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        ad = null
                        preload()
                        if (cont.isActive) cont.resume(if (earned) RewardOutcome.EARNED else RewardOutcome.DISMISSED)
                    }

                    override fun onAdFailedToShowFullScreenContent(error: com.google.android.gms.ads.AdError) {
                        ad = null
                        if (cont.isActive) cont.resume(RewardOutcome.UNAVAILABLE)
                    }
                }
            current.show(activity) { earned = true }
        }
    }

    private fun ensureInitialized() {
        if (initialized.compareAndSet(false, true)) {
            MobileAds.initialize(context) {}
        }
    }

    private companion object {
        // Google's official rewarded TEST unit id. Replace for production.
        const val TEST_REWARDED_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"
    }
}
