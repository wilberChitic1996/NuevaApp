package gt.guardian.cadejo.core.data.save

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import gt.guardian.cadejo.domain.integrity.Signer
import java.security.KeyStore
import javax.crypto.KeyGenerator
import javax.crypto.Mac
import javax.crypto.SecretKey
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Signs the save payload with HMAC-SHA256 using a key held in the **Android
 * Keystore**.
 *
 * How the anti-tampering works, end to end:
 *  1. On first use we generate an HMAC key inside the Keystore under a fixed
 *     alias. The raw key bytes never leave the secure hardware/TEE — we can ask it
 *     to sign, but we can never read the key out.
 *  2. Every time the sensitive save (coins, unlocks, ads-removed) changes, we
 *     serialise it canonically and store `HMAC(payload)` next to it.
 *  3. On load we recompute the HMAC and compare it in constant time. A mismatch
 *     means the file was edited off-app, so we reset progression to a safe state.
 *
 * Because the key is device-bound and non-exportable, an attacker can't recompute
 * a valid HMAC for edited data on another machine. This stops casual/offline
 * tampering; anything tied to real money is still validated server-side.
 */
@Singleton
class KeystoreHmacSigner @Inject constructor() : Signer {
    override fun sign(bytes: ByteArray): ByteArray {
        val mac = Mac.getInstance(MAC_ALGORITHM)
        mac.init(getOrCreateKey())
        return mac.doFinal(bytes)
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(PROVIDER).apply { load(null) }
        (keyStore.getEntry(ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_HMAC_SHA256, PROVIDER)
        generator.init(
            KeyGenParameterSpec
                .Builder(ALIAS, KeyProperties.PURPOSE_SIGN)
                .setKeySize(256)
                .build(),
        )
        return generator.generateKey()
    }

    private companion object {
        const val PROVIDER = "AndroidKeyStore"
        const val ALIAS = "cadejo_save_hmac_v1"
        const val MAC_ALGORITHM = "HmacSHA256"
    }
}
