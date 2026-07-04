package gt.guardian.cadejo.core.data.monetization

import android.content.Context
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import dagger.hilt.android.qualifiers.ApplicationContext
import gt.guardian.cadejo.domain.monetization.ConsentManager
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * GDPR/CCPA consent via Google's User Messaging Platform. The form is resolved
 * BEFORE ads initialise. If no Activity is available or the SDK errors, we fail
 * open to "no ads" rather than showing unconsented ads.
 */
@Singleton
class UmpConsentManager @Inject constructor(
    @ApplicationContext context: Context,
    private val activityHolder: CurrentActivityHolder,
) : ConsentManager {

    private val consentInformation = UserMessagingPlatform.getConsentInformation(context)

    override val canRequestAds: Boolean
        get() = runCatching { consentInformation.canRequestAds() }.getOrDefault(false)

    override suspend fun ensureConsent() {
        val activity = activityHolder.current ?: return
        suspendCancellableCoroutine<Unit> { cont ->
            val params = ConsentRequestParameters.Builder().build()
            consentInformation.requestConsentInfoUpdate(
                activity,
                params,
                {
                    UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                        if (formError != null) Timber.w("Consent form error: %s", formError.message)
                        if (cont.isActive) cont.resume(Unit)
                    }
                },
                { requestError ->
                    Timber.w("Consent info update failed: %s", requestError.message)
                    if (cont.isActive) cont.resume(Unit)
                },
            )
        }
    }
}
