package gt.guardian.cadejo.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

/**
 * The app theme. The game is set at night, so it is always dark: we don't offer a
 * light scheme, which keeps the mood consistent and the contrast decisions simple.
 * [isSystemInDarkTheme] is referenced only to keep the signature idiomatic.
 */
private val CadejoDarkScheme = darkColorScheme(
    primary = CadejoColors.Gold,
    onPrimary = CadejoColors.NightDeep,
    secondary = CadejoColors.NightViolet,
    background = CadejoColors.NightDeep,
    onBackground = CadejoColors.OnNight,
    surface = CadejoColors.NightMid,
    onSurface = CadejoColors.OnNight,
    error = CadejoColors.HexHazard,
)

@Composable
fun CadejoTheme(
    @Suppress("UNUSED_PARAMETER") darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = CadejoDarkScheme,
        typography = Typography(),
        content = content,
    )
}
