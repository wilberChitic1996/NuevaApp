package gt.guardian.cadejo.domain.model

import gt.guardian.cadejo.domain.hex.Hex

/** The kinds of nocturnal spirit that hunt the traveler, each with its own movement pattern. */
enum class SpiritKind {
    /** The black Cadejo — mirrors the player's movement. */
    CADEJO_NEGRO,

    /** La Llorona — a relentless chaser. */
    LLORONA,

    /** El Sombrerón — patrols a fixed route. */
    SOMBRERON,
}

/** The rule an enemy follows to choose its next hex each turn. */
enum class PatternType {
    /** Move one step along the shortest path toward the target (the traveler). */
    CHASE,

    /** Follow a fixed cyclic route. */
    PATROL,

    /** Repeat the player's last move direction. */
    MIRROR,
}

/**
 * An enemy spirit on the board. Immutable: the engine produces a new [Enemy] each
 * turn rather than mutating in place.
 *
 * @param stunnedTurns turns remaining that the enemy is paralysed (by Howl). While
 *   > 0 the enemy does not move.
 * @param patrolRoute for [PatternType.PATROL]: the waypoints it cycles through.
 * @param patrolIndex the current waypoint index within [patrolRoute].
 */
data class Enemy(
    val id: String,
    val kind: SpiritKind,
    val position: Hex,
    val pattern: PatternType,
    val stunnedTurns: Int = 0,
    val patrolRoute: List<Hex> = emptyList(),
    val patrolIndex: Int = 0,
)
