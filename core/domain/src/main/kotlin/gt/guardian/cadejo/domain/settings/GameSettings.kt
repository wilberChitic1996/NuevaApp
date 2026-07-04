package gt.guardian.cadejo.domain.settings

import kotlinx.coroutines.flow.Flow

enum class AppLanguage { SYSTEM, ES, EN }

/**
 * User preferences. Small, flat and typed — the ideal shape for DataStore (as
 * opposed to Room, which we reserve for queryable/relational data like history).
 * Consent flags gate analytics and ads, and are never assumed true by default.
 */
data class GameSettings(
    val language: AppLanguage = AppLanguage.SYSTEM,
    val soundOn: Boolean = true,
    val musicOn: Boolean = true,
    val hapticsOn: Boolean = true,
    val colorblindMode: Boolean = false,
    val reduceMotion: Boolean = false,
    val umpConsentCollected: Boolean = false,
    val crashlyticsConsent: Boolean = false,
)

/** Settings store abstraction; DataStore-backed implementation lives in :core:data. */
interface SettingsRepository {
    val settings: Flow<GameSettings>

    suspend fun setLanguage(language: AppLanguage)

    suspend fun setColorblindMode(enabled: Boolean)

    suspend fun setReduceMotion(enabled: Boolean)

    suspend fun setSound(enabled: Boolean)

    suspend fun setMusic(enabled: Boolean)

    suspend fun setHaptics(enabled: Boolean)

    suspend fun setUmpConsentCollected(collected: Boolean)

    suspend fun setCrashlyticsConsent(consent: Boolean)
}
