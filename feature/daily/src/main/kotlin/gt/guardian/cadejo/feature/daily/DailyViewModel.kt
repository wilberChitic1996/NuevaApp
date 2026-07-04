package gt.guardian.cadejo.feature.daily

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import gt.guardian.cadejo.domain.daily.DailySeed
import gt.guardian.cadejo.domain.daily.DateProvider
import gt.guardian.cadejo.domain.daily.IntentCodec
import gt.guardian.cadejo.domain.daily.LeaderboardEntry
import gt.guardian.cadejo.domain.daily.LeaderboardRepository
import gt.guardian.cadejo.domain.daily.ScoreSubmission
import gt.guardian.cadejo.domain.daily.SubmitResult
import gt.guardian.cadejo.domain.hex.Hex
import gt.guardian.cadejo.domain.progress.ProgressRepository
import gt.guardian.cadejo.domain.progress.RunOutcome
import gt.guardian.cadejo.domain.progress.RunRecord
import gt.guardian.cadejo.domain.run.RunEngine
import gt.guardian.cadejo.domain.run.RunMode
import gt.guardian.cadejo.domain.run.RunState
import gt.guardian.cadejo.domain.run.RunStatus
import gt.guardian.cadejo.feature.game.GameInteractor
import gt.guardian.cadejo.feature.game.GameUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Status of the daily leaderboard submission. */
sealed interface SubmitState {
    data object Idle : SubmitState

    data object Submitting : SubmitState

    data object Submitted : SubmitState

    data object Disabled : SubmitState

    data class Error(
        val reason: String,
    ) : SubmitState
}

@HiltViewModel
class DailyViewModel @Inject constructor(
    dateProvider: DateProvider,
    private val leaderboard: LeaderboardRepository,
    private val progress: ProgressRepository,
) : ViewModel() {
    val today: String = dateProvider.todayUtcIso()
    private val seed: Long = DailySeed.seedForDate(today)

    val leaderboardEnabled: Boolean = leaderboard.isEnabled

    private val _ui = MutableStateFlow(GameUiState(run = RunEngine.newRun(seed, RunMode.DAILY)))
    val ui: StateFlow<GameUiState> = _ui.asStateFlow()

    private val _submit = MutableStateFlow<SubmitState>(SubmitState.Idle)
    val submitState: StateFlow<SubmitState> = _submit.asStateFlow()

    val topScores: StateFlow<List<LeaderboardEntry>> =
        leaderboard.topScores(today).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun onHexTap(hex: Hex) = update { GameInteractor.tap(it, hex) }

    fun onWait() = update { GameInteractor.wait(it) }

    fun onHowl() = update { GameInteractor.howl(it) }

    fun onProtect() = update { GameInteractor.protect(it) }

    fun onToggleLeap() = update(runEndable = false) { GameInteractor.toggleLeap(it) }

    /** Replays today's puzzle from the same seed (leaderboard keeps the best). */
    fun onRetry() {
        _submit.value = SubmitState.Idle
        _ui.value = GameUiState(run = RunEngine.newRun(seed, RunMode.DAILY))
    }

    private fun update(
        runEndable: Boolean = true,
        transition: (GameUiState) -> GameUiState,
    ) {
        val previous = _ui.value
        val next = transition(previous)
        _ui.value = next
        if (runEndable && !previous.run.isOver && next.run.isOver) {
            onRunEnded(next.run)
        }
    }

    private fun onRunEnded(run: RunState) {
        viewModelScope.launch {
            progress.awardRun(
                RunRecord(
                    seed = run.seed,
                    mode = run.mode,
                    reachedLevel = run.levelIndex,
                    score = run.score,
                    outcome = if (run.status == RunStatus.COMPLETED) RunOutcome.COMPLETED else RunOutcome.FAILED,
                ),
            )

            _submit.value = SubmitState.Submitting
            val submission =
                ScoreSubmission(
                    dateUtc = today,
                    seed = seed,
                    reachedLevel = run.levelIndex,
                    score = run.score,
                    intentsEncoded = IntentCodec.encode(run.intents),
                    displayName = "Guardián",
                    // Play Integrity token is attached in Phase 5 once the SDK is wired.
                    integrityToken = null,
                )
            _submit.value =
                when (val result = leaderboard.submit(submission)) {
                    SubmitResult.Accepted -> SubmitState.Submitted
                    SubmitResult.Disabled -> SubmitState.Disabled
                    is SubmitResult.Rejected -> SubmitState.Error(result.reason)
                }
        }
    }
}
