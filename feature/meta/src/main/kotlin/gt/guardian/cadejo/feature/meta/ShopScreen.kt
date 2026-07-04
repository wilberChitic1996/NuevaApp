package gt.guardian.cadejo.feature.meta

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import gt.guardian.cadejo.domain.progress.PlayerProfile
import gt.guardian.cadejo.domain.progress.UnlockCategory
import gt.guardian.cadejo.domain.progress.UnlockId

@Composable
fun ShopRoute(onBack: () -> Unit = {}, modifier: Modifier = Modifier) {
    val viewModel: MetaViewModel = hiltViewModel()
    val profile by viewModel.profile.collectAsState()
    val feedback by viewModel.feedback.collectAsState()
    ShopScreen(
        profile = profile,
        feedback = feedback,
        modifier = modifier,
        onBuy = viewModel::buy,
        onEquip = viewModel::equip,
        onDismissFeedback = viewModel::clearFeedback,
        onBack = onBack,
    )
}

@Composable
fun ShopScreen(
    profile: PlayerProfile,
    feedback: ShopFeedback?,
    modifier: Modifier = Modifier,
    onBuy: (UnlockId) -> Unit = {},
    onEquip: (UnlockId?) -> Unit = {},
    onDismissFeedback: () -> Unit = {},
    onBack: () -> Unit = {},
) {
    Surface(modifier = modifier.fillMaxSize(), color = CadejoColors.NightDeep) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.shop_title),
                    color = CadejoColors.OnNight,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = stringResource(R.string.shop_coins, profile.coins),
                    color = CadejoColors.GoldSoft,
                    style = MaterialTheme.typography.titleMedium,
                )
            }

            feedback?.let {
                Text(
                    text = stringResource(feedbackMessage(it)),
                    color = CadejoColors.GoldSoft,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }

            Spacer(Modifier.heightIn(min = 8.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
                items(UnlockId.entries.toList(), key = { it.name }) { unlock ->
                    UnlockRow(
                        unlock = unlock,
                        profile = profile,
                        onBuy = { onBuy(unlock) },
                        onEquip = { onEquip(unlock) },
                    )
                }
            }

            TextButton(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            ) { Text(stringResource(R.string.shop_back)) }
        }
    }
}

@Composable
private fun UnlockRow(
    unlock: UnlockId,
    profile: PlayerProfile,
    onBuy: () -> Unit,
    onEquip: () -> Unit,
) {
    val owned = profile.owns(unlock)
    val equipped = profile.selectedSkin == unlock

    Card(colors = CardDefaults.cardColors(containerColor = CadejoColors.NightMid)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(unlockLabel(unlock)), color = CadejoColors.OnNight, fontWeight = FontWeight.Bold)
                Text(
                    text = if (owned) stringResource(R.string.shop_owned_label)
                    else stringResource(R.string.shop_cost, unlock.cost),
                    color = CadejoColors.OnNightDim,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            when {
                owned && unlock.category == UnlockCategory.SKIN -> OutlinedButton(
                    onClick = onEquip,
                    enabled = !equipped,
                    modifier = Modifier.heightIn(min = 48.dp),
                ) { Text(stringResource(if (equipped) R.string.shop_equipped else R.string.shop_equip)) }

                owned -> Text(stringResource(R.string.shop_active), color = CadejoColors.GoldSoft)

                else -> Button(
                    onClick = onBuy,
                    enabled = profile.coins >= unlock.cost,
                    modifier = Modifier.heightIn(min = 48.dp),
                ) { Text(stringResource(R.string.shop_buy)) }
            }
        }
    }
}

private fun feedbackMessage(feedback: ShopFeedback): Int = when (feedback) {
    ShopFeedback.BOUGHT -> R.string.shop_feedback_bought
    ShopFeedback.ALREADY_OWNED -> R.string.shop_feedback_owned
    ShopFeedback.NOT_ENOUGH_COINS -> R.string.shop_feedback_poor
}

private fun unlockLabel(unlock: UnlockId): Int = when (unlock) {
    UnlockId.SKIN_JADE -> R.string.unlock_skin_jade
    UnlockId.SKIN_OBSIDIAN -> R.string.unlock_skin_obsidian
    UnlockId.SKIN_DAWN -> R.string.unlock_skin_dawn
    UnlockId.MODIFIER_SWIFT -> R.string.unlock_mod_swift
    UnlockId.MODIFIER_WARD -> R.string.unlock_mod_ward
}
