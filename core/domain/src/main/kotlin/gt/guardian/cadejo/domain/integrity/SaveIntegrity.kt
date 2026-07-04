package gt.guardian.cadejo.domain.integrity

import gt.guardian.cadejo.domain.progress.PlayerProfile
import gt.guardian.cadejo.domain.progress.UnlockId

/**
 * Signs arbitrary bytes. The production implementation ([gt.guardian.cadejo.core.data]
 * `KeystoreHmacSigner`) computes HMAC-SHA256 with a key that never leaves the
 * Android Keystore, so a signature can't be forged off-device. Keeping the
 * interface in the domain lets us unit-test the integrity flow on the JVM with a
 * plain HMAC signer standing in for the Keystore.
 */
fun interface Signer {
    fun sign(bytes: ByteArray): ByteArray
}

/**
 * Turns the *sensitive* part of a profile into a stable, canonical byte string.
 *
 * "Canonical" matters: the signature is only meaningful if the exact same profile
 * always serialises to the exact same bytes. So unlocks are sorted by name and the
 * format is fixed and versioned. Fields that don't affect the economy (stats,
 * cosmetic selection) are intentionally excluded from the signed payload.
 */
object SaveCodec {
    const val VERSION = 1

    fun canonicalBytes(profile: PlayerProfile): ByteArray = buildString {
        append("cadejo-save|v").append(VERSION)
        append("|coins=").append(profile.coins)
        append("|ads=").append(if (profile.adsRemoved) 1 else 0)
        append("|unlocks=")
        append(profile.unlocks.map(UnlockId::name).sorted().joinToString(","))
    }.encodeToByteArray()
}

/**
 * Verifies a profile against a stored signature and resets to a safe state on
 * tampering.
 *
 * The comparison is **constant-time** on purpose: a naive early-exit `equals`
 * leaks, via timing, how many leading bytes matched, which can help an attacker
 * forge a signature byte by byte. Comparing every byte regardless closes that.
 */
object SaveIntegrity {

    /** Recompute the signature and compare it, in constant time, to [storedSignature]. */
    fun verify(profile: PlayerProfile, storedSignature: ByteArray, signer: Signer): Boolean {
        val expected = signer.sign(SaveCodec.canonicalBytes(profile))
        return constantTimeEquals(expected, storedSignature)
    }

    /** Fresh signature for a profile, to store alongside it after every legitimate write. */
    fun sign(profile: PlayerProfile, signer: Signer): ByteArray =
        signer.sign(SaveCodec.canonicalBytes(profile))

    /**
     * The profile to trust: [loaded] if its signature checks out, otherwise the
     * safe [PlayerProfile.INITIAL] (a detected tamper resets progression rather
     * than honouring forged coins/unlocks).
     */
    fun sanitized(loaded: PlayerProfile, storedSignature: ByteArray?, signer: Signer): PlayerProfile =
        if (storedSignature != null && verify(loaded, storedSignature, signer)) loaded else PlayerProfile.INITIAL

    fun constantTimeEquals(a: ByteArray, b: ByteArray): Boolean {
        if (a.size != b.size) return false
        var diff = 0
        for (i in a.indices) diff = diff or (a[i].toInt() xor b[i].toInt())
        return diff == 0
    }
}
