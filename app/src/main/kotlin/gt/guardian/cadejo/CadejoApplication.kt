package gt.guardian.cadejo

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

/**
 * Application entry point. [HiltAndroidApp] triggers Hilt's code generation and
 * creates the app-level dependency container.
 *
 * Timber is planted only in debug builds; in release we stay silent until Phase 6
 * wires a Crashlytics tree gated behind user consent — so no logs leak in
 * production and nothing is reported without permission.
 */
@HiltAndroidApp
class CadejoApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}
