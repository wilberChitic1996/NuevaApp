package gt.guardian.cadejo.feature.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import gt.guardian.cadejo.domain.hex.Hex
import gt.guardian.cadejo.domain.model.AbilityId
import gt.guardian.cadejo.domain.model.Balance
import gt.guardian.cadejo.domain.model.Intent
import gt.guardian.cadejo.domain.progress.ProgressRepository
import gt.guardian.cadejo.domain.progress.RunOutcome
import gt.guardian.cadejo.domain.progress.RunRecord
import gt.guardian.cadejo.domain.run.RunEngine
import gt.guardian.cadejo.domain.run.RunState
import gt.guardian.cadejo.domain.run.RunStatus
import gt.guardian.cadejo.domain.session.SeedSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Everything the game screen needs to render, in one immutable value. */
data class GameUiState(
    val run: RunState,
    val leapArming: Boolean = false,
    /** Hexes to highlight (leap destinations while arming). */
    val highlight: Set<Hex> = emptySet(),
)

/**
 * Holds the current [RunState] and forwards user actions to the pure [RunEngine].
 * The ViewModel owns no game rules — only the observable UI state and the seed for
 * a run — so all logic stays in the JVM-testable domain.
 */
@HiltViewModel
class GameViewModel @Inject constructor(
    private val seedSource: SeedSource,
    private val progressRepository: ProgressRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(GameUiState(run = RunEngine.newRun(seedSource.newSeed())))
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    /** A tap on the board: either a leap destination (if arming) or a normal move. */
    fun onHexTap(hex: Hex) {
        val current = _uiState.value
        if (current.run.isOver) return
        if (current.leapArming) {
            apply(Intent.UseAbility(AbilityId.LEAP, hex))
            clearLeapArming()
        } else {
            apply(Intent.Move(hex))
        }
    }

    fun onWait() = apply(Intent.Wait)

    fun onHowl() = apply(Intent.UseAbility(AbilityId.HOWL))

    fun onProtect() = apply(Intent.UseAbility(AbilityId.PROTECTIVE_LIGHT))

    /** Toggle leap-targeting: the next board tap becomes the leap destination. */
    fun onToggleLeap() {
        val s = _uiState.value
        if (s.run.isOver) return
        val ability = s.run.current.ability(AbilityId.LEAP)
        if (ability?.isReady != true) return
        _uiState.value = if (s.leapArming) {
            s.copy(leapArming = false, highlight = emptySet())
        } else {
            s.copy(leapArming = true, highlight = leapTargets(s))
        }
    }

    fun onRestart() {
        _uiState.value = GameUiState(run = RunEngine.newRun(seedSource.newSeed()))
    }

    private fun apply(intent: Intent) {
        val s = _uiState.value
        if (s.run.isOver) return
        val newRun = RunEngine.apply(s.run, intent)
        _uiState.value = s.copy(run = newRun, leapArming = false, highlight = emptySet())

        // Pay out coins and record the run exactly once, on the transition to over.
        if (!s.run.isOver && newRun.isOver) {
            awardRun(newRun)
        }
    }

    private fun awardRun(run: RunState) {
        val record = RunRecord(
            seed = run.seed,
            mode = run.mode,
            reachedLevel = run.levelIndex,
            score = run.score,
            outcome = if (run.status == RunStatus.COMPLETED) RunOutcome.COMPLETED else RunOutcome.FAILED,
        )
        viewModelScope.launch { progressRepository.awardRun(record) }
    }

    private fun clearLeapArming() {
        _uiState.value = _uiState.value.copy(leapArming = false, highlight = emptySet())
    }

    /** Walkable hexes within leap range of the player (excluding the traveler's cell). */
    private fun leapTargets(s: GameUiState): Set<Hex> {
        val game = s.run.current
        return game.board.cells.keys.filter { hex ->
            val d = hex.distanceTo(game.player)
            d in 1..Balance.LEAP_RANGE && game.board.isWalkable(hex) && hex != game.traveler
        }.toSet()
    }
}
