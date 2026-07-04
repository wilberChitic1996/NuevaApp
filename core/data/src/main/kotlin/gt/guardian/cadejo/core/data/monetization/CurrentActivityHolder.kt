package gt.guardian.cadejo.core.data.monetization

import android.app.Activity
import java.lang.ref.WeakReference
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Holds a weak reference to the currently-resumed Activity so the ad / billing /
 * consent implementations can present UI without view models ever handling an
 * Activity. Updated from [gt.guardian.cadejo.CadejoApplication] via
 * ActivityLifecycleCallbacks. Weak so it never leaks a destroyed Activity.
 */
@Singleton
class CurrentActivityHolder @Inject constructor() {
    private var ref: WeakReference<Activity>? = null

    var current: Activity?
        get() = ref?.get()
        set(value) {
            ref = value?.let { WeakReference(it) }
        }
}
