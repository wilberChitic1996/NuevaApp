package gt.guardian.cadejo.domain.hex

import kotlin.math.abs

/**
 * A hexagon in **axial coordinates** `(q, r)`.
 *
 * We use axial coords (with the third cube coordinate `s = -q - r` derived on
 * demand) because they make the six directions, distance and neighbourhood
 * trivial to compute with plain integer arithmetic and no special cases — the
 * approach popularised by Red Blob Games. Orientation is pointy-top, which fits
 * portrait phone screens.
 *
 * The type is a value class over two ints packed into a Long: cheap to allocate,
 * cheap to use as a map key, and immutable (which the pure game engine relies on).
 */
@JvmInline
value class Hex private constructor(private val packed: Long) {

    constructor(q: Int, r: Int) : this((q.toLong() and 0xFFFFFFFFL) or (r.toLong() shl 32))

    val q: Int get() = packed.toInt()
    val r: Int get() = (packed shr 32).toInt()

    /** The derived cube coordinate. Always satisfies `q + r + s == 0`. */
    val s: Int get() = -q - r

    operator fun plus(other: Hex): Hex = Hex(q + other.q, r + other.r)
    operator fun minus(other: Hex): Hex = Hex(q - other.q, r - other.r)

    /** Grid distance in steps between this hex and [other]. */
    fun distanceTo(other: Hex): Int {
        val dq = q - other.q
        val dr = r - other.r
        val ds = s - other.s
        return (abs(dq) + abs(dr) + abs(ds)) / 2
    }

    /** The six adjacent hexes, in a fixed direction order (deterministic). */
    fun neighbors(): List<Hex> = DIRECTIONS.map { this + it }

    override fun toString(): String = "Hex($q, $r)"

    companion object {
        val ORIGIN = Hex(0, 0)

        /**
         * The six axial direction vectors, in clockwise order starting East.
         * The order is part of the game's contract: enemy movement breaks ties
         * by this order, so changing it would change every generated run.
         */
        val DIRECTIONS: List<Hex> = listOf(
            Hex(1, 0),   // E
            Hex(1, -1),  // NE
            Hex(0, -1),  // NW
            Hex(-1, 0),  // W
            Hex(-1, 1),  // SW
            Hex(0, 1),   // SE
        )

        /** All hexes within [radius] steps of [center] (a filled hexagon). */
        fun disc(center: Hex, radius: Int): Set<Hex> {
            val result = LinkedHashSet<Hex>()
            for (dq in -radius..radius) {
                val rMin = maxOf(-radius, -dq - radius)
                val rMax = minOf(radius, -dq + radius)
                for (dr in rMin..rMax) {
                    result.add(center + Hex(dq, dr))
                }
            }
            return result
        }
    }
}
