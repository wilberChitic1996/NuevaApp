package gt.guardian.cadejo.feature.game

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.test.ext.junit.runners.AndroidJUnit4
import gt.guardian.cadejo.core.ui.theme.CadejoTheme
import gt.guardian.cadejo.domain.run.RunEngine
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Compose smoke test: the game screen renders the board and its controls for a
 * fresh run. Runs on a device/emulator (CI or local). Uses the fixed content
 * descriptions (Spanish, locale-independent) rather than localized button text.
 */
@RunWith(AndroidJUnit4::class)
class GameScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun board_and_controls_are_displayed() {
        val ui = GameUiState(run = RunEngine.newRun(seed = 1L))

        composeRule.setContent {
            CadejoTheme {
                GameScreen(ui = ui)
            }
        }

        composeRule.onNodeWithContentDescription("Tablero hexagonal del juego").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Esperar un turno").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Reiniciar la partida").assertIsDisplayed()
    }
}
