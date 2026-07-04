package gt.guardian.cadejo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import gt.guardian.cadejo.domain.monetization.ConsentManager
import gt.guardian.cadejo.domain.monetization.RewardedAdService
import gt.guardian.cadejo.navigation.CadejoNavHost
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * The single Activity. [AndroidEntryPoint] lets Hilt inject here and into the
 * Compose view models it hosts.
 *
 * On start it resolves UMP consent BEFORE preloading ads — never requesting an ad
 * without consent — then hands off to the Compose navigation graph.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var consentManager: ConsentManager
    @Inject lateinit var rewardedAdService: RewardedAdService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        lifecycleScope.launch {
            consentManager.ensureConsent()
            if (consentManager.canRequestAds) {
                rewardedAdService.preload()
            }
        }

        setContent {
            CadejoNavHost()
        }
    }
}
