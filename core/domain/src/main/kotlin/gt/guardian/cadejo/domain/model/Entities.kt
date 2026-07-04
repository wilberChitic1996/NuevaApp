package gt.guardian.cadejo.domain.model

import gt.guardian.cadejo.domain.hex.Hex

/** The kinds of nocturnal spirit that hunt the traveler, each with its own movement pattern. */
enum class SpiritKind {
    /** The black Cadejo — mirrors and chases the player. */
    CADEJO_NEGRO,

    /** La Llorona — a relentless chaser. */
    LLORONA,

    /** El Sombrerón — patrols a fixed route. */
    SOMBRERON,
}

/** The rule an enemy follows to choose its next hex each turn. */
enum class PatternType {
    /** Move one step along the shortest path toward the player. */
    CHASE,

    /** Follow a fixed cyclic route (used from Phase 2). */
    PATROL,

    /** Mirror the player's last move (used from Phase 2). */
    MIRROR,
}

/**
 * An enemy spirit on the board. Immutable: the engine produces a new [Enemy]
 * (via [copy]) each turn rather than mutating in place.
 *
 * @param stunnedTurns turns remaining that the enemy is paralysed (by the Howl
 *   ability). While > 0 the enemy does not move. Wired for Phase 1 so the Howl
 *   mechanic in a later phase needs no state changes here.
 */
data class Enemy(
    val id: String,
    val kind: SpiritKind,
    val position: Hex,
    val pattern: PatternType,
    val stunnedTurns: Int = 0,
)
