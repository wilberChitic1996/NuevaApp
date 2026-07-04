package gt.guardian.cadejo.feature.game

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import gt.guardian.cadejo.core.ui.board.HexBoard
import gt.guardian.cadejo.core.ui.theme.CadejoColors
import gt.guardian.cadejo.domain.model.AbilityId
import gt.guardian.cadejo.domain.model.Balance
import gt.guardian.cadejo.domain.model.GameState
import gt.guardian.cadejo.domain.run.RunStatus

@Composable
fun GameRoute(modifier: Modifier = Modifier) {
    val viewModel: GameViewModel = hiltViewModel()
    val ui by viewModel.uiState.collectAsState()
    GameScreen(
        ui = ui,
        modifier = modifier,
        onHexTap = viewModel::onHexTap,
        onWait = viewModel::onWait,
        onHowl = viewModel::onHowl,
        onToggleLeap = viewModel::onToggleLeap,
        onProtect = viewModel::onProtect,
        onRestart = viewModel::onRestart,
        onRevive = viewModel::onReviveWithAd,
        onDoubleCoins = viewModel::onDoubleCoinsWithAd,
    )
}

@Composable
fun GameScreen(
    ui: GameUiState,
    modifier: Modifier = Modifier,
    colorblind: Boolean = false,
    showAdActions: Boolean = true,
    onHexTap: (gt.guardian.cadejo.domain.hex.Hex) -> Unit = {},
    onWait: () -> Unit = {},
    onHowl: () -> Unit = {},
    onToggleLeap: () -> Unit = {},
    onProtect: () -> Unit = {},
    onRestart: () -> Unit = {},
    onRevive: () -> Unit = {},
    onDoubleCoins: () -> Unit = {},
) {
    val run = ui.run
    val game = run.current

    Surface(modifier = modifier.fillMaxSize(), color = CadejoColors.NightDeep) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Hud(game, run.levelIndex)

            Spacer(Modifier.heightIn(min = 8.dp))

            HexBoard(
                state = game,
                colorblind = colorblind,
                highlight = ui.highlight,
                onHexTap = onHexTap,
                modifier = Modifier.fillMaxWidth(),
            )

            RunBanner(run.status, game.score)

            if (showAdActions && (ui.canRevive || ui.canDoubleCoins)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    if (ui.canRevive) {
                        OutlinedButton(
                            onClick = onRevive,
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                        ) { Text(stringResource(R.string.game_revive)) }
                    }
                    if (ui.canDoubleCoins) {
                        OutlinedButton(
                            onClick = onDoubleCoins,
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                        ) { Text(stringResource(R.string.game_double_coins)) }
                    }
                }
            }

            Spacer(Modifier.heightIn(min = 8.dp))

            AbilityBar(
                game = game,
                enabled = !run.isOver,
                leapArming = ui.leapArming,
                onHowl = onHowl,
                onToggleLeap = onToggleLeap,
                onProtect = onProtect,
            )

            Spacer(Modifier.heightIn(min = 8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = onWait,
                    enabled = !run.isOver,
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                        .semantics { contentDescription = "Esperar un turno" },
                ) { Text(stringResource(R.string.game_action_wait)) }

                Button(
                    onClick = onRestart,
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                        .semantics { contentDescription = "Reiniciar la partida" },
                ) { Text(stringResource(R.string.game_action_restart)) }
            }
        }
    }
}

@Composable
private fun Hud(game: GameState, levelIndex: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = stringResource(R.string.game_hud_level, levelIndex, Balance.LEVELS_PER_RUN),
            color = CadejoColors.OnNight,
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = stringResource(R.string.game_hud_score, game.score),
            color = CadejoColors.GoldSoft,
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

@Composable
private fun AbilityBar(
    game: GameState,
    enabled: Boolean,
    leapArming: Boolean,
    onHowl: () -> Unit,
    onToggleLeap: () -> Unit,
    onProtect: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AbilityButton(
            label = stringResource(R.string.ability_howl),
            remaining = game.ability(AbilityId.HOWL)?.remaining ?: 0,
            enabled = enabled && game.ability(AbilityId.HOWL)?.isReady == true,
            highlighted = false,
            onClick = onHowl,
            modifier = Modifier.weight(1f),
        )
        AbilityButton(
            label = stringResource(R.string.ability_leap),
            remaining = game.ability(AbilityId.LEAP)?.remaining ?: 0,
            enabled = enabled && game.ability(AbilityId.LEAP)?.isReady == true,
            highlighted = leapArming,
            onClick = onToggleLeap,
            modifier = Modifier.weight(1f),
        )
        AbilityButton(
            label = stringResource(R.string.ability_protect),
            remaining = game.ability(AbilityId.PROTECTIVE_LIGHT)?.remaining ?: 0,
            enabled = enabled && game.ability(AbilityId.PROTECTIVE_LIGHT)?.isReady == true,
            highlighted = game.travelerShield > 0,
            onClick = onProtect,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun AbilityButton(
    label: String,
    remaining: Int,
    enabled: Boolean,
    highlighted: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val text = if (remaining > 0) "$label ($remaining)" else label
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        colors = if (highlighted) {
            ButtonDefaults.outlinedButtonColors(contentColor = CadejoColors.NightDeep)
        } else {
            ButtonDefaults.outlinedButtonColors(contentColor = CadejoColors.GoldSoft)
        },
        modifier = modifier.heightIn(min = 48.dp)
            .semantics { contentDescription = "$label${if (remaining > 0) ", en recarga $remaining turnos" else ", lista"}" },
    ) { Text(text, textAlign = TextAlign.Center) }
}

@Composable
private fun RunBanner(status: RunStatus, score: Int) {
    val over = status != RunStatus.RUNNING
    val alpha by animateFloatAsState(if (over) 1f else 0f, animationSpec = tween(400), label = "runBanner")
    val (text, color) = when (status) {
        RunStatus.COMPLETED -> stringResource(R.string.run_completed, score) to CadejoColors.GoldSoft
        RunStatus.FAILED -> stringResource(R.string.run_failed, score) to CadejoColors.Llorona
        RunStatus.RUNNING -> "" to Color.Transparent
    }
    Text(
        text = text,
        color = color,
        textAlign = TextAlign.Center,
        fontWeight = FontWeight.Bold,
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp).alpha(alpha)
            .clearAndSetSemantics { contentDescription = text },
    )
}
