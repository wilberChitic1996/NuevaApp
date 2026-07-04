package gt.guardian.cadejo.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import gt.guardian.cadejo.R
import gt.guardian.cadejo.core.ui.theme.CadejoColors
import gt.guardian.cadejo.domain.settings.GameSettings

@Composable
fun SettingsRoute(
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val viewModel: SettingsViewModel = hiltViewModel()
    val settings by viewModel.settings.collectAsState()
    SettingsScreen(
        settings = settings,
        modifier = modifier,
        onColorblind = viewModel::setColorblind,
        onReduceMotion = viewModel::setReduceMotion,
        onSound = viewModel::setSound,
        onMusic = viewModel::setMusic,
        onHaptics = viewModel::setHaptics,
        onBack = onBack,
    )
}

@Composable
fun SettingsScreen(
    settings: GameSettings,
    modifier: Modifier = Modifier,
    onColorblind: (Boolean) -> Unit = {},
    onReduceMotion: (Boolean) -> Unit = {},
    onSound: (Boolean) -> Unit = {},
    onMusic: (Boolean) -> Unit = {},
    onHaptics: (Boolean) -> Unit = {},
    onBack: () -> Unit = {},
) {
    Surface(modifier = modifier.fillMaxSize(), color = CadejoColors.NightDeep) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Text(
                text = stringResource(R.string.settings_title),
                color = CadejoColors.OnNight,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            ToggleRow(stringResource(R.string.settings_colorblind), settings.colorblindMode, onColorblind)
            ToggleRow(stringResource(R.string.settings_reduce_motion), settings.reduceMotion, onReduceMotion)
            ToggleRow(stringResource(R.string.settings_sound), settings.soundOn, onSound)
            ToggleRow(stringResource(R.string.settings_music), settings.musicOn, onMusic)
            ToggleRow(stringResource(R.string.settings_haptics), settings.hapticsOn, onHaptics)

            TextButton(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(top = 12.dp),
            ) { Text(stringResource(R.string.settings_back)) }
        }
    }
}

@Composable
private fun ToggleRow(
    label: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = CadejoColors.OnNight)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
