package gt.guardian.cadejo.domain.model

import gt.guardian.cadejo.domain.hex.Hex

/** What occupies a hex cell. */
enum class Terrain {
    /** Walkable ground. */
    FLOOR,

    /** Impassable — blocks both the Cadejo and enemies. */
    WALL,

    /** The escort destination (added to the board so it renders and is queryable). */
    GOAL,

    /** Walkable but damaging (used from Phase 2 onward). */
    HAZARD,
}

/** A single cell on the board. */
data class Cell(
    val hex: Hex,
    val terrain: Terrain,
)

/**
 * The playing field: an immutable map of hex -> cell. A hex that is not present
 * in [cells] is "off the board" and cannot be entered.
 */
data class Board(
    val cells: Map<Hex, Cell>,
) {
    fun terrainAt(hex: Hex): Terrain? = cells[hex]?.terrain

    fun contains(hex: Hex): Boolean = cells.containsKey(hex)

    /** True if the hex exists on the board and is not a wall. */
    fun isWalkable(hex: Hex): Boolean =
        when (terrainAt(hex)) {
            null, Terrain.WALL -> false
            else -> true
        }

    companion object {
        /** Builds a filled-hexagon board of the given [radius], all [Terrain.FLOOR]. */
        fun hexagon(radius: Int): Board {
            val cells =
                Hex
                    .disc(Hex.ORIGIN, radius)
                    .associateWith { Cell(it, Terrain.FLOOR) }
            return Board(cells)
        }
    }
}
