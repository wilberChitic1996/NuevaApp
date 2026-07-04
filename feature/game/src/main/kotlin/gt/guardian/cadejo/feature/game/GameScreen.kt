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
import gt.guardian.cadejo.domain.model.GameState
import gt.guardian.cadejo.domain.model.GameStatus

/**
 * Entry point for the game feature. Grabs the Hilt-scoped [GameViewModel] and
 * renders the stateless [GameScreen]. Splitting route (stateful) from screen
 * (stateless) keeps the screen easy to preview and test.
 */
@Composable
fun GameRoute(modifier: Modifier = Modifier) {
    val viewModel: GameViewModel = hiltViewModel()
    val state by viewModel.state.collectAsState()
    GameScreen(
        state = state,
        modifier = modifier,
        onHexTap = viewModel::onHexTap,
        onWait = viewModel::onWait,
        onRestart = viewModel::onRestart,
    )
}

@Composable
fun GameScreen(
    state: GameState,
    modifier: Modifier = Modifier,
    colorblind: Boolean = false,
    onHexTap: (gt.guardian.cadejo.domain.hex.Hex) -> Unit = {},
    onWait: () -> Unit = {},
    onRestart: () -> Unit = {},
) {
    Surface(modifier = modifier.fillMaxSize(), color = CadejoColors.NightDeep) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Hud(state)

            Spacer(Modifier.heightIn(min = 12.dp))

            HexBoard(
                state = state,
                colorblind = colorblind,
                onHexTap = onHexTap,
                modifier = Modifier.fillMaxWidth(),
            )

            StatusBanner(state)

            Spacer(Modifier.heightIn(min = 12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = onWait,
                    enabled = !state.isOver,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp) // accessible touch target
                        .semantics { contentDescription = "Esperar un turno" },
                ) { Text(stringResource(R.string.game_action_wait)) }

                Button(
                    onClick = onRestart,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .semantics { contentDescription = "Reiniciar la partida" },
                ) { Text(stringResource(R.string.game_action_restart)) }
            }
        }
    }
}

@Composable
private fun Hud(state: GameState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {},
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = stringResource(R.string.game_hud_level, state.level),
            color = CadejoColors.OnNight,
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = stringResource(R.string.game_hud_turn, state.turn),
            color = CadejoColors.OnNightDim,
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

@Composable
private fun StatusBanner(state: GameState) {
    // Fade the banner in when the level ends — a small, cheap animate*AsState use
    // that avoids recomposing anything else.
    val target = if (state.isOver) 1f else 0f
    val alpha by animateFloatAsState(targetValue = target, animationSpec = tween(400), label = "banner")

    val (text, color) = when (state.status) {
        GameStatus.WON -> stringResource(R.string.game_status_won) to CadejoColors.GoldSoft
        GameStatus.LOST -> stringResource(R.string.game_status_lost) to CadejoColors.Llorona
        GameStatus.PLAYING -> "" to CadejoColors.OnNight
    }

    Text(
        text = text,
        color = color,
        textAlign = TextAlign.Center,
        fontWeight = FontWeight.Bold,
        style = MaterialTheme.typography.headlineSmall,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .alpha(alpha)
            .clearAndSetSemantics { contentDescription = text },
    )
}
