package gt.guardian.cadejo.domain.integrity

import gt.guardian.cadejo.domain.progress.PlayerProfile
import gt.guardian.cadejo.domain.progress.UnlockId
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class SaveIntegrityTest {
    // A plain HMAC signer stands in for the Android Keystore in JVM tests.
    private val goodSigner = hmacSigner("device-bound-key")
    private val attackerSigner = hmacSigner("attacker-key")

    private fun hmacSigner(key: String): Signer =
        Signer { bytes ->
            Mac
                .getInstance("HmacSHA256")
                .apply {
                    init(SecretKeySpec(key.encodeToByteArray(), "HmacSHA256"))
                }.doFinal(bytes)
        }

    @Test
    fun `canonical bytes are stable regardless of unlock ordering`() {
        val a = PlayerProfile(coins = 50, unlocks = setOf(UnlockId.SKIN_JADE, UnlockId.MODIFIER_SWIFT))
        val b = PlayerProfile(coins = 50, unlocks = setOf(UnlockId.MODIFIER_SWIFT, UnlockId.SKIN_JADE))
        assertArrayEquals(SaveCodec.canonicalBytes(a), SaveCodec.canonicalBytes(b))
    }

    @Test
    fun `a genuine signature verifies`() {
        val profile = PlayerProfile(coins = 999, unlocks = setOf(UnlockId.SKIN_OBSIDIAN))
        val sig = SaveIntegrity.sign(profile, goodSigner)
        assertTrue(SaveIntegrity.verify(profile, sig, goodSigner))
    }

    @Test
    fun `tampering with coins invalidates the signature`() {
        val honest = PlayerProfile(coins = 100)
        val sig = SaveIntegrity.sign(honest, goodSigner)

        val hacked = honest.copy(coins = 999_999)
        assertFalse("edited coins must fail verification", SaveIntegrity.verify(hacked, sig, goodSigner))
    }

    @Test
    fun `a signature from a different key does not verify`() {
        val profile = PlayerProfile(coins = 100)
        val forged = SaveIntegrity.sign(profile, attackerSigner)
        assertFalse(SaveIntegrity.verify(profile, forged, goodSigner))
    }

    @Test
    fun `sanitized resets tampered saves to the safe default`() {
        val honest = PlayerProfile(coins = 100)
        val sig = SaveIntegrity.sign(honest, goodSigner)
        val hacked = honest.copy(coins = 999_999, unlocks = UnlockId.entries.toSet())

        val trusted = SaveIntegrity.sanitized(hacked, sig, goodSigner)
        assertEquals(PlayerProfile.INITIAL, trusted)
    }

    @Test
    fun `sanitized keeps a valid save intact`() {
        val honest = PlayerProfile(coins = 100, unlocks = setOf(UnlockId.SKIN_JADE))
        val sig = SaveIntegrity.sign(honest, goodSigner)
        assertEquals(honest, SaveIntegrity.sanitized(honest, sig, goodSigner))
    }

    @Test
    fun `constant-time equals matches only identical arrays`() {
        assertTrue(SaveIntegrity.constantTimeEquals(byteArrayOf(1, 2, 3), byteArrayOf(1, 2, 3)))
        assertFalse(SaveIntegrity.constantTimeEquals(byteArrayOf(1, 2, 3), byteArrayOf(1, 2, 4)))
        assertFalse(SaveIntegrity.constantTimeEquals(byteArrayOf(1, 2), byteArrayOf(1, 2, 3)))
    }
}
