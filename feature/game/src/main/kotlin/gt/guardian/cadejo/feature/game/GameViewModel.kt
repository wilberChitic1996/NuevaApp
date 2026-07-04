package gt.guardian.cadejo.feature.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import gt.guardian.cadejo.domain.hex.Hex
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
 * Holds the current run and forwards board actions to [GameInteractor] (pure) and
 * the pure [RunEngine]. The ViewModel owns no game rules — only the observable
 * state, the seed, and the side effect of paying out coins when a run ends.
 */
@HiltViewModel
class GameViewModel @Inject constructor(
    private val seedSource: SeedSource,
    private val progressRepository: ProgressRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(GameUiState(run = RunEngine.newRun(seedSource.newSeed())))
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    fun onHexTap(hex: Hex) = update { GameInteractor.tap(it, hex) }
    fun onWait() = update { GameInteractor.wait(it) }
    fun onHowl() = update { GameInteractor.howl(it) }
    fun onProtect() = update { GameInteractor.protect(it) }
    fun onToggleLeap() = update(awardable = false) { GameInteractor.toggleLeap(it) }

    fun onRestart() {
        _uiState.value = GameUiState(run = RunEngine.newRun(seedSource.newSeed()))
    }

    /** Apply a transition and, if the run just ended, pay it out once. */
    private fun update(awardable: Boolean = true, transition: (GameUiState) -> GameUiState) {
        val previous = _uiState.value
        val next = transition(previous)
        _uiState.value = next
        if (awardable && !previous.run.isOver && next.run.isOver) {
            awardRun(next.run)
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
}
