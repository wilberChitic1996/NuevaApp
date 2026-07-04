package gt.guardian.cadejo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint
import gt.guardian.cadejo.core.ui.theme.CadejoTheme
import gt.guardian.cadejo.feature.game.GameRoute

/**
 * The single Activity. [AndroidEntryPoint] lets Hilt inject into this Activity and
 * the Compose view models it hosts. Phase 1 shows the game screen directly; a
 * navigation graph (menu -> game -> shop -> daily) is introduced in later phases.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CadejoTheme {
                GameRoute()
            }
        }
    }
}
