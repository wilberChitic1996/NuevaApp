package gt.guardian.cadejo.core.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import gt.guardian.cadejo.domain.settings.AppLanguage
import gt.guardian.cadejo.domain.settings.GameSettings
import gt.guardian.cadejo.domain.settings.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "cadejo_settings")

/**
 * Settings backed by DataStore (Preferences). We use Preferences rather than the
 * protobuf variant to avoid the codegen toolchain for a handful of flat flags; the
 * typed [GameSettings] domain model gives the same type-safety at the boundary.
 */
@Singleton
class DataStoreSettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) : SettingsRepository {
    override val settings: Flow<GameSettings> =
        context.dataStore.data.map { p ->
            GameSettings(
                language = p[Keys.LANGUAGE]?.let { runCatching { AppLanguage.valueOf(it) }.getOrNull() } ?: AppLanguage.SYSTEM,
                soundOn = p[Keys.SOUND] ?: true,
                musicOn = p[Keys.MUSIC] ?: true,
                hapticsOn = p[Keys.HAPTICS] ?: true,
                colorblindMode = p[Keys.COLORBLIND] ?: false,
                reduceMotion = p[Keys.REDUCE_MOTION] ?: false,
                umpConsentCollected = p[Keys.UMP_CONSENT] ?: false,
                crashlyticsConsent = p[Keys.CRASHLYTICS_CONSENT] ?: false,
            )
        }

    override suspend fun setLanguage(language: AppLanguage) = edit { it[Keys.LANGUAGE] = language.name }

    override suspend fun setColorblindMode(enabled: Boolean) = edit { it[Keys.COLORBLIND] = enabled }

    override suspend fun setReduceMotion(enabled: Boolean) = edit { it[Keys.REDUCE_MOTION] = enabled }

    override suspend fun setSound(enabled: Boolean) = edit { it[Keys.SOUND] = enabled }

    override suspend fun setMusic(enabled: Boolean) = edit { it[Keys.MUSIC] = enabled }

    override suspend fun setHaptics(enabled: Boolean) = edit { it[Keys.HAPTICS] = enabled }

    override suspend fun setUmpConsentCollected(collected: Boolean) = edit { it[Keys.UMP_CONSENT] = collected }

    override suspend fun setCrashlyticsConsent(consent: Boolean) = edit { it[Keys.CRASHLYTICS_CONSENT] = consent }

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.dataStore.edit(block)
    }

    private object Keys {
        val LANGUAGE = stringPreferencesKey("language")
        val SOUND = booleanPreferencesKey("sound")
        val MUSIC = booleanPreferencesKey("music")
        val HAPTICS = booleanPreferencesKey("haptics")
        val COLORBLIND = booleanPreferencesKey("colorblind")
        val REDUCE_MOTION = booleanPreferencesKey("reduce_motion")
        val UMP_CONSENT = booleanPreferencesKey("ump_consent")
        val CRASHLYTICS_CONSENT = booleanPreferencesKey("crashlytics_consent")
    }
}
