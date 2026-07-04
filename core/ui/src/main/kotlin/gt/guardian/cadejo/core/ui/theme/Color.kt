package gt.guardian.cadejo.core.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * The nocturnal palette: deep blues and purples with warm gold accents, inspired
 * by night skies and Guatemalan textile motifs. Defined once here so every module
 * shares one source of truth for colour.
 */
object CadejoColors {
    // Backgrounds
    val NightDeep = Color(0xFF0D1B2A)
    val NightMid = Color(0xFF1B263B)
    val NightViolet = Color(0xFF3A2E5C)

    // Board
    val HexFloor = Color(0xFF223A5E)
    val HexFloorAlt = Color(0xFF1D3350)
    val HexWall = Color(0xFF0A1220)
    val HexGoal = Color(0xFF2E5D4B)
    val HexHazard = Color(0xFF5C2E3A)
    val HexBorder = Color(0xFF4A5B7A)

    // Accents (gold thread)
    val Gold = Color(0xFFE0A500)
    val GoldSoft = Color(0xFFF2C94C)

    // Entities — chosen so white/black Cadejo differ in luminance AND are paired
    // with distinct silhouettes in the renderer for colour-blind players.
    val CadejoWhite = Color(0xFFF5F3FF)
    val CadejoBlack = Color(0xFF120B1F)
    val Traveler = Color(0xFFE0A500)
    val Llorona = Color(0xFF6FD3E0)
    val Sombreron = Color(0xFF9B7BD4)

    val OnNight = Color(0xFFE8ECF4)
    val OnNightDim = Color(0xFF9BA6BE)
}
