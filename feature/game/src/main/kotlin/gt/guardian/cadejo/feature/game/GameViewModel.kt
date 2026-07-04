package gt.guardian.cadejo.feature.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import gt.guardian.cadejo.domain.hex.Hex
import gt.guardian.cadejo.domain.monetization.RewardOutcome
import gt.guardian.cadejo.domain.monetization.RewardedAdService
import gt.guardian.cadejo.domain.progress.Economy
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
    /** True once the player has doubled the coins from this run via a rewarded ad. */
    val coinsDoubled: Boolean = false,
) {
    val canRevive: Boolean get() = run.status == RunStatus.FAILED && run.revivesUsed < 1
    val canDoubleCoins: Boolean get() = run.isOver && !coinsDoubled && run.score > 0
}

/**
 * Holds the current run and forwards board actions to [GameInteractor] (pure) and
 * the pure [RunEngine]. Side effects: paying out coins when a run ends, and the two
 * optional rewarded-ad flows (revive, double coins).
 */
@HiltViewModel
class GameViewModel @Inject constructor(
    private val seedSource: SeedSource,
    private val progressRepository: ProgressRepository,
    private val rewardedAds: RewardedAdService,
) : ViewModel() {
    private val _uiState = MutableStateFlow(GameUiState(run = RunEngine.newRun(seedSource.newSeed())))
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private var lastEarnedCoins: Long = 0

    fun onHexTap(hex: Hex) = update { GameInteractor.tap(it, hex) }

    fun onWait() = update { GameInteractor.wait(it) }

    fun onHowl() = update { GameInteractor.howl(it) }

    fun onProtect() = update { GameInteractor.protect(it) }

    fun onToggleLeap() = update(awardable = false) { GameInteractor.toggleLeap(it) }

    fun onRestart() {
        lastEarnedCoins = 0
        _uiState.value = GameUiState(run = RunEngine.newRun(seedSource.newSeed()))
    }

    /** Watch a rewarded ad to revive the current level (once per run). */
    fun onReviveWithAd() {
        val state = _uiState.value
        if (!state.canRevive) return
        viewModelScope.launch {
            if (rewardedAds.showRewarded() == RewardOutcome.EARNED) {
                _uiState.value = state.copy(run = RunEngine.revive(state.run))
            }
        }
    }

    /** Watch a rewarded ad to double the coins earned from this run. */
    fun onDoubleCoinsWithAd() {
        val state = _uiState.value
        if (!state.canDoubleCoins) return
        viewModelScope.launch {
            if (rewardedAds.showRewarded() == RewardOutcome.EARNED) {
                progressRepository.addCoins(lastEarnedCoins)
                _uiState.value = _uiState.value.copy(coinsDoubled = true)
            }
        }
    }

    private fun update(
        awardable: Boolean = true,
        transition: (GameUiState) -> GameUiState,
    ) {
        val previous = _uiState.value
        val next = transition(previous)
        _uiState.value = next
        if (awardable && !previous.run.isOver && next.run.isOver) {
            awardRun(next.run)
            rewardedAds.preload() // ready for a possible revive/double
        }
    }

    private fun awardRun(run: RunState) {
        lastEarnedCoins = Economy.coinsForRun(run.score, run.levelIndex)
        val record =
            RunRecord(
                seed = run.seed,
                mode = run.mode,
                reachedLevel = run.levelIndex,
                score = run.score,
                outcome = if (run.status == RunStatus.COMPLETED) RunOutcome.COMPLETED else RunOutcome.FAILED,
            )
        viewModelScope.launch { progressRepository.awardRun(record) }
    }
}
