package gt.guardian.cadejo.core.ui.board

import androidx.compose.ui.geometry.Offset
import gt.guardian.cadejo.domain.hex.Hex
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Converts between axial hex coordinates and screen pixels for a **pointy-top**
 * layout. Pure math (no Compose state), so it's easy to reason about and reuse.
 *
 * @param size distance from a hex centre to a corner, in pixels.
 * @param origin pixel offset added to every hex centre (used to centre the board).
 */
class HexLayout(
    val size: Float,
    val origin: Offset,
) {
    fun toPixel(hex: Hex): Offset {
        val x = size * (SQRT3 * hex.q + SQRT3 / 2f * hex.r)
        val y = size * (1.5f * hex.r)
        return Offset(x + origin.x, y + origin.y)
    }

    /** Inverse mapping with cube rounding, for hit-testing taps. */
    fun toHex(point: Offset): Hex {
        val px = (point.x - origin.x) / size
        val py = (point.y - origin.y) / size
        val q = (SQRT3 / 3f * px - 1f / 3f * py)
        val r = (2f / 3f * py)
        return cubeRound(q, r)
    }

    /** The six corner points of a hex centred at [center]. */
    fun corners(center: Offset): List<Offset> =
        (0 until 6).map { i ->
            val angle = Math.PI / 180.0 * (60.0 * i - 30.0)
            Offset(
                x = center.x + size * cos(angle).toFloat(),
                y = center.y + size * sin(angle).toFloat(),
            )
        }

    private fun cubeRound(
        qf: Float,
        rf: Float,
    ): Hex {
        val sf = -qf - rf
        var q = qf.roundToInt()
        var r = rf.roundToInt()
        val s = sf.roundToInt()
        val dq = kotlin.math.abs(q - qf)
        val dr = kotlin.math.abs(r - rf)
        val ds = kotlin.math.abs(s - sf)
        when {
            dq > dr && dq > ds -> q = -r - s
            dr > ds -> r = -q - s
        }
        return Hex(q, r)
    }

    companion object {
        val SQRT3 = sqrt(3f)

        /**
         * Builds a layout that fits every [hexes] cell inside [widthPx] x [heightPx]
         * with a small margin, and centres it.
         */
        fun fitting(
            hexes: Collection<Hex>,
            widthPx: Float,
            heightPx: Float,
            marginPx: Float = 24f,
        ): HexLayout {
            if (hexes.isEmpty()) return HexLayout(1f, Offset.Zero)
            // Use a unit layout to measure the bounding box, then scale.
            val unit = HexLayout(1f, Offset.Zero)
            val pts = hexes.map { unit.toPixel(it) }
            val minX = pts.minOf { it.x } - 1f
            val maxX = pts.maxOf { it.x } + 1f
            val minY = pts.minOf { it.y } - 1f
            val maxY = pts.maxOf { it.y } + 1f
            val spanX = (maxX - minX).coerceAtLeast(0.0001f)
            val spanY = (maxY - minY).coerceAtLeast(0.0001f)

            val usableW = (widthPx - 2 * marginPx).coerceAtLeast(1f)
            val usableH = (heightPx - 2 * marginPx).coerceAtLeast(1f)
            val size = minOf(usableW / spanX, usableH / spanY)

            // Centre: place the bounding-box centre at the viewport centre.
            val boxCenter = Offset((minX + maxX) / 2f * size, (minY + maxY) / 2f * size)
            val viewCenter = Offset(widthPx / 2f, heightPx / 2f)
            return HexLayout(size, viewCenter - boxCenter)
        }
    }
}
