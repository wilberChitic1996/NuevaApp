package gt.guardian.cadejo.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import gt.guardian.cadejo.domain.settings.GameSettings
import gt.guardian.cadejo.domain.settings.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsRepository,
) : ViewModel() {
    val settings: StateFlow<GameSettings> =
        repository.settings.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = GameSettings(),
        )

    fun setColorblind(enabled: Boolean) = viewModelScope.launch { repository.setColorblindMode(enabled) }.let {}

    fun setReduceMotion(enabled: Boolean) = viewModelScope.launch { repository.setReduceMotion(enabled) }.let {}

    fun setSound(enabled: Boolean) = viewModelScope.launch { repository.setSound(enabled) }.let {}

    fun setMusic(enabled: Boolean) = viewModelScope.launch { repository.setMusic(enabled) }.let {}

    fun setHaptics(enabled: Boolean) = viewModelScope.launch { repository.setHaptics(enabled) }.let {}
}
