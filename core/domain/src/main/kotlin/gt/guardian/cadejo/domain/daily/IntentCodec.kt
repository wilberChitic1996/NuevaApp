package gt.guardian.cadejo.domain.daily

import gt.guardian.cadejo.domain.hex.Hex
import gt.guardian.cadejo.domain.model.AbilityId
import gt.guardian.cadejo.domain.model.Intent

/**
 * Compact, stable text encoding of a run's intent tape, so it can travel to the
 * backend for re-validation. The format is deliberately simple and mirrored
 * byte-for-byte by the TypeScript decoder in the Edge Function.
 *
 * Tokens (joined by ';'):
 *   W                    -> Wait
 *   M{q},{r}             -> Move to hex (q, r)
 *   A{ABILITY}           -> UseAbility with no target (Howl, Protective Light)
 *   A{ABILITY},{q},{r}   -> UseAbility with a target hex (Leap)
 */
object IntentCodec {

    fun encode(intents: List<Intent>): String =
        intents.joinToString(";") { encodeOne(it) }

    fun decode(encoded: String): List<Intent> {
        if (encoded.isBlank()) return emptyList()
        return encoded.split(";").map { decodeOne(it) }
    }

    private fun encodeOne(intent: Intent): String = when (intent) {
        is Intent.Wait -> "W"
        is Intent.Move -> "M${intent.target.q},${intent.target.r}"
        is Intent.UseAbility -> {
            val base = "A${intent.id.name}"
            val t = intent.target
            if (t != null) "$base,${t.q},${t.r}" else base
        }
    }

    private fun decodeOne(token: String): Intent = when (token.firstOrNull()) {
        'W' -> Intent.Wait
        'M' -> {
            val (q, r) = token.drop(1).split(",").map { it.toInt() }
            Intent.Move(Hex(q, r))
        }
        'A' -> {
            val parts = token.drop(1).split(",")
            val id = AbilityId.valueOf(parts[0])
            val target = if (parts.size >= 3) Hex(parts[1].toInt(), parts[2].toInt()) else null
            Intent.UseAbility(id, target)
        }
        else -> error("Unknown intent token: $token")
    }
}
