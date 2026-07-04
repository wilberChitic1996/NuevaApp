package gt.guardian.cadejo.feature.game

import gt.guardian.cadejo.domain.hex.Hex
import gt.guardian.cadejo.domain.model.AbilityId
import gt.guardian.cadejo.domain.model.Balance
import gt.guardian.cadejo.domain.model.Intent
import gt.guardian.cadejo.domain.run.RunEngine

/**
 * Pure UI-state transitions for a playable run. Both [GameViewModel] (roguelite)
 * and the daily-mode view model drive the same interactions through here, so the
 * board behaves identically in both — and it's all trivially unit-testable since
 * it's just functions over immutable values (no Android, no coroutines).
 */
object GameInteractor {
    fun tap(
        ui: GameUiState,
        hex: Hex,
    ): GameUiState {
        if (ui.run.isOver) return ui
        return if (ui.leapArming) {
            applyIntent(ui, Intent.UseAbility(AbilityId.LEAP, hex))
        } else {
            applyIntent(ui, Intent.Move(hex))
        }
    }

    fun wait(ui: GameUiState): GameUiState = applyIntent(ui, Intent.Wait)

    fun howl(ui: GameUiState): GameUiState = applyIntent(ui, Intent.UseAbility(AbilityId.HOWL))

    fun protect(ui: GameUiState): GameUiState = applyIntent(ui, Intent.UseAbility(AbilityId.PROTECTIVE_LIGHT))

    fun toggleLeap(ui: GameUiState): GameUiState {
        if (ui.run.isOver) return ui
        val ready =
            ui.run.current
                .ability(AbilityId.LEAP)
                ?.isReady == true
        if (!ready) return ui
        return if (ui.leapArming) {
            ui.copy(leapArming = false, highlight = emptySet())
        } else {
            ui.copy(leapArming = true, highlight = leapTargets(ui))
        }
    }

    private fun applyIntent(
        ui: GameUiState,
        intent: Intent,
    ): GameUiState {
        if (ui.run.isOver) return ui
        return ui.copy(run = RunEngine.apply(ui.run, intent), leapArming = false, highlight = emptySet())
    }

    fun leapTargets(ui: GameUiState): Set<Hex> {
        val game = ui.run.current
        return game.board.cells.keys
            .filter { hex ->
                val d = hex.distanceTo(game.player)
                d in 1..Balance.LEAP_RANGE && game.board.isWalkable(hex) && hex != game.traveler
            }.toSet()
    }
}
