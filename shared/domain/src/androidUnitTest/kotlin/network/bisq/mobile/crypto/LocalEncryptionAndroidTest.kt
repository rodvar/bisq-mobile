package network.bisq.mobile.crypto

import org.junit.Test
import org.junit.Assume.assumeTrue
import kotlin.test.assertContentEquals
import kotlin.test.assertFailsWith
import java.security.KeyStore

class LocalEncryptionAndroidTest {

    @Test
    fun round_trip_encrypt_decrypt_when_keystore_available() {
        assumeTrue(isAndroidKeyStoreAvailable())
        val plaintext = "KMP Android AES-GCM".encodeToByteArray()
        val ciphertext = encrypt(plaintext)
        val decrypted = decrypt(ciphertext)
        assertContentEquals(plaintext, decrypted)
    }

    @Test
    fun tamper_detection_fails_when_keystore_available() {
        assumeTrue(isAndroidKeyStoreAvailable())
        val plaintext = "tamper".encodeToByteArray()
        val ciphertext = encrypt(plaintext)
        val tampered = ciphertext.copyOf().also { it[it.lastIndex] = (it.last().toInt() xor 0x01).toByte() }
        assertFailsWith<Throwable> { decrypt(tampered) }
    }

    private fun isAndroidKeyStoreAvailable(): Boolean {
        return try {
            val ks = KeyStore.getInstance("AndroidKeyStore")
            ks.load(null)
            true
        } catch (_: Throwable) {
            false
        }
    }
}

