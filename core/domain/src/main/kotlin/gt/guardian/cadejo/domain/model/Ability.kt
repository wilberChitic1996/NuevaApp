package gt.guardian.cadejo.domain.model

/** The three powers of the white Cadejo. */
enum class AbilityId { HOWL, LEAP, PROTECTIVE_LIGHT }

/**
 * Runtime state of one ability: its cooldown length and how many turns remain
 * until it can be used again. Immutable — the engine returns a fresh copy each
 * turn, keeping the whole game state a pure value.
 */
data class AbilityState(
    val id: AbilityId,
    val cooldown: Int,
    val remaining: Int = 0,
) {
    val isReady: Boolean get() = remaining == 0

    fun triggered(): AbilityState = copy(remaining = cooldown)

    fun ticked(): AbilityState = copy(remaining = (remaining - 1).coerceAtLeast(0))
}

/**
 * Balance constants for abilities and the escort. Centralised so tuning the game
 * is a one-file change and so tests can reference the exact numbers.
 */
object Balance {
    const val HOWL_COOLDOWN = 4
    const val HOWL_RADIUS = 2
    const val HOWL_STUN_TURNS = 1

    const val LEAP_COOLDOWN = 3
    const val LEAP_RANGE = 2

    const val LIGHT_COOLDOWN = 5
    const val LIGHT_SHIELD_TURNS = 3

    const val LEVELS_PER_RUN = 10

    /** Reference turn count for the speed bonus; finishing faster scores more. */
    const val PAR_TURNS = 40

    fun defaultAbilities(): List<AbilityState> = listOf(
        AbilityState(AbilityId.HOWL, HOWL_COOLDOWN),
        AbilityState(AbilityId.LEAP, LEAP_COOLDOWN),
        AbilityState(AbilityId.PROTECTIVE_LIGHT, LIGHT_COOLDOWN),
    )
}
