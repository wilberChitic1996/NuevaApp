package gt.guardian.cadejo.feature.daily

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import gt.guardian.cadejo.core.ui.theme.CadejoColors
import gt.guardian.cadejo.domain.daily.LeaderboardEntry
import gt.guardian.cadejo.feature.game.GameScreen

@Composable
fun DailyRoute(onBack: () -> Unit = {}, modifier: Modifier = Modifier) {
    val viewModel: DailyViewModel = hiltViewModel()
    val ui by viewModel.ui.collectAsState()
    val submit by viewModel.submitState.collectAsState()
    val scores by viewModel.topScores.collectAsState()

    Surface(modifier = modifier.fillMaxSize(), color = CadejoColors.NightDeep) {
        Column(Modifier.fillMaxSize().padding(top = 12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.daily_title, viewModel.today),
                    color = CadejoColors.OnNight,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                )
                TextButton(onClick = onBack, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.daily_back))
                }
            }

            // The board + controls are exactly the roguelite screen, driven by the
            // daily seed; "restart" replays today's identical puzzle.
            GameScreen(
                ui = ui,
                modifier = Modifier.weight(1f),
                // No revive in daily: it would break the replay tape the server validates.
                showAdActions = false,
                onHexTap = viewModel::onHexTap,
                onWait = viewModel::onWait,
                onHowl = viewModel::onHowl,
                onToggleLeap = viewModel::onToggleLeap,
                onProtect = viewModel::onProtect,
                onRestart = viewModel::onRetry,
            )

            LeaderboardPanel(
                enabled = viewModel.leaderboardEnabled,
                submit = submit,
                scores = scores,
            )
        }
    }
}

@Composable
private fun LeaderboardPanel(enabled: Boolean, submit: SubmitState, scores: List<LeaderboardEntry>) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CadejoColors.NightMid),
        modifier = Modifier.fillMaxWidth().padding(12.dp),
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(
                text = stringResource(R.string.daily_leaderboard),
                color = CadejoColors.GoldSoft,
                fontWeight = FontWeight.Bold,
            )
            val status = when {
                !enabled -> stringResource(R.string.daily_backend_off)
                submit is SubmitState.Submitting -> stringResource(R.string.daily_submitting)
                submit is SubmitState.Submitted -> stringResource(R.string.daily_submitted)
                submit is SubmitState.Error -> stringResource(R.string.daily_submit_error, submit.reason)
                else -> ""
            }
            if (status.isNotEmpty()) {
                Text(status, color = CadejoColors.OnNightDim, style = MaterialTheme.typography.bodySmall)
            }
            if (scores.isEmpty()) {
                Text(
                    stringResource(R.string.daily_no_scores),
                    color = CadejoColors.OnNightDim,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp),
                )
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 140.dp)) {
                    items(scores, key = { it.rank }) { entry ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text("${entry.rank}. ${entry.displayName}", color = CadejoColors.OnNight)
                            Text("${entry.score}", color = CadejoColors.GoldSoft)
                        }
                    }
                }
            }
        }
    }
}
