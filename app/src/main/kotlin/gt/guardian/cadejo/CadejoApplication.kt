package gt.guardian.cadejo

import android.app.Activity
import android.app.Application
import android.os.Bundle
import dagger.hilt.android.HiltAndroidApp
import gt.guardian.cadejo.core.data.monetization.CurrentActivityHolder
import timber.log.Timber
import javax.inject.Inject

/**
 * Application entry point. [HiltAndroidApp] triggers Hilt's code generation.
 *
 * Also tracks the currently-resumed Activity into [CurrentActivityHolder] so the
 * ad/billing/consent layers can present UI without any view model touching an
 * Activity. Timber is planted only in debug builds; release stays silent until a
 * consent-gated Crashlytics tree is added.
 */
@HiltAndroidApp
class CadejoApplication : Application() {

    @Inject lateinit var activityHolder: CurrentActivityHolder

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacksAdapter() {
            override fun onActivityResumed(activity: Activity) {
                activityHolder.current = activity
            }

            override fun onActivityPaused(activity: Activity) {
                if (activityHolder.current === activity) activityHolder.current = null
            }
        })
    }
}

/** No-op base so callbacks only override what they need. */
private abstract class ActivityLifecycleCallbacksAdapter : Application.ActivityLifecycleCallbacks {
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityStarted(activity: Activity) {}
    override fun onActivityResumed(activity: Activity) {}
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {}
}
