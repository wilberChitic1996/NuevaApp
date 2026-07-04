package gt.guardian.cadejo.feature.game

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import gt.guardian.cadejo.domain.engine.GameEngine
import gt.guardian.cadejo.domain.hex.Hex
import gt.guardian.cadejo.domain.level.LevelGenerator
import gt.guardian.cadejo.domain.model.GameState
import gt.guardian.cadejo.domain.model.Intent
import gt.guardian.cadejo.domain.session.SeedSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * Holds the current [GameState] and forwards user actions to the pure
 * [GameEngine]. The ViewModel is a thin shell: it owns no game rules, only the
 * observable state and the seed used to start a run. That keeps all logic in the
 * JVM-testable domain and makes this class trivial to test with Turbine.
 */
@HiltViewModel
class GameViewModel @Inject constructor(
    private val seedSource: SeedSource,
) : ViewModel() {

    private val _state = MutableStateFlow(newGame())
    val state: StateFlow<GameState> = _state.asStateFlow()

    /** Attempt to move the Cadejo to [hex]. Illegal taps are no-ops in the engine. */
    fun onHexTap(hex: Hex) {
        val current = _state.value
        if (current.isOver) return
        _state.value = GameEngine.reduce(current, Intent.Move(hex))
    }

    /** Pass the turn; enemies still act. */
    fun onWait() {
        val current = _state.value
        if (current.isOver) return
        _state.value = GameEngine.reduce(current, Intent.Wait)
    }

    /** Start a fresh run from a new seed. */
    fun onRestart() {
        _state.value = newGame()
    }

    private fun newGame(): GameState =
        LevelGenerator.generate(seed = seedSource.newSeed(), levelIndex = 1)
}
