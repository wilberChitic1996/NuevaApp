package gt.guardian.cadejo.feature.meta

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import gt.guardian.cadejo.domain.monetization.BillingRepository
import gt.guardian.cadejo.domain.monetization.ProductIds
import gt.guardian.cadejo.domain.progress.PlayerProfile
import gt.guardian.cadejo.domain.progress.ProgressRepository
import gt.guardian.cadejo.domain.progress.PurchaseResult
import gt.guardian.cadejo.domain.progress.UnlockId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Feedback from the last purchase attempt, mapped to a message by the UI. */
enum class ShopFeedback { BOUGHT, ALREADY_OWNED, NOT_ENOUGH_COINS }

@HiltViewModel
class MetaViewModel @Inject constructor(
    private val progress: ProgressRepository,
    private val billing: BillingRepository,
) : ViewModel() {
    val profile: StateFlow<PlayerProfile> =
        progress.profile.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = PlayerProfile.INITIAL,
        )

    private val _feedback = MutableStateFlow<ShopFeedback?>(null)
    val feedback: StateFlow<ShopFeedback?> = _feedback.asStateFlow()

    fun buy(id: UnlockId) {
        viewModelScope.launch {
            _feedback.value =
                when (progress.purchase(id)) {
                    is PurchaseResult.Success -> ShopFeedback.BOUGHT
                    PurchaseResult.AlreadyOwned -> ShopFeedback.ALREADY_OWNED
                    PurchaseResult.InsufficientCoins -> ShopFeedback.NOT_ENOUGH_COINS
                }
        }
    }

    fun equip(id: UnlockId?) {
        viewModelScope.launch { progress.selectSkin(id) }
    }

    /** Launch the "remove ads" IAP; the entitlement flips adsRemoved on success. */
    fun removeAds() {
        viewModelScope.launch { billing.purchase(ProductIds.REMOVE_ADS) }
    }

    fun clearFeedback() {
        _feedback.value = null
    }
}
