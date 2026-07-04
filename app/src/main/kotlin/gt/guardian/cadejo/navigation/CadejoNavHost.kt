package gt.guardian.cadejo.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import gt.guardian.cadejo.core.ui.theme.CadejoTheme
import gt.guardian.cadejo.feature.daily.DailyRoute
import gt.guardian.cadejo.feature.game.GameRoute
import gt.guardian.cadejo.feature.meta.ShopRoute
import gt.guardian.cadejo.settings.SettingsRoute

/** Top-level routes. */
private object Routes {
    const val MENU = "menu"
    const val GAME = "game"
    const val DAILY = "daily"
    const val SHOP = "shop"
    const val SETTINGS = "settings"
}

/**
 * The single navigation graph: a menu that branches into the roguelite run, the
 * daily challenge, the shop and settings. Each leaf screen pops back to the menu.
 */
@Composable
fun CadejoNavHost() {
    CadejoTheme {
        val nav = rememberNavController()
        NavHost(navController = nav, startDestination = Routes.MENU) {
            composable(Routes.MENU) {
                MenuScreen(
                    onPlay = { nav.navigate(Routes.GAME) },
                    onDaily = { nav.navigate(Routes.DAILY) },
                    onShop = { nav.navigate(Routes.SHOP) },
                    onSettings = { nav.navigate(Routes.SETTINGS) },
                )
            }
            composable(Routes.GAME) { GameRoute() }
            composable(Routes.DAILY) { DailyRoute(onBack = { nav.popBackStack() }) }
            composable(Routes.SHOP) { ShopRoute(onBack = { nav.popBackStack() }) }
            composable(Routes.SETTINGS) { SettingsRoute(onBack = { nav.popBackStack() }) }
        }
    }
}
