package gt.guardian.cadejo.core.ui.board

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import gt.guardian.cadejo.core.ui.theme.CadejoColors
import gt.guardian.cadejo.domain.hex.Hex
import gt.guardian.cadejo.domain.model.Enemy
import gt.guardian.cadejo.domain.model.GameState
import gt.guardian.cadejo.domain.model.SpiritKind
import gt.guardian.cadejo.domain.model.Terrain

/**
 * Renders a [GameState] on a Compose [Canvas] and reports taps as the hex the user
 * touched. The renderer is intentionally "dumb": it draws whatever state it's given
 * and never mutates game logic — all rules live in :core:domain.
 *
 * Accessibility: entities are distinguished by **silhouette** as well as colour
 * (round vs. angular vs. hat), so colour-blind players can tell the white Cadejo
 * from the black one. When [colorblind] is true the shape cues are emphasised.
 */
@Composable
fun HexBoard(
    state: GameState,
    modifier: Modifier = Modifier,
    colorblind: Boolean = false,
    highlight: Set<Hex> = emptySet(),
    onHexTap: (Hex) -> Unit = {},
) {
    // Remember the layout keyed on the current cells so it only recomputes when the
    // board shape changes, not every recomposition — cheap and stable.
    val cells = state.board.cells.keys

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .semantics { contentDescription = "Tablero hexagonal del juego" },
    ) {
        // We can't know pixel size before layout, so compute the layout inside the
        // Canvas draw scope where size is available, and also for tap mapping.
        var layoutHolder = remember { LayoutHolder() }

        Canvas(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .pointerInput(cells) {
                        detectTapGestures { tap ->
                            layoutHolder.layout?.let { onHexTap(it.toHex(tap)) }
                        }
                    },
        ) {
            val layout = HexLayout.fitting(cells, size.width, size.height)
            layoutHolder.layout = layout

            drawCells(state, layout, colorblind)
            highlight.forEach { drawHighlight(layout, it) }
            drawGoal(state, layout)
            state.enemies.forEach { drawEnemy(it, layout, colorblind) }
            drawTraveler(layout.toPixel(state.traveler), layout.size, state.travelerShield > 0)
            drawCadejoWhite(layout.toPixel(state.player), layout.size, colorblind)
        }
    }
}

/** Small mutable holder so the tap handler and the draw pass share one layout. */
private class LayoutHolder(
    var layout: HexLayout? = null,
)

private fun DrawScope.drawCells(
    state: GameState,
    layout: HexLayout,
    colorblind: Boolean,
) {
    state.board.cells.forEach { (hex, cell) ->
        val center = layout.toPixel(hex)
        val fill =
            when (cell.terrain) {
                Terrain.FLOOR -> if ((hex.q + hex.r) % 2 == 0) CadejoColors.HexFloor else CadejoColors.HexFloorAlt
                Terrain.WALL -> CadejoColors.HexWall
                Terrain.GOAL -> CadejoColors.HexGoal
                Terrain.HAZARD -> CadejoColors.HexHazard
            }
        val path = hexPath(layout.corners(center))
        drawPath(path, color = fill)
        drawPath(path, color = CadejoColors.HexBorder, style = Stroke(width = layout.size * 0.06f))
    }
}

private fun DrawScope.drawHighlight(
    layout: HexLayout,
    hex: Hex,
) {
    val path = hexPath(layout.corners(layout.toPixel(hex)))
    drawPath(path, color = CadejoColors.GoldSoft.copy(alpha = 0.28f))
    drawPath(path, color = CadejoColors.GoldSoft, style = Stroke(width = layout.size * 0.08f))
}

private fun DrawScope.drawTraveler(
    center: Offset,
    s: Float,
    shielded: Boolean,
) {
    // The escortee: a small warm figure (head + cloak triangle) so it reads as a
    // person distinct from the animal spirits.
    val cloak =
        Path().apply {
            moveTo(center.x, center.y - s * 0.05f)
            lineTo(center.x + s * 0.22f, center.y + s * 0.3f)
            lineTo(center.x - s * 0.22f, center.y + s * 0.3f)
            close()
        }
    drawPath(cloak, CadejoColors.Traveler)
    drawCircle(CadejoColors.Traveler, radius = s * 0.14f, center = Offset(center.x, center.y - s * 0.18f))
    if (shielded) {
        // Protective light: a soft cyan-white ring around the traveler.
        drawCircle(CadejoColors.GoldSoft, radius = s * 0.42f, center = center, style = Stroke(s * 0.09f))
    }
}

private fun DrawScope.drawGoal(
    state: GameState,
    layout: HexLayout,
) {
    val center = layout.toPixel(state.goal)
    val s = layout.size
    // Concentric gold rings — a clear, shape-based "sanctuary" marker.
    drawCircle(CadejoColors.Gold, radius = s * 0.42f, center = center, style = Stroke(s * 0.08f))
    drawCircle(CadejoColors.GoldSoft, radius = s * 0.22f, center = center, style = Stroke(s * 0.08f))
}

private fun DrawScope.drawCadejoWhite(
    center: Offset,
    s: Float,
    colorblind: Boolean,
) {
    // Friendly ROUND silhouette: body circle + two pointed ears. White fill with a
    // gold rim so it reads against light and dark hexes alike.
    drawCircle(CadejoColors.CadejoWhite, radius = s * 0.34f, center = center)
    drawCircle(CadejoColors.Gold, radius = s * 0.34f, center = center, style = Stroke(s * 0.06f))
    // Ears
    val ear = s * 0.22f
    listOf(-1f, 1f).forEach { dir ->
        val base = Offset(center.x + dir * s * 0.2f, center.y - s * 0.24f)
        val path =
            Path().apply {
                moveTo(base.x, base.y)
                lineTo(base.x + dir * ear * 0.5f, base.y - ear)
                lineTo(base.x + dir * ear, base.y)
                close()
            }
        drawPath(path, CadejoColors.CadejoWhite)
    }
    if (colorblind) {
        // Extra cue: a small gold dot marks the "good" spirit.
        drawCircle(CadejoColors.Gold, radius = s * 0.07f, center = center)
    }
}

private fun DrawScope.drawEnemy(
    enemy: Enemy,
    layout: HexLayout,
    colorblind: Boolean,
) {
    val center = layout.toPixel(enemy.position)
    val s = layout.size
    when (enemy.kind) {
        SpiritKind.CADEJO_NEGRO -> {
            // ANGULAR silhouette (diamond) — deliberately unlike the round white
            // Cadejo, so the two are distinguishable by shape, not just colour.
            val d = s * 0.4f
            val path =
                Path().apply {
                    moveTo(center.x, center.y - d)
                    lineTo(center.x + d, center.y)
                    lineTo(center.x, center.y + d)
                    lineTo(center.x - d, center.y)
                    close()
                }
            drawPath(path, CadejoColors.CadejoBlack)
            drawPath(path, CadejoColors.GoldSoft, style = Stroke(s * 0.06f))
            if (colorblind) drawPath(hexCross(center, s * 0.16f), CadejoColors.GoldSoft, style = Stroke(s * 0.05f))
        }
        SpiritKind.LLORONA -> drawTeardrop(center, s, CadejoColors.Llorona)
        SpiritKind.SOMBRERON -> drawHat(center, s, CadejoColors.Sombreron)
    }
}

private fun DrawScope.drawTeardrop(
    center: Offset,
    s: Float,
    color: Color,
) {
    val path =
        Path().apply {
            moveTo(center.x, center.y - s * 0.4f)
            cubicTo(center.x + s * 0.4f, center.y, center.x + s * 0.28f, center.y + s * 0.4f, center.x, center.y + s * 0.4f)
            cubicTo(center.x - s * 0.28f, center.y + s * 0.4f, center.x - s * 0.4f, center.y, center.x, center.y - s * 0.4f)
            close()
        }
    drawPath(path, color)
}

private fun DrawScope.drawHat(
    center: Offset,
    s: Float,
    color: Color,
) {
    val brim =
        Path().apply {
            moveTo(center.x - s * 0.4f, center.y + s * 0.12f)
            lineTo(center.x + s * 0.4f, center.y + s * 0.12f)
            lineTo(center.x + s * 0.24f, center.y + s * 0.22f)
            lineTo(center.x - s * 0.24f, center.y + s * 0.22f)
            close()
        }
    val crown =
        Path().apply {
            moveTo(center.x - s * 0.22f, center.y + s * 0.12f)
            lineTo(center.x, center.y - s * 0.34f)
            lineTo(center.x + s * 0.22f, center.y + s * 0.12f)
            close()
        }
    drawPath(brim, color)
    drawPath(crown, color)
}

private fun hexPath(corners: List<Offset>): Path =
    Path().apply {
        moveTo(corners[0].x, corners[0].y)
        for (i in 1 until corners.size) lineTo(corners[i].x, corners[i].y)
        close()
    }

private fun hexCross(
    center: Offset,
    r: Float,
): Path =
    Path().apply {
        moveTo(center.x - r, center.y)
        lineTo(center.x + r, center.y)
        moveTo(center.x, center.y - r)
        lineTo(center.x, center.y + r)
    }
